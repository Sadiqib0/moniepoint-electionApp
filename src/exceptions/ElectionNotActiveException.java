package exceptions;

public class ElectionNotActiveException extends ElectionException {
    public ElectionNotActiveException(String message) {
        super(message);
    }
}
