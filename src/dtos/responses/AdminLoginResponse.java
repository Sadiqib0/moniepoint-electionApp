package dtos.responses;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private String username;
    private String token;
}
