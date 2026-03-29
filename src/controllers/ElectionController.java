package controllers;

import data.models.Position;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import exceptions.ElectionException;
import services.ElectionService;
import dtos.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

}