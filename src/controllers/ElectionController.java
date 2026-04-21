package controllers;

import data.models.Position;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.LoginRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import jakarta.validation.Valid;
import dtos.responses.ApiResponse;
import dtos.responses.HealthResponse;
import exceptions.ElectionException;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.ElectionService;

import java.time.LocalDateTime;

@RestController
public class ElectionController {

    @Autowired
    private ElectionService electionService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/voter")
    public ResponseEntity<?> registerVoter(@Valid @RequestBody VoterRegistrationRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.registerVoter(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/candidate")
    public ResponseEntity<?> registerCandidate(@Valid @RequestBody CandidateRegistrationRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.registerCandidate(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/vote")
    public ResponseEntity<?> castVote(@Valid @RequestBody VoteRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.castVote(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/vote/verify/{receipt}")
    public ResponseEntity<?> verifyVote(@PathVariable String receipt) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.verifyVote(receipt)), HttpStatus.OK);
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

    @PostMapping("/voter/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.login(request)), HttpStatus.OK);
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
    public ResponseEntity<?> getAllVoters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getAllVoters(page, size)), HttpStatus.OK);
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

    @PostMapping("/election/start")
    public ResponseEntity<?> startElection() {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.startElection()), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/election/end")
    public ResponseEntity<?> endElection() {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.endElection()), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/election/status")
    public ResponseEntity<?> getElectionStatus() {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getElectionStatus()), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/election/stats")
    public ResponseEntity<?> getStats() {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getStats()), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<?> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getAuditLog(page, size)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            return ResponseEntity.ok(new ApiResponse(true, new HealthResponse("UP", true, LocalDateTime.now())));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, new HealthResponse("DOWN", false, LocalDateTime.now())));
        }
    }
}
