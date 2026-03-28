package dtos.responses;

import data.models.Position;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class VoterResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String matricNumber;
    private Set<Position> votedPositions;
    private LocalDateTime registeredAt;
}
