package dtos.responses;

import lombok.Data;

@Data
public class LogoutResponse {
    private String email;
    private boolean isLoggedIn;
    private String message;
}
