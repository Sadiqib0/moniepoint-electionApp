package exceptions;

public class VoterNotLoggedInException extends ElectionException{
    public VoterNotLoggedInException(String message) {
        super(message);
    }
}
