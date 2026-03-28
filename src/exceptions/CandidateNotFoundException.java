package exceptions;

public class CandidateNotFoundException extends ElectionException {
    public CandidateNotFoundException(String message) {
        super(message);
    }
}
