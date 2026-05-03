package exceptions;

public class UnauthorizedException extends ElectionException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
