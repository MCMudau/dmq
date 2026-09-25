package contractor.site.dmq.controllers;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import contractor.site.dmq.dtos.AccountDtos.AccountResponse;
import contractor.site.dmq.dtos.AccountDtos.ConfirmPasswordResetRequest;
import contractor.site.dmq.dtos.AccountDtos.LoginRequest;
import contractor.site.dmq.dtos.AccountDtos.PasswordResetResponse;
import contractor.site.dmq.dtos.AccountDtos.RegisterAdminRequest;
import contractor.site.dmq.dtos.AccountDtos.RequestPasswordResetRequest;
import contractor.site.dmq.dtos.AccountDtos.Status;
import contractor.site.dmq.dtos.AccountDtos.VerifyAdminRequest;
import contractor.site.dmq.dtos.AccountDtos.VerifyResetPinRequest;
import contractor.site.dmq.services.AccountManagementService;
import contractor.site.dmq.services.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
@Slf4j
public class AccountManagementController {

    private final AccountManagementService accountService;

    /** Create an unverified admin account (or report the state of an existing one). */
    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@RequestBody RegisterAdminRequest request) {
        try {
            AccountResponse response = accountService.register(request);
            return ResponseEntity.status(httpStatus(response.status())).body(response);
        } catch (EmailService.EmailSendException e) {
            AccountResponse response = AccountResponse.of(Status.EMAIL_FAILED,
                    "We couldn't complete your registration because the verification email could not be sent. "
                            + "Please try again in a few minutes.");
            return ResponseEntity.status(httpStatus(response.status())).body(response);
        }
    }

    /** Verify an admin with the PIN the owner gave them. */
    @PostMapping("/verify")
    public ResponseEntity<AccountResponse> verify(@RequestBody VerifyAdminRequest request) {
        AccountResponse response = accountService.verify(request);
        return ResponseEntity.status(httpStatus(response.status())).body(response);
    }
        /** Log an admin in. On success the admin's email is stored in the HTTP session. */
    @PostMapping("/login")
    public ResponseEntity<AccountResponse> login(@RequestBody LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        AccountResponse response = accountService.login(request);

        if (response.status() == Status.LOGIN_SUCCESS) {
            HttpSession session = httpRequest.getSession(true);
            httpRequest.changeSessionId();   // new session id on login (prevents session fixation)
            session.setAttribute("adminEmail", request.email().trim().toLowerCase(Locale.ROOT));
        }
        return ResponseEntity.status(httpStatus(response.status())).body(response);
    }

        /** "Send Reset PIN" button. */
    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(@RequestBody RequestPasswordResetRequest request) {
        try {
            PasswordResetResponse response = accountService.requestPasswordReset(request);
            return ResponseEntity.status(httpStatus(response.status())).body(response);
        } catch (EmailService.EmailSendException e) {
            PasswordResetResponse response = PasswordResetResponse.of(Status.EMAIL_FAILED,
                    "We couldn't send the reset PIN email. Please try again shortly.");
            return ResponseEntity.status(httpStatus(response.status())).body(response);
        }
    }

    /** Silent check fired whenever the admin (re)enters their email; never sends an email. */
    @PostMapping("/password-reset/status")
    public ResponseEntity<PasswordResetResponse> passwordResetStatus(@RequestBody RequestPasswordResetRequest request) {
        PasswordResetResponse response = accountService.checkResetStatus(request == null ? null : request.email());
        return ResponseEntity.status(httpStatus(response.status())).body(response);
    }

    /** "Resend PIN" inside the modal. */
    @PostMapping("/password-reset/resend")
    public ResponseEntity<PasswordResetResponse> resendResetPin(@RequestBody RequestPasswordResetRequest request) {
        try {
            PasswordResetResponse response = accountService.resendResetPin(request == null ? null : request.email());
            return ResponseEntity.status(httpStatus(response.status())).body(response);
        } catch (EmailService.EmailSendException e) {
            PasswordResetResponse response = PasswordResetResponse.of(Status.EMAIL_FAILED,
                    "We couldn't resend the PIN. Please try again shortly.");
            return ResponseEntity.status(httpStatus(response.status())).body(response);
        }
    }

    /** PIN entered in the modal. */
    @PostMapping("/password-reset/verify-pin")
    public ResponseEntity<PasswordResetResponse> verifyResetPin(@RequestBody VerifyResetPinRequest request) {
        PasswordResetResponse response = accountService.verifyResetPin(request);
        return ResponseEntity.status(httpStatus(response.status())).body(response);
    }

    /** Final "Reset Password" submit. */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<AccountResponse> confirmPasswordReset(@RequestBody ConfirmPasswordResetRequest request) {
        AccountResponse response = accountService.confirmPasswordReset(request);
        return ResponseEntity.status(httpStatus(response.status())).body(response);
    }

       /** Who is currently logged in, read from the session set by /login. 401 if no one is. */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String email = session == null ? null : (String) session.getAttribute("adminEmail");
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "NOT_AUTHENTICATED"));
        }
        return accountService.findVerifiedAdminByEmail(email)
                .map(admin -> ResponseEntity.ok(Map.of(
                        "name", admin.getName(),
                        "surname", admin.getSurname(),
                        "email", admin.getEmail())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "NOT_AUTHENTICATED")));
    }

    /** Ends the admin's session. */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("status", "LOGGED_OUT"));
    }

       private HttpStatus httpStatus(Status status) {
        return switch (status) {
            case PENDING_VERIFICATION, VERIFIED, LOGIN_SUCCESS, RESET_PIN_NONE, RESET_PIN_SENT,
                 RESET_PIN_ACTIVE, RESET_PIN_VERIFIED, PASSWORD_RESET_SUCCESS -> HttpStatus.OK;
            case VALIDATION_ERROR, INVALID_PIN -> HttpStatus.BAD_REQUEST;
            case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case NOT_VERIFIED -> HttpStatus.FORBIDDEN;
            case ALREADY_VERIFIED -> HttpStatus.CONFLICT;
            case RESET_EMAIL_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESET_PIN_EXPIRED -> HttpStatus.GONE;
            case LOCKED -> HttpStatus.TOO_MANY_REQUESTS;
            case EMAIL_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
