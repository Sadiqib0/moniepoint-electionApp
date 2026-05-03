package dtos.responses;

import lombok.Data;

@Data
public class CandidateResponse {
    private String id;
    private String fullName;
    private String position;
}
