package exceptions;

public class InvalidVoteException extends ElectionException {
    public InvalidVoteException(String message) {
        super(message);
    }
}
