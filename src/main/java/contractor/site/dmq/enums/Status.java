package contractor.site.dmq.enums;

public enum Status {
     /** The owner could not be emailed, so nothing was saved. */
        EMAIL_FAILED,
        /** Email and password are correct and the account is verified. */
        LOGIN_SUCCESS,
        /** Wrong email or password (deliberately the same answer for both). */
        INVALID_CREDENTIALS,
        /** Email and password are correct, but the owner has not verified this admin yet. */
        NOT_VERIFIED,
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