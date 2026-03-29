package controllers;

import app.Main;
import data.models.Position;
import data.repositories.CandidateRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
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

    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        voterRepository.deleteAll();
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
        VoterResponse voter1 = electionService.registerVoter(voterRequest1);

        VoterRegistrationRequest voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Musa");
        voterRequest2.setEmail("aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);

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
        VoterResponse voter1 = electionService.registerVoter(voterRequest1);

        VoterRegistrationRequest voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Musa");
        voterRequest2.setEmail("aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);

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
        VoterResponse voter1 = electionService.registerVoter(voterRequest1);

        VoterRegistrationRequest voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Musa");
        voterRequest2.setEmail("aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);
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
}