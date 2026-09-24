package contractor.site.dmq.controllers;

import contractor.site.dmq.dtos.AccountDtos.AccountResponse;
import contractor.site.dmq.dtos.AccountDtos.RegisterAdminRequest;
import contractor.site.dmq.dtos.AccountDtos.Status;
import contractor.site.dmq.dtos.AccountDtos.VerifyAdminRequest;
import contractor.site.dmq.services.AccountManagementService;
import contractor.site.dmq.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private HttpStatus httpStatus(Status status) {
        return switch (status) {
            case PENDING_VERIFICATION, VERIFIED -> HttpStatus.OK;
            case VALIDATION_ERROR, INVALID_PIN -> HttpStatus.BAD_REQUEST;
            case ALREADY_VERIFIED -> HttpStatus.CONFLICT;
            case LOCKED -> HttpStatus.TOO_MANY_REQUESTS;
            case EMAIL_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
