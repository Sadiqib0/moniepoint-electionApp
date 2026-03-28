package exceptions;

public class VoterNotFoundException extends ElectionException {
    public VoterNotFoundException(String message) {
        super(message);
    }
}
