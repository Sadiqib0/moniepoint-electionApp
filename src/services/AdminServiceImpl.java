package services;

import data.models.Admin;
import data.repositories.AdminRepository;
import dtos.requests.AdminLoginRequest;
import dtos.requests.AdminRegistrationRequest;
import dtos.responses.AdminLoginResponse;
import exceptions.ElectionException;
import exceptions.InvalidLoginDetailsException;
import exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String register(AdminRegistrationRequest request) {
        if (adminRepository.findByUsername(request.getUsername()).isPresent())
            throw new ElectionException("Admin with username '" + request.getUsername() + "' already exists.");
        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        adminRepository.save(admin);
        return "Admin account created for '" + request.getUsername() + "'.";
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidLoginDetailsException("Invalid username or password."));
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword()))
            throw new InvalidLoginDetailsException("Invalid username or password.");
        admin.setLoggedIn(true);
        admin.setToken(UUID.randomUUID().toString());
        adminRepository.save(admin);
        AdminLoginResponse response = new AdminLoginResponse();
        response.setUsername(admin.getUsername());
        response.setToken(admin.getToken());
        return response;
    }

    @Override
    public String logout(String token) {
        Admin admin = adminRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired admin token."));
        admin.setLoggedIn(false);
        admin.setToken(null);
        adminRepository.save(admin);
        return "Admin '" + admin.getUsername() + "' has been logged out.";
    }
}
