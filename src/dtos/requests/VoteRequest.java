package dtos.requests;

import data.models.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank(message = "Voter ID is required")
    private String voterId;

    @NotBlank(message = "Candidate ID is required")
    private String candidateId;

    @NotNull(message = "Position is required")
    private Position position;

    @NotBlank(message = "Token is required")
    private String token;
}
