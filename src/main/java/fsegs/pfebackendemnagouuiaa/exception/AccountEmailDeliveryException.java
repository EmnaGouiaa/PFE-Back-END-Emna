package fsegs.pfebackendemnagouuiaa.exception;

public class AccountEmailDeliveryException extends RuntimeException {

    private final String details;

    public AccountEmailDeliveryException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
