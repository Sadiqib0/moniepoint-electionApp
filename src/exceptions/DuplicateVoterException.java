package exceptions;

public class DuplicateVoterException extends ElectionException {
    public DuplicateVoterException(String message) {
        super(message);
    }
}
