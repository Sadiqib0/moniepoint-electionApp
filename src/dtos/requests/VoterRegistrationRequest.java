package dtos.requests;


import lombok.Data;

@Data

public class VoterRegistrationRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String matricNumber;

}
