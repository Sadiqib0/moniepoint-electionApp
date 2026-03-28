package exceptions;

public class VoteAlreadyCastException extends ElectionException {
    public VoteAlreadyCastException(String message) {
        super(message);
    }
}
