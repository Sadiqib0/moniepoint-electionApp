package exceptions;

public class InvalidLoginDetailsException extends ElectionException{
    public InvalidLoginDetailsException(String message) {
        super(message);
    }
}
