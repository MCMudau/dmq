package contractor.site.dmq.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import contractor.site.dmq.dtos.AccountDtos.AccountResponse;
import contractor.site.dmq.dtos.AccountDtos.LoginRequest;
import contractor.site.dmq.dtos.AccountDtos.RegisterAdminRequest;
import contractor.site.dmq.dtos.AccountDtos.Status;
import contractor.site.dmq.dtos.AccountDtos.VerifyAdminRequest;
import contractor.site.dmq.models.Admin;
import contractor.site.dmq.repos.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin account creation and owner verification.
 *
 * Register: validate -> (existing email? report its state) -> save as UNVERIFIED with a PIN
 *           -> email the owner the PIN -> email the admin "account created, contact the owner".
 * Verify:   admin enters the PIN the owner gave them -> account marked verified.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountManagementService {

    // Same rules as the registration page, so the two can never disagree.
    private static final Pattern EMAIL   = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern PHONE   = Pattern.compile("^(\\+27|0)[\\s-]?\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{4}$");
    private static final Pattern UPPER   = Pattern.compile("[A-Z]");
    private static final Pattern LOWER   = Pattern.compile("[a-z]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9\\s]");

    private static final int MIN_PASSWORD = 7;
    private static final int MAX_PASSWORD = 64;   // BCrypt only uses the first 72 bytes

    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
        // Burns the same time as a real check when the email doesn't exist, so response
    // time can't be used to discover which emails are registered.
    private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("not-a-real-password");

    private final AdminRepository adminRepository;
    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    /** Failed-PIN counters per email. In memory: resets on restart, which is fine for a lockout. */
    private final Map<String, Attempts> pinAttempts = new ConcurrentHashMap<>();

    private static final class Attempts {
        int failures;
        Instant lockedUntil;
    }

    // ------------------------------------------------------------------
    // Register
    // ------------------------------------------------------------------

    /**
     * If the owner cannot be emailed this throws EmailService.EmailSendException and the
     * transaction rolls back, so no account is left behind that nobody can verify.
     */
    @Transactional
    public AccountResponse register(RegisterAdminRequest req) {
        Map<String, String> errors = validate(req);
        if (!errors.isEmpty()) {
            return AccountResponse.invalid(errors);
        }

        String email = normaliseEmail(req.email());

        // Account already there? Report its state; never touch or overwrite it.
        Optional<Admin> existing = adminRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            if (Boolean.TRUE.equals(existing.get().getVerified())) {
                return AccountResponse.of(Status.ALREADY_VERIFIED,
                        "An account with this email already exists and is verified. Please log in.");
            }
            return AccountResponse.pending(
                    "An account with this email has already been created but is not verified yet. "
                            + "Please contact the owner for the verification PIN.", false);
        }

        Admin admin = new Admin();
        admin.setName(clean(req.name()));
        admin.setSurname(clean(req.surname()));
        admin.setEmail(email);
        admin.setPhoneNumber(clean(req.phoneNumber()));
        admin.setPassword(passwordEncoder.encode(req.password()));
        admin.setVerificationPin(generatePin());
        admin.setVerified(false);
        adminRepository.save(admin);

        // Owner must receive the PIN; if this fails the whole registration is rolled back.
        emailService.notifyOwnerOfNewAdmin(admin);

        // The admin's own email is a courtesy; don't undo the registration if it fails.
        try {
            emailService.sendRegistrationReceivedToAdmin(admin);
        } catch (EmailService.EmailSendException e) {
            log.warn("Admin {} was registered but the confirmation email could not be sent", email);
        }

        return AccountResponse.pending(
                "Your account has been created. Please contact the owner to get your verification PIN. "
                        + "We've also emailed you these instructions.", true);
    }

    // ------------------------------------------------------------------
    // Verify
    // ------------------------------------------------------------------

    @Transactional
    public AccountResponse verify(VerifyAdminRequest req) {
        String email = normaliseEmail(req == null ? null : req.email());
        String pin = clean(req == null ? null : req.pin());

        Optional<Admin> found = adminRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            // Same answer as a wrong PIN, so this can't be used to discover which emails exist.
            return AccountResponse.of(Status.INVALID_PIN, "That PIN is not correct.");
        }
        Admin admin = found.get();

        if (Boolean.TRUE.equals(admin.getVerified())) {
            return AccountResponse.of(Status.VERIFIED, "Your account is already verified.");
        }

        Optional<Duration> locked = lockRemaining(email);
        if (locked.isPresent()) {
            long minutes = Math.max(1, (locked.get().getSeconds() + 59) / 60);
            return AccountResponse.of(Status.LOCKED,
                    "Too many incorrect attempts. Please try again in " + minutes + " minute(s).");
        }

        boolean match = MessageDigest.isEqual(
                admin.getVerificationPin().getBytes(StandardCharsets.UTF_8),
                pin.getBytes(StandardCharsets.UTF_8));

        if (!match) {
            int failures = recordFailure(email);
            if (failures >= MAX_PIN_ATTEMPTS) {
                return AccountResponse.of(Status.LOCKED,
                        "Too many incorrect attempts. Please try again in " + LOCKOUT.toMinutes() + " minutes.");
            }
            int left = MAX_PIN_ATTEMPTS - failures;
            return AccountResponse.of(Status.INVALID_PIN,
                    "That PIN is not correct. " + left + " attempt(s) left.");
        }

        pinAttempts.remove(email);
        admin.setVerified(true);
        adminRepository.save(admin);
        log.info("Admin {} verified", email);

        try {
            emailService.sendAdminVerifiedNotice(admin);
        } catch (EmailService.EmailSendException e) {
            log.warn("Admin {} verified but the confirmation email could not be sent", email);
        }
        return AccountResponse.of(Status.VERIFIED, "Your account has been verified.");
    }

        // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AccountResponse login(LoginRequest req) {
        String email = normaliseEmail(req == null ? null : req.email());
        String password = (req == null || req.password() == null) ? "" : req.password();

        if (email.isEmpty() || password.isEmpty()) {
            return AccountResponse.of(Status.INVALID_CREDENTIALS, "Enter your email and password.");
        }
        if (password.length() > MAX_PASSWORD) {
            return AccountResponse.of(Status.INVALID_CREDENTIALS, "Incorrect email or password.");
        }

        // Separate counter from PIN attempts ("login:" prefix), reusing the same lockout helpers.
        String lockKey = "login:" + email;
        Optional<Duration> locked = lockRemaining(lockKey);
        if (locked.isPresent()) {
            long minutes = Math.max(1, (locked.get().getSeconds() + 59) / 60);
            return AccountResponse.of(Status.LOCKED,
                    "Too many incorrect attempts. Please try again in " + minutes + " minute(s).");
        }

        Optional<Admin> found = adminRepository.findByEmailIgnoreCase(email);
        String hash = found.map(Admin::getPassword).orElse(DUMMY_HASH);
        boolean passwordOk = passwordEncoder.matches(password, hash);

        if (found.isEmpty() || !passwordOk) {
            int failures = recordFailure(lockKey);
            if (failures >= MAX_PIN_ATTEMPTS) {
                return AccountResponse.of(Status.LOCKED,
                        "Too many incorrect attempts. Please try again in " + LOCKOUT.toMinutes() + " minutes.");
            }
            return AccountResponse.of(Status.INVALID_CREDENTIALS, "Incorrect email or password.");
        }

        pinAttempts.remove(lockKey);
        Admin admin = found.get();

        // Checked only AFTER the password is correct, so an unverified account's
        // existence is never revealed to someone who doesn't know its password.
        if (!Boolean.TRUE.equals(admin.getVerified())) {
            return AccountResponse.of(Status.NOT_VERIFIED,
                    "Your account has not been verified yet. Contact the owner for your verification PIN, "
                            + "then enter it on the registration page.");
        }

        log.info("Admin {} logged in", email);
        return AccountResponse.of(Status.LOGIN_SUCCESS, "Welcome back, " + admin.getName() + ".");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Map<String, String> validate(RegisterAdminRequest r) {
        Map<String, String> e = new LinkedHashMap<>();
        if (r == null) {
            e.put("name", "Please fill in the form.");
            return e;
        }

        String name = clean(r.name());
        String surname = clean(r.surname());
        String email = clean(r.email());
        String phone = clean(r.phoneNumber());
        String pw = r.password() == null ? "" : r.password();

        if (name.length() < 2 || name.length() > 50) {
            e.put("name", "Please enter your name (2-50 characters).");
        }
        if (surname.length() < 2 || surname.length() > 50) {
            e.put("surname", "Please enter your surname (2-50 characters).");
        }
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            e.put("email", "Enter a valid email address.");
        }
        if (!PHONE.matcher(phone).matches()) {
            e.put("phoneNumber", "Enter a valid South African number, e.g. 071 932 8617.");
        }

        if (pw.length() < MIN_PASSWORD) {
            e.put("password", "Password must be at least " + MIN_PASSWORD + " characters.");
        } else if (pw.length() > MAX_PASSWORD) {
            e.put("password", "Password must be at most " + MAX_PASSWORD + " characters.");
        } else if (!UPPER.matcher(pw).find()) {
            e.put("password", "Password must contain an uppercase letter.");
        } else if (!LOWER.matcher(pw).find()) {
            e.put("password", "Password must contain a lowercase letter.");
        } else if (!SPECIAL.matcher(pw).find()) {
            e.put("password", "Password must contain a special character.");
        }

        if (!pw.equals(r.confirmPassword())) {
            e.put("confirmPassword", "Passwords do not match.");
        }
        return e;
    }

    private String generatePin() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normaliseEmail(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private Optional<Duration> lockRemaining(String email) {
        Attempts a = pinAttempts.get(email);
        if (a == null || a.lockedUntil == null) {
            return Optional.empty();
        }
        Duration left = Duration.between(Instant.now(), a.lockedUntil);
        if (left.isNegative() || left.isZero()) {
            pinAttempts.remove(email);   // lock expired, start fresh
            return Optional.empty();
        }
        return Optional.of(left);
    }

    private int recordFailure(String email) {
        Attempts a = pinAttempts.computeIfAbsent(email, k -> new Attempts());
        synchronized (a) {
            a.failures++;
            if (a.failures >= MAX_PIN_ATTEMPTS) {
                a.lockedUntil = Instant.now().plus(LOCKOUT);
            }
            return a.failures;
        }
    }
}
