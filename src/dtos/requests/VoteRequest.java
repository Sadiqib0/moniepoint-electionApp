package dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank(message = "Voter ID is required")
    private String voterId;

    @NotBlank(message = "Candidate ID is required")
    private String candidateId;

    @NotBlank(message = "Position is required")
    private String position;

    @NotBlank(message = "Token is required")
    private String token;
}
