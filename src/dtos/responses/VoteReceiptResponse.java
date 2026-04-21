package dtos.responses;

import data.models.Position;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoteReceiptResponse {
    private String receipt;
    private Position position;
    private LocalDateTime timestamp;
    private String message;
}
