package dtos.requests;

import data.models.Position;
import lombok.Data;

@Data
public class CandidateRegistrationRequest {
    private String voterId;
    private Position position;

}
