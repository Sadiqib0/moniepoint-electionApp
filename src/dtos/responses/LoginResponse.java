package dtos.responses;

import lombok.Data;

import java.util.Set;

@Data
public class LoginResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String token;
    private Set<String> votedPositions;
}
