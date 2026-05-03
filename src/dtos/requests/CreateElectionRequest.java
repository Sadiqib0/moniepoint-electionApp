package dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateElectionRequest {
    @NotBlank(message = "Election name is required")
    private String name;

    @NotEmpty(message = "At least one position is required")
    private List<String> positions;
}
