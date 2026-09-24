package contractor.site.dmq.services;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Mail settings, bound from application.properties (prefix "dmq.mail").
 *
 * ownerEmail is the fixed business-owner address. It is never taken from user input:
 * every new admin registration is sent to this address so the owner can verify it.
 */
@Component
@ConfigurationProperties(prefix = "dmq.mail")
@Data
public class EmailProperties {

    /** The owner who approves every new admin. Set once in configuration. */
    private String ownerEmail;

    /** Sender address. With Gmail/most providers this must match the SMTP login. */
    private String fromAddress;

    /** Display name shown in the recipient's inbox. */
    private String fromName = "DMQ Plumbers & Renovations";

    /** Link placed in the "your account is active" email. */
    private String adminLoginUrl = "http://localhost:8080/admin/login";

    public String getAdminLoginUrl() {
       return adminLoginUrl;
    }
}
