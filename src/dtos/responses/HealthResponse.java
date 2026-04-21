package dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private boolean dbConnected;
    private LocalDateTime timestamp;
}
