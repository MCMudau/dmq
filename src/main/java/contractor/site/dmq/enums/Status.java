package contractor.site.dmq.enums;

public enum Status {
     /** The owner could not be emailed, so nothing was saved. */
        EMAIL_FAILED,
        /** Email and password are correct and the account is verified. */
        LOGIN_SUCCESS,
        /** Wrong email or password (deliberately the same answer for both). */
        INVALID_CREDENTIALS,
        /** Email and password are correct, but the owner has not verified this admin yet. */
        NOT_VERIFIED
}
