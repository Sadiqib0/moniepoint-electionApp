package dtos.requests;

import data.models.Position;
import lombok.Data;

@Data
public class VoteRequest {
    private String voterId;
    private String candidateId;
    private Position position;
}
