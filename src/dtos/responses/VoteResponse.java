package dtos.responses;

import data.models.Position;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoteResponse {
    private String id;
    private String voterId;
    private String candidateId;
    private Position position;
    private LocalDateTime timestamp;
    private String message;
}
