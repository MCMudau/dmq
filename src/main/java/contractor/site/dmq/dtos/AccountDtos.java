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
        
    public record LoginRequest(String email, String password) {

        @Override
        public String toString() {
            return "LoginRequest[email=" + email + "]";
        }
    }

    public record RequestPasswordResetRequest(String email) {}

    public record VerifyResetPinRequest(String email, String pin) {

        @Override
        public String toString() {
            return "VerifyResetPinRequest[email=" + email + "]";
        }
    }

    public record ConfirmPasswordResetRequest(
            String email, String pin, String newPassword, String confirmNewPassword) {

        @Override
        public String toString() {
            return "ConfirmPasswordResetRequest[email=" + email + "]";
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
        EMAIL_FAILED, LOGIN_SUCCESS, INVALID_CREDENTIALS, NOT_VERIFIED,
        /** No admin account exists with that email. */
        RESET_EMAIL_NOT_FOUND,
        /** Email exists, but there is no reset PIN currently pending for it. */
        RESET_PIN_NONE,
        /** A brand-new reset PIN was generated and emailed. */
        RESET_PIN_SENT,
        /** A previously-sent PIN is still inside its 5-minute window; no new email was sent. */
        RESET_PIN_ACTIVE,
        /** The PIN the admin typed was correct; they may now choose a new password. */
        RESET_PIN_VERIFIED,
        /** The PIN existed but its 5-minute window has passed. */
        RESET_PIN_EXPIRED,
        PASSWORD_RESET_SUCCESS
    
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

    public record PasswordResetResponse(Status status, String message, Integer secondsRemaining) {

        public static PasswordResetResponse of(Status status, String message) {
            return new PasswordResetResponse(status, message, null);
        }

        public static PasswordResetResponse withSeconds(Status status, String message, int secondsRemaining) {
            return new PasswordResetResponse(status, message, secondsRemaining);
        }
    }
}
