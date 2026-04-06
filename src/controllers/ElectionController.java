package controllers;

import data.models.Position;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import exceptions.ElectionException;
import org.springframework.web.bind.annotation.*;
import services.ElectionService;
import dtos.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
public class ElectionController {

    @Autowired
    private ElectionService electionService;

    @PostMapping("/voter")
    public ResponseEntity<?> registerVoter(@RequestBody VoterRegistrationRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.registerVoter(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/candidate")
    public ResponseEntity<?> registerCandidate(@RequestBody CandidateRegistrationRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.registerCandidate(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping("/vote")
    public ResponseEntity<?> castVote(@RequestBody VoteRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.castVote(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/results/{position}")
    public ResponseEntity<?> getResults(@PathVariable Position position) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getResults(position)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
    @PatchMapping("/voter/login/{email}/{password}")
    public ResponseEntity<?> login(@PathVariable String email, @PathVariable String password) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.login(email, password)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/voter/logout/{email}")
    public ResponseEntity<?> logout(@PathVariable String email) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.logout(email)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/voter/{id}")
    public ResponseEntity<?> getVoter(@PathVariable String id) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getVoter(id)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/voters")
    public ResponseEntity<?> getAllVoters() {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getAllVoters()), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/candidates/{position}")
    public ResponseEntity<?> getAllCandidates(@PathVariable Position position) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getAllCandidates(position)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}