package dtos.responses;

import lombok.Data;

@Data
public class LoginResponse {
    private String id;
    private String email;
    private boolean loggedIn;
    private String token;
}
