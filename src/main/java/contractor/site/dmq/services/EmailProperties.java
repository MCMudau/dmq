package contractor.site.dmq.services;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Mail settings, bound from application.properties (prefix "dmq.mail").
 *
 * ownerEmail is the fixed business-owner address. It is never taken from user input:
 * every new admin registration is sent to this address so the owner can verify it.
 *
 * Sender addresses (quote@, admin@, noreply@...) are NOT here - they're defined as
 * constants in EmailService itself (FROM_QUOTES / FROM_ADMIN / FROM_NOREPLY), since
 * they're fixed per email type rather than per-environment config.
 */
@Component
@ConfigurationProperties(prefix = "dmq.mail")
@Data
public class EmailProperties {

    /** The owner who approves every new admin. Set once in configuration. */
    private String ownerEmail;

    /** Brevo API key (xkeysib-...), used to send all email over Brevo's HTTPS API. Read from dmq.mail.brevo-api-key. */
    private String brevoApiKey;

    /** Link placed in the "your account is active" email. Read from dmq.mail.admin-login-url. */
    private String adminLoginUrl = "http://localhost:8080/admin/login";
}