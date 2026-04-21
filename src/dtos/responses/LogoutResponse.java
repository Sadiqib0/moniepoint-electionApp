package dtos.responses;

import lombok.Data;

@Data
public class LogoutResponse {
    private String email;
    private boolean loggedIn;
    private String message;
}
