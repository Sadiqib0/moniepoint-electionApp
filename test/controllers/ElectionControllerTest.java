package controllers;

import app.Main;
import data.repositories.AdminRepository;
import data.repositories.AuditLogRepository;
import data.repositories.CandidateRepository;
import data.repositories.ElectionRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import dtos.requests.*;
import dtos.responses.*;
import services.AdminService;
import services.ElectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ElectionControllerTest {

    @LocalServerPort
    private int port;

    @Autowired private ElectionService electionService;
    @Autowired private AdminService adminService;
    @Autowired private VoterRepository voterRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private ElectionRepository electionRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private WebTestClient webTestClient;
    private String adminToken;

    private String loginVoter(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return electionService.login(req).getToken();
    }

    private void startElection() {
        electionService.startElection(adminToken);
    }

    @BeforeEach
    public void setUp() {
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        voterRepository.deleteAll();
        electionRepository.deleteAll();
        adminRepository.deleteAll();
        auditLogRepository.deleteAll();

        AdminRegistrationRequest adminReg = new AdminRegistrationRequest();
        adminReg.setUsername("admin");
        adminReg.setPassword("adminpass");
        adminService.register(adminReg);

        AdminLoginRequest adminLogin = new AdminLoginRequest();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("adminpass");
        adminToken = adminService.login(adminLogin).getToken();

        CreateElectionRequest electionReq = new CreateElectionRequest();
        electionReq.setName("Test Election");
        electionReq.setPositions(List.of("PRESIDENT", "VICE_PRESIDENT", "GENERAL_SECRETARY", "TREASURER"));
        electionService.createElection(electionReq, adminToken);

        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @Test
    public void registerAdmin_successTest() {
        AdminRegistrationRequest request = new AdminRegistrationRequest();
        request.setUsername("admin2");
        request.setPassword("adminpass2");

        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true);
    }

    @Test
    public void registerAdminTwice_returnsErrorTest() {
        AdminRegistrationRequest request = new AdminRegistrationRequest();
        request.setUsername("admin");
        request.setPassword("adminpass");

        webTestClient.post()
                .uri("/admin/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void loginAdmin_successTest() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("adminpass");

        webTestClient.post()
                .uri("/admin/login")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.username").isEqualTo("admin")
                .jsonPath("$.data.token").isNotEmpty();
    }

    @Test
    public void loginAdmin_wrongPassword_returnsErrorTest() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpass");

        webTestClient.post()
                .uri("/admin/login")
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void logoutAdmin_successTest() {
        webTestClient.patch()
                .uri("/admin/logout")
                .header("X-Admin-Token", adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true);
    }

    @Test
    public void logoutAdmin_invalidToken_returnsErrorTest() {
        webTestClient.patch()
                .uri("/admin/logout")
                .header("X-Admin-Token", "bad-token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    // ── Election management endpoints ─────────────────────────────────────────

    @Test
    public void createElection_successTest() {
        electionRepository.deleteAll();
        CreateElectionRequest request = new CreateElectionRequest();
        request.setName("New Election");
        request.setPositions(List.of("PRESIDENT", "VICE_PRESIDENT"));

        webTestClient.post()
                .uri("/election")
                .header("X-Admin-Token", adminToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true);
    }

    @Test
    public void createElection_withoutToken_returnsErrorTest() {
        CreateElectionRequest request = new CreateElectionRequest();
        request.setName("New Election");
        request.setPositions(List.of("PRESIDENT"));

        webTestClient.post()
                .uri("/election")
                .header("X-Admin-Token", "bad-token")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void startElection_successTest() {
        webTestClient.post()
                .uri("/election/start")
                .header("X-Admin-Token", adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true);
    }

    @Test
    public void startElection_withoutToken_returnsErrorTest() {
        webTestClient.post()
                .uri("/election/start")
                .header("X-Admin-Token", "bad-token")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void endElection_successTest() {
        startElection();

        webTestClient.post()
                .uri("/election/end")
                .header("X-Admin-Token", adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true);
    }

    @Test
    public void getElectionStatus_returnsNotStartedAfterCreateTest() {
        webTestClient.get()
                .uri("/election/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data").isEqualTo("NOT_STARTED");
    }

    @Test
    public void getElectionStatus_returnsOngoingAfterStartTest() {
        startElection();

        webTestClient.get()
                .uri("/election/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data").isEqualTo("ONGOING");
    }

    @Test
    public void getElectionPositions_returnsFourPositionsTest() {
        webTestClient.get()
                .uri("/election/positions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.length()").isEqualTo(4);
    }

    @Test
    public void getElectionStats_returnsStatsTest() {
        webTestClient.get()
                .uri("/election/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.totalVoters").isEqualTo(0)
                .jsonPath("$.data.totalVoted").isEqualTo(0);
    }

    // ── Admin nomination endpoint ─────────────────────────────────────────────

    @Test
    public void nominateCandidate_successTest() {
        AdminNominateRequest request = new AdminNominateRequest();
        request.setFullName("John Doe");
        request.setPosition("PRESIDENT");

        webTestClient.post()
                .uri("/admin/candidate")
                .header("X-Admin-Token", adminToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.fullName").isEqualTo("John Doe")
                .jsonPath("$.data.position").isEqualTo("PRESIDENT");
    }

    @Test
    public void nominateCandidate_withoutToken_returnsErrorTest() {
        AdminNominateRequest request = new AdminNominateRequest();
        request.setFullName("John Doe");
        request.setPosition("PRESIDENT");

        webTestClient.post()
                .uri("/admin/candidate")
                .header("X-Admin-Token", "bad-token")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void nominateCandidate_invalidPosition_returnsErrorTest() {
        AdminNominateRequest request = new AdminNominateRequest();
        request.setFullName("John Doe");
        request.setPosition("INVALID_POSITION");

        webTestClient.post()
                .uri("/admin/candidate")
                .header("X-Admin-Token", adminToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    // ── Voter registration endpoints ──────────────────────────────────────────

    @Test
    public void registerVoter_successTest() {
        VoterRegistrationRequest request = new VoterRegistrationRequest();
        request.setFirstName("Sadiq");
        request.setLastName("Ibrahim");
        request.setEmail("sadiq@moniepoint.edu");
        request.setMatricNumber("CSC/21/0001");
        request.setPassword("password123");

        webTestClient.post()
                .uri("/voter")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.email").isEqualTo("sadiq@moniepoint.edu");
    }

    @Test
    public void registerVoterTwice_returnsErrorTest() {
        VoterRegistrationRequest request = new VoterRegistrationRequest();
        request.setFirstName("Sadiq");
        request.setLastName("Ibrahim");
        request.setEmail("sadiq@moniepoint.edu");
        request.setMatricNumber("CSC/21/0001");
        request.setPassword("password123");
        electionService.registerVoter(request);

        webTestClient.post()
                .uri("/voter")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void registerVoter_missingFields_returns400Test() {
        VoterRegistrationRequest request = new VoterRegistrationRequest();
        request.setFirstName("Sadiq");

        webTestClient.post()
                .uri("/voter")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false)
                .jsonPath("$.data").isNotEmpty();
    }

    @Test
    public void registerVoter_invalidEmail_returns400Test() {
        VoterRegistrationRequest request = new VoterRegistrationRequest();
        request.setFirstName("Sadiq");
        request.setLastName("Ibrahim");
        request.setEmail("not-an-email");
        request.setMatricNumber("CSC/21/0001");
        request.setPassword("password123");

        webTestClient.post()
                .uri("/voter")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void registerVoter_shortPassword_returns400Test() {
        VoterRegistrationRequest request = new VoterRegistrationRequest();
        request.setFirstName("Sadiq");
        request.setLastName("Ibrahim");
        request.setEmail("sadiq@moniepoint.edu");
        request.setMatricNumber("CSC/21/0001");
        request.setPassword("abc");

        webTestClient.post()
                .uri("/voter")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    // ── Login / logout voter endpoints ────────────────────────────────────────

    @Test
    public void login_responseContainsTokenTest() {
        VoterRegistrationRequest request = new VoterRegistrationRequest();
        request.setFirstName("Sadiq");
        request.setLastName("Ibrahim");
        request.setEmail("sadiq@moniepoint.edu");
        request.setMatricNumber("CSC/21/0001");
        request.setPassword("password123");
        electionService.registerVoter(request);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("sadiq@moniepoint.edu");
        loginRequest.setPassword("password123");

        webTestClient.post()
                .uri("/voter/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.token").isNotEmpty();
    }

    @Test
    public void login_missingEmail_returns400Test() {
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");

        webTestClient.post()
                .uri("/voter/login")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void logoutVoter_successTest() {
        VoterRegistrationRequest voterReq = new VoterRegistrationRequest();
        voterReq.setFirstName("Sadiq");
        voterReq.setLastName("Ibrahim");
        voterReq.setEmail("sadiq@moniepoint.edu");
        voterReq.setMatricNumber("CSC/21/0001");
        voterReq.setPassword("password123");
        electionService.registerVoter(voterReq);
        loginVoter("sadiq@moniepoint.edu", "password123");

        webTestClient.patch()
                .uri("/voter/logout/sadiq@moniepoint.edu")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true);
    }

    // ── Vote casting endpoints ────────────────────────────────────────────────

    @Test
    public void castVote_successTest() {
        VoterRegistrationRequest voterReq1 = new VoterRegistrationRequest();
        voterReq1.setFirstName("Sadiq"); voterReq1.setLastName("Ibrahim");
        voterReq1.setEmail("sadiq@moniepoint.edu"); voterReq1.setMatricNumber("CSC/21/0001");
        voterReq1.setPassword("password123");
        electionService.registerVoter(voterReq1);

        VoterRegistrationRequest voterReq2 = new VoterRegistrationRequest();
        voterReq2.setFirstName("Aliyu"); voterReq2.setLastName("Musa");
        voterReq2.setEmail("aliyu@moniepoint.edu"); voterReq2.setMatricNumber("CSC/21/0002");
        voterReq2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterReq2);

        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        CandidateResponse candidate = electionService.nominateCandidate(nominateReq, adminToken);

        startElection();
        String token = loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition("PRESIDENT");
        voteRequest.setToken(token);

        webTestClient.post()
                .uri("/vote")
                .bodyValue(voteRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.receipt").isNotEmpty();
    }

    @Test
    public void castVoteTwice_returnsErrorTest() {
        VoterRegistrationRequest voterReq1 = new VoterRegistrationRequest();
        voterReq1.setFirstName("Sadiq"); voterReq1.setLastName("Ibrahim");
        voterReq1.setEmail("sadiq@moniepoint.edu"); voterReq1.setMatricNumber("CSC/21/0001");
        voterReq1.setPassword("password123");
        electionService.registerVoter(voterReq1);

        VoterRegistrationRequest voterReq2 = new VoterRegistrationRequest();
        voterReq2.setFirstName("Aliyu"); voterReq2.setLastName("Musa");
        voterReq2.setEmail("aliyu@moniepoint.edu"); voterReq2.setMatricNumber("CSC/21/0002");
        voterReq2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterReq2);

        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        CandidateResponse candidate = electionService.nominateCandidate(nominateReq, adminToken);

        startElection();
        String token = loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition("PRESIDENT");
        voteRequest.setToken(token);
        electionService.castVote(voteRequest);

        webTestClient.post()
                .uri("/vote")
                .bodyValue(voteRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void castVoteWithWrongToken_returns400Test() {
        VoterRegistrationRequest voterReq1 = new VoterRegistrationRequest();
        voterReq1.setFirstName("Sadiq"); voterReq1.setLastName("Ibrahim");
        voterReq1.setEmail("sadiq@moniepoint.edu"); voterReq1.setMatricNumber("CSC/21/0001");
        voterReq1.setPassword("password123");
        electionService.registerVoter(voterReq1);

        VoterRegistrationRequest voterReq2 = new VoterRegistrationRequest();
        voterReq2.setFirstName("Aliyu"); voterReq2.setLastName("Musa");
        voterReq2.setEmail("aliyu@moniepoint.edu"); voterReq2.setMatricNumber("CSC/21/0002");
        voterReq2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterReq2);

        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        CandidateResponse candidate = electionService.nominateCandidate(nominateReq, adminToken);

        startElection();
        loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition("PRESIDENT");
        voteRequest.setToken("wrong-token");

        webTestClient.post()
                .uri("/vote")
                .bodyValue(voteRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    // ── Vote verification endpoint ────────────────────────────────────────────

    @Test
    public void verifyVote_successTest() {
        VoterRegistrationRequest voterReq1 = new VoterRegistrationRequest();
        voterReq1.setFirstName("Sadiq"); voterReq1.setLastName("Ibrahim");
        voterReq1.setEmail("sadiq@moniepoint.edu"); voterReq1.setMatricNumber("CSC/21/0001");
        voterReq1.setPassword("password123");
        electionService.registerVoter(voterReq1);

        VoterRegistrationRequest voterReq2 = new VoterRegistrationRequest();
        voterReq2.setFirstName("Aliyu"); voterReq2.setLastName("Musa");
        voterReq2.setEmail("aliyu@moniepoint.edu"); voterReq2.setMatricNumber("CSC/21/0002");
        voterReq2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterReq2);

        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        CandidateResponse candidate = electionService.nominateCandidate(nominateReq, adminToken);

        startElection();
        String token = loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition("PRESIDENT");
        voteRequest.setToken(token);
        VoteResponse voteResponse = electionService.castVote(voteRequest);

        webTestClient.get()
                .uri("/vote/verify/" + voteResponse.getReceipt())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.position").isEqualTo("PRESIDENT")
                .jsonPath("$.data.timestamp").isNotEmpty();
    }

    @Test
    public void verifyVote_invalidReceipt_returnsErrorTest() {
        webTestClient.get()
                .uri("/vote/verify/fake-receipt-code")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    // ── Results endpoint ──────────────────────────────────────────────────────

    @Test
    public void getResults_returnsWinnerTest() {
        VoterRegistrationRequest voterReq1 = new VoterRegistrationRequest();
        voterReq1.setFirstName("Sadiq"); voterReq1.setLastName("Ibrahim");
        voterReq1.setEmail("sadiq@moniepoint.edu"); voterReq1.setMatricNumber("CSC/21/0001");
        voterReq1.setPassword("password123");
        electionService.registerVoter(voterReq1);

        VoterRegistrationRequest voterReq2 = new VoterRegistrationRequest();
        voterReq2.setFirstName("Aliyu"); voterReq2.setLastName("Musa");
        voterReq2.setEmail("aliyu@moniepoint.edu"); voterReq2.setMatricNumber("CSC/21/0002");
        voterReq2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterReq2);

        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        CandidateResponse candidate = electionService.nominateCandidate(nominateReq, adminToken);

        startElection();
        String token = loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition("PRESIDENT");
        voteRequest.setToken(token);
        electionService.castVote(voteRequest);

        webTestClient.get()
                .uri("/results/PRESIDENT")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.winnerName").isEqualTo("Sadiq Ibrahim")
                .jsonPath("$.data.totalVotesCast").isEqualTo(1);
    }

    @Test
    public void getResults_noVotes_returnsNoVotesCastYetTest() {
        webTestClient.get()
                .uri("/results/PRESIDENT")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.winnerName").isEqualTo("No votes cast yet")
                .jsonPath("$.data.totalVotesCast").isEqualTo(0);
    }

    // ── Audit log endpoint ────────────────────────────────────────────────────

    @Test
    public void getAuditLog_afterEndElection_returnsLogsTest() {
        startElection();
        electionService.endElection(adminToken);

        webTestClient.get()
                .uri("/audit?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.length()").isEqualTo(4);
    }

    @Test
    public void getAuditLog_beforeEndElection_returnsEmptyListTest() {
        webTestClient.get()
                .uri("/audit?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.length()").isEqualTo(0);
    }
}
