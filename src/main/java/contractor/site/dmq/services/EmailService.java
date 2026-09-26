package contractor.site.dmq.services;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

import contractor.site.dmq.models.Admin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends all outgoing platform emails through Brevo's transactional email HTTP API
 * (POST https://api.brevo.com/v3/smtp/email). HTTPS on port 443 is used instead of SMTP,
 * so it works even where a VPS provider blocks outbound mail ports.
 *
 * Admin verification flow:
 *   1. Admin registers  -> notifyOwnerOfNewAdmin()        (owner gets details + PIN)
 *                       -> sendRegistrationReceivedToAdmin() (admin is told to wait)
 *   2. Owner shares the PIN with the admin, admin enters it, account is marked verified
 *                       -> sendAdminVerifiedNotice()       (admin told they can log in)
 *
 * Sender addresses: every email is sent from one of the FROM_* constants below rather
 * than a single configured address, so different kinds of mail (admin alerts vs.
 * future client-facing quotation mail) come from an address that matches their purpose.
 * All of these live on dmq.schedulers.trade, which is already domain-authenticated
 * (SPF/DKIM/DMARC) with Brevo, so any address on it can send without separate
 * per-address verification.
 *
 * Note: this class uses Spring's @Service. Import it explicitly (not via a wildcard) so it
 * is never confused with your own contractor.site.dmq.models.Service entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // ------------------------------------------------------------------
    // Sender addresses - all on the authenticated dmq.schedulers.trade domain.
    // Pick the one that matches what the email is about, not who technically sent it.
    // ------------------------------------------------------------------

    /** Admin account lifecycle: registration, verification, password reset, owner alerts. */
    public static final String FROM_ADMIN = "admin@dmq.schedulers.trade";

    /** Quotation-related mail to clients (used once quotation emails are added). */
    public static final String FROM_QUOTES = "quote@dmq.schedulers.trade";

    /** Anything automated that shouldn't invite a reply. */
    public static final String FROM_NOREPLY = "noreply@dmq.schedulers.trade";

    /** Display name shown in the recipient's inbox, regardless of which address sent it. */
    private static final String DISPLAY_NAME = "DMQ Plumbers & Renovations";

    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private static final RestClient BREVO = RestClient.builder()
            .requestFactory(brevoRequestFactory())
            .build();

    private final EmailProperties props;

    private static JdkClientHttpRequestFactory brevoRequestFactory() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    // ------------------------------------------------------------------
    // Domain emails
    // ------------------------------------------------------------------

    /** To the OWNER: a new admin has registered; includes the PIN needed to verify them. */
    public void notifyOwnerOfNewAdmin(Admin admin) {
        String content = """
            <p>A new person has registered as an <b>administrator</b> on the DMQ platform and is waiting for your approval.</p>
            <table style="border-collapse:collapse;width:100%;margin:16px 0;font-size:14px;">
              {{rows}}
            </table>
            <p style="margin:20px 0 6px;">Verification PIN:</p>
            <div style="font-size:30px;font-weight:700;letter-spacing:8px;background:#eef3fb;border:2px dashed #0a63c9;
                        border-radius:8px;padding:14px;text-align:center;color:#002f87;">{{pin}}</div>
            <p style="margin-top:20px;">
              <b>If you recognise this person</b>, give them the PIN so they can complete verification.<br>
              <b>If you do not recognise them</b>, do not share the PIN and ignore this email.
            </p>
            """
            .replace("{{rows}}",
                row("Name", admin.getName() + " " + admin.getSurname())
                + row("Email", admin.getEmail())
                + row("Phone", admin.getPhoneNumber())
                + row("Registered", LocalDateTime.now().format(WHEN)))
            .replace("{{pin}}", esc(admin.getVerificationPin()));

        sendToOwner("New admin awaiting your approval: " + admin.getName() + " " + admin.getSurname(),
                layout("New admin registration", content));
    }

    /** To the ADMIN: we received your registration, the owner still has to approve it. */
    public void sendRegistrationReceivedToAdmin(Admin admin) {
        String content = """
            <p>Hi {{name}},</p>
            <p>Your administrator account on the DMQ Plumbers &amp; Renovations platform has been <b>created</b>,
               but it is <b>not verified yet</b>.</p>
            <p>For security, every new admin account must be approved by the business owner. The owner has been
               notified. Please <b>contact the owner</b> to get your <b>verification PIN</b>, then enter it on the
               registration page to activate your account.</p>
            <p>If you closed the page, go back to it, fill in the form again and click <b>Create account</b>.
               The PIN box will appear again.</p>
            <p style="color:#5b6577;font-size:13px;">If you did not create this account, you can safely ignore this email.</p>
            """
            .replace("{{name}}", esc(admin.getName()));

        sendToAdmin(admin, "Your admin account has been created - verification needed",
                layout("Account created", content));
    }

    /** To the ADMIN: verification done, account is active. */
    public void sendAdminVerifiedNotice(Admin admin) {
        String content = """
            <p>Hi {{name}},</p>
            <p>Good news &mdash; your administrator account has been <b>verified</b> and is now active.</p>
            <p style="margin:24px 0;">
              <a href="{{url}}" style="background:#002f87;color:#ffffff;text-decoration:none;font-weight:700;
                 padding:12px 22px;border-radius:8px;display:inline-block;">Log in to your account</a>
            </p>
            """
            .replace("{{name}}", esc(admin.getName()))
            .replace("{{url}}", esc(props.getAdminLoginUrl()));

        sendToAdmin(admin, "Your admin account is now active", layout("Account verified", content));
    }

    /** To the ADMIN: the PIN needed to reset their password. Expires in 5 minutes. */
    public void sendPasswordResetPin(Admin admin) {
        String content = """
            <p>Hi {{name}},</p>
            <p>We received a request to reset the password for your DMQ Plumbers &amp; Renovations
               administrator account.</p>
            <p style="margin:20px 0 6px;">Your password reset PIN:</p>
            <div style="font-size:30px;font-weight:700;letter-spacing:8px;background:#eef3fb;border:2px dashed #0a63c9;
                        border-radius:8px;padding:14px;text-align:center;color:#002f87;">{{pin}}</div>
            <p style="margin-top:20px;">This PIN expires in <b>5 minutes</b>.</p>
            <p style="color:#5b6577;font-size:13px;">If you did not request a password reset, you can safely
               ignore this email; your password will not be changed.</p>
            """
            .replace("{{name}}", esc(admin.getName()))
            .replace("{{pin}}", esc(admin.getPasswordResetPin()));

        sendToAdmin(admin, "Your password reset PIN", layout("Password reset requested", content));
    }

    /** To the ADMIN: confirmation that the password was actually changed. */
    public void sendPasswordResetConfirmation(Admin admin) {
        String content = """
            <p>Hi {{name}},</p>
            <p>Your administrator account password was just changed.</p>
            <p style="color:#5b6577;font-size:13px;">If this wasn't you, please contact the owner immediately.</p>
            """
            .replace("{{name}}", esc(admin.getName()));

        sendToAdmin(admin, "Your password has been changed", layout("Password changed", content));
    }

    // ------------------------------------------------------------------
    // Generic helpers (reusable for other features)
    // ------------------------------------------------------------------

    /** Sends an already-built HTML email to the owner's fixed address, from FROM_ADMIN. */
    public void sendToOwner(String subject, String htmlBody) {
        sendEmail(FROM_ADMIN, props.getOwnerEmail(), subject, htmlBody);
    }

    /** Sends an already-built HTML email to an admin, from FROM_ADMIN. */
    public void sendToAdmin(Admin admin, String subject, String htmlBody) {
        sendEmail(FROM_ADMIN, admin.getEmail(), subject, htmlBody);
    }

    /**
     * Core send via the Brevo API. Throws EmailSendException on failure so callers can decide
     * what to do (e.g. roll back a registration if the owner could not be notified).
     *
     * @param from one of the FROM_* constants above - pick the one matching the email's purpose.
     */
    public void sendEmail(String from, String to, String subject, String htmlBody) {
        String apiKey = props.getBrevoApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Cannot send email to {}: dmq.mail.brevo-api-key (BREVO_API_KEY) is not set", to);
            throw new EmailSendException("Brevo API key is not configured", null);
        }

        Map<String, Object> body = Map.of(
                "sender", Map.of("name", DISPLAY_NAME, "email", from),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody);

        try {
            BREVO.post()
                    .uri(BREVO_SEND_URL)
                    .header("api-key", apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email sent to {} (from: {}, subject: {})", to, from, subject);
        } catch (RestClientResponseException e) {
            // Brevo explains the problem in the body, e.g. {"code":"unauthorized","message":"Key not found"}
            log.error("Brevo rejected email to {} (from: {}, subject: {}): HTTP {} {}",
                    to, from, subject, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new EmailSendException("Could not send email to " + to, e);
        } catch (RestClientException e) {
            log.error("Failed to reach Brevo for email to {} (from: {}, subject: {})", to, from, subject, e);
            throw new EmailSendException("Could not send email to " + to, e);
        }
    }

    // ------------------------------------------------------------------
    // HTML building
    // ------------------------------------------------------------------

    private String layout(String heading, String content) {
        return """
            <div style="background:#eef3fb;padding:24px 12px;font-family:'Segoe UI',Arial,sans-serif;color:#14213d;">
              <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;
                          box-shadow:0 4px 14px rgba(0,30,90,.12);">
                <div style="background:#002f87;padding:20px 24px;border-bottom:5px solid #f9e526;">
                  <div style="color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.5px;">DMQ PLUMBERS</div>
                  <div style="color:#ffffff;opacity:.85;font-size:13px;letter-spacing:2px;">And Renovations</div>
                </div>
                <div style="padding:24px;line-height:1.55;font-size:15px;">
                  <h2 style="margin:0 0 14px;color:#002f87;font-size:20px;">{{heading}}</h2>
                  {{content}}
                </div>
                <div style="background:#002f87;color:#ffffff;font-size:12px;padding:14px 24px;">
                  29 Knight Rd, Turffontein, Johannesburg South, 2190 &nbsp;|&nbsp; +27 71 932 8617
                </div>
              </div>
            </div>
            """
            .replace("{{heading}}", esc(heading))
            .replace("{{content}}", content);
    }

    private String row(String label, String value) {
        return "<tr><td style=\"padding:8px 10px;border-bottom:1px solid #e3e8f2;color:#5b6577;width:110px;\">"
                + esc(label) + "</td><td style=\"padding:8px 10px;border-bottom:1px solid #e3e8f2;font-weight:600;\">"
                + esc(value) + "</td></tr>";
    }

    /** Escapes user-supplied text so names/emails can never inject HTML into an email. */
    private String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }

    // ------------------------------------------------------------------

    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}