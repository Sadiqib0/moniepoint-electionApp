package controllers;

import dtos.requests.AdminLoginRequest;
import dtos.requests.AdminNominateRequest;
import dtos.requests.AdminRegistrationRequest;
import dtos.requests.CreateElectionRequest;
import dtos.requests.LoginRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import jakarta.validation.Valid;
import dtos.responses.ApiResponse;
import exceptions.ElectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.ElectionService;

@RestController
public class ElectionController {

    @Autowired
    private ElectionService electionService;

    @Autowired
    private services.AdminService adminService;

    // ── Admin endpoints ────────────────────────────────────────────────────────

    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody AdminRegistrationRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, adminService.register(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, adminService.login(request)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    @PatchMapping("/admin/logout")
    public ResponseEntity<?> logoutAdmin(@RequestHeader("X-Admin-Token") String token) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, adminService.logout(token)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    // ── Election management (admin only) ──────────────────────────────────────

    @PostMapping("/election")
    public ResponseEntity<?> createElection(
            @Valid @RequestBody CreateElectionRequest request,
            @RequestHeader("X-Admin-Token") String adminToken) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.createElection(request, adminToken)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/voter")
    public ResponseEntity<?> registerVoter(@Valid @RequestBody VoterRegistrationRequest request) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.registerVoter(request)), HttpStatus.CREATED);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/admin/candidate")
    public ResponseEntity<?> nominateCandidate(
            @Valid @RequestBody AdminNominateRequest request,
            @RequestHeader("X-Admin-Token") String adminToken) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.nominateCandidate(request, adminToken)), HttpStatus.CREATED);
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
    public ResponseEntity<?> getResults(@PathVariable String position) {
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

    @GetMapping("/candidates/{position}")
    public ResponseEntity<?> getAllCandidates(@PathVariable String position) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getAllCandidates(position)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/election/start")
    public ResponseEntity<?> startElection(@RequestHeader("X-Admin-Token") String adminToken) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.startElection(adminToken)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/election/end")
    public ResponseEntity<?> endElection(@RequestHeader("X-Admin-Token") String adminToken) {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.endElection(adminToken)), HttpStatus.OK);
        } catch (ElectionException e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/election/positions")
    public ResponseEntity<?> getElectionPositions() {
        try {
            return new ResponseEntity<>(new ApiResponse(true, electionService.getElectionPositions()), HttpStatus.OK);
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

}
