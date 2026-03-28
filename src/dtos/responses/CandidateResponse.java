package dtos.responses;


import data.models.Position;
import lombok.Data;

import java.time.LocalDateTime;

@Data

public class CandidateResponse {
    private String id;
    private String voterId;
    private String fullName;
    private Position position;
    private LocalDateTime nominatedAt;
}
