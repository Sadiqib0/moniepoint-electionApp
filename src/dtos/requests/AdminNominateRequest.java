package dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminNominateRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Position is required")
    private String position;
}
