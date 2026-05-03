package dtos.responses;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoteReceiptResponse {
    private String position;
    private LocalDateTime timestamp;
}
