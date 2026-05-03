package services;

import dtos.requests.AdminLoginRequest;
import dtos.requests.AdminRegistrationRequest;
import dtos.responses.AdminLoginResponse;

public interface AdminService {
    String register(AdminRegistrationRequest request);
    AdminLoginResponse login(AdminLoginRequest request);
    String logout(String token);
}
