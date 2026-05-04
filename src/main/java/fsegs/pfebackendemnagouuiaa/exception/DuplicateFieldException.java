package fsegs.pfebackendemnagouuiaa.exception;

public class DuplicateFieldException extends RuntimeException {

    private final String field;

    public DuplicateFieldException(String message) {
        super(message);
        this.field = null;
    }

    public DuplicateFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
