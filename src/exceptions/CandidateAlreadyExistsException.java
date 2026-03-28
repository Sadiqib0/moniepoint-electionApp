package exceptions;

public class CandidateAlreadyExistsException extends ElectionException {
    public CandidateAlreadyExistsException(String message) {
        super(message);
    }
}
