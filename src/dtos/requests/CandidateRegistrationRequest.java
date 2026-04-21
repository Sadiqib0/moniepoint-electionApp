package dtos.requests;

import data.models.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CandidateRegistrationRequest {
    @NotBlank(message = "Voter ID is required")
    private String voterId;

    @NotNull(message = "Position is required")
    private Position position;
}
