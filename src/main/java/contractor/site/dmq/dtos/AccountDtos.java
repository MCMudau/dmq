package contractor.site.dmq.dtos;

import java.util.Map;

/** Request/response shapes for the admin account endpoints. */
public final class AccountDtos {

    private AccountDtos() {}

    public record RegisterAdminRequest(
            String name,
            String surname,
            String email,
            String phoneNumber,
            String password,
            String confirmPassword) {

        /** Never print passwords if this object ends up in a log line. */
        @Override
        public String toString() {
            return "RegisterAdminRequest[email=" + email + "]";
        }
    }

    public record VerifyAdminRequest(String email, String pin) {

        @Override
        public String toString() {
            return "VerifyAdminRequest[email=" + email + "]";
        }
    }

    public enum Status {
        /** Account exists (just created, or created earlier) and still needs the owner's PIN. */
        PENDING_VERIFICATION,
        /** An account with this email exists and is already verified. */
        ALREADY_VERIFIED,
        /** One or more submitted fields are in the wrong format. */
        VALIDATION_ERROR,
        /** PIN accepted, account is now active. */
        VERIFIED,
        INVALID_PIN,
        /** Too many wrong PINs; temporarily locked. */
        LOCKED,
        /** The owner could not be emailed, so nothing was saved. */
        EMAIL_FAILED
    }

    public record AccountResponse(Status status, String message, boolean newAccount, Map<String, String> fieldErrors) {

        public static AccountResponse of(Status status, String message) {
            return new AccountResponse(status, message, false, Map.of());
        }

        public static AccountResponse pending(String message, boolean newAccount) {
            return new AccountResponse(Status.PENDING_VERIFICATION, message, newAccount, Map.of());
        }

        public static AccountResponse invalid(Map<String, String> fieldErrors) {
            return new AccountResponse(Status.VALIDATION_ERROR,
                    "Please correct the highlighted fields.", false, fieldErrors);
        }
    }
}
