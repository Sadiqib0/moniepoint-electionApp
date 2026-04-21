package controllers;

import app.Main;
import data.models.Position;
import data.repositories.CandidateRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import data.repositories.ElectionRepository;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.LoginRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
import dtos.responses.LoginResponse;
import dtos.responses.VoterResponse;
import services.ElectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ElectionControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ElectionService electionService;

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private ElectionRepository electionRepository;

    private WebTestClient webTestClient;

    private String loginVoter(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return electionService.login(request).getToken();
    }

    @BeforeEach
    public void setUp() {
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        voterRepository.deleteAll();
        electionRepository.deleteAll();
        electionService.startElection();
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

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
    public void registerCandidate_successTest() {
        VoterRegistrationRequest voterRequest = new VoterRegistrationRequest();
        voterRequest.setFirstName("Sadiq");
        voterRequest.setLastName("Ibrahim");
        voterRequest.setEmail("sadiq@moniepoint.edu");
        voterRequest.setMatricNumber("CSC/21/0001");
        voterRequest.setPassword("password123");
        VoterResponse voter = electionService.registerVoter(voterRequest);

        CandidateRegistrationRequest request = new CandidateRegistrationRequest();
        request.setVoterId(voter.getId());
        request.setPosition(Position.PRESIDENT);

        webTestClient.post()
                .uri("/candidate")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.fullName").isEqualTo("Sadiq Ibrahim")
                .jsonPath("$.data.position").isEqualTo("PRESIDENT");
    }

    @Test
    public void registerCandidateUnknownVoter_returnsErrorTest() {
        CandidateRegistrationRequest request = new CandidateRegistrationRequest();
        request.setVoterId("unknown-id");
        request.setPosition(Position.PRESIDENT);

        webTestClient.post()
                .uri("/candidate")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void castVote_successTest() {
        VoterRegistrationRequest voterRequest1 = new VoterRegistrationRequest();
        voterRequest1.setFirstName("Sadiq");
        voterRequest1.setLastName("Ibrahim");
        voterRequest1.setEmail("sadiq@moniepoint.edu");
        voterRequest1.setMatricNumber("CSC/21/0001");
        voterRequest1.setPassword("password123");
        VoterResponse voter1 = electionService.registerVoter(voterRequest1);

        VoterRegistrationRequest voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Musa");
        voterRequest2.setEmail("aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
        voterRequest2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        String token = loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);
        voteRequest.setToken(token);

        webTestClient.post()
                .uri("/vote")
                .bodyValue(voteRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.position").isEqualTo("PRESIDENT");
    }

    @Test
    public void castVoteTwice_returnsErrorTest() {
        VoterRegistrationRequest voterRequest1 = new VoterRegistrationRequest();
        voterRequest1.setFirstName("Sadiq");
        voterRequest1.setLastName("Ibrahim");
        voterRequest1.setEmail("sadiq@moniepoint.edu");
        voterRequest1.setMatricNumber("CSC/21/0001");
        voterRequest1.setPassword("password123");
        VoterResponse voter1 = electionService.registerVoter(voterRequest1);

        VoterRegistrationRequest voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Musa");
        voterRequest2.setEmail("aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
        voterRequest2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        String token = loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);
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
    public void getResults_returnsWinnerTest() {
        VoterRegistrationRequest voterRequest1 = new VoterRegistrationRequest();
        voterRequest1.setFirstName("Sadiq");
        voterRequest1.setLastName("Ibrahim");
        voterRequest1.setEmail("sadiq@moniepoint.edu");
        voterRequest1.setMatricNumber("CSC/21/0001");
        voterRequest1.setPassword("password123");
        VoterResponse voter1 = electionService.registerVoter(voterRequest1);

        VoterRegistrationRequest voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Musa");
        voterRequest2.setEmail("aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
        voterRequest2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        String token = loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);
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

    @Test
    public void registerVoter_missingFields_returns400WithErrorMessageTest() {
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
    public void castVoteWithWrongToken_returns400Test() {
        VoterRegistrationRequest voterRequest1 = new VoterRegistrationRequest();
        voterRequest1.setFirstName("Sadiq"); voterRequest1.setLastName("Ibrahim");
        voterRequest1.setEmail("sadiq@moniepoint.edu"); voterRequest1.setMatricNumber("CSC/21/0001");
        voterRequest1.setPassword("password123");
        VoterResponse voter1 = electionService.registerVoter(voterRequest1);

        VoterRegistrationRequest voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu"); voterRequest2.setLastName("Musa");
        voterRequest2.setEmail("aliyu@moniepoint.edu"); voterRequest2.setMatricNumber("CSC/21/0002");
        voterRequest2.setPassword("password456");
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        loginVoter("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);
        voteRequest.setToken("wrong-token");

        webTestClient.post()
                .uri("/vote")
                .bodyValue(voteRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(false);
    }

    @Test
    public void login_responseContainsTokenTest() {
        VoterRegistrationRequest request = new VoterRegistrationRequest();
        request.setFirstName("Sadiq"); request.setLastName("Ibrahim");
        request.setEmail("sadiq@moniepoint.edu"); request.setMatricNumber("CSC/21/0001");
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
    public void getAllVoters_withPagination_returnsPagedResultsTest() {
        VoterRegistrationRequest req1 = new VoterRegistrationRequest();
        req1.setFirstName("Sadiq"); req1.setLastName("Ibrahim");
        req1.setEmail("sadiq@moniepoint.edu"); req1.setMatricNumber("CSC/21/0001");
        req1.setPassword("password123");
        electionService.registerVoter(req1);

        VoterRegistrationRequest req2 = new VoterRegistrationRequest();
        req2.setFirstName("Aliyu"); req2.setLastName("Musa");
        req2.setEmail("aliyu@moniepoint.edu"); req2.setMatricNumber("CSC/21/0002");
        req2.setPassword("password456");
        electionService.registerVoter(req2);

        webTestClient.get()
                .uri("/voters?page=0&size=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.length()").isEqualTo(1);

        webTestClient.get()
                .uri("/voters?page=1&size=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(true)
                .jsonPath("$.data.length()").isEqualTo(1);
    }
}