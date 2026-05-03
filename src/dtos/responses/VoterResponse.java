package dtos.responses;

import lombok.Data;

import java.util.Set;

@Data
public class VoterResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String matricNumber;
    private Set<String> votedPositions;
}
