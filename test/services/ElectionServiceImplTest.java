package services;

import data.models.Position;
import data.repositories.CandidateRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
import dtos.responses.ElectionResultResponse;
import dtos.responses.VoterResponse;
import exceptions.InvalidLoginDetailsException;
import exceptions.VoterNotLoggedInException;
import exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = app.Main.class)
public class ElectionServiceImplTest {

    @Autowired
    private ElectionService electionService;
    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoteRepository voteRepository;

    private VoterRegistrationRequest voterRequest;
    private VoterRegistrationRequest voterRequest2;

    @BeforeEach
    public void setUp() {
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        voterRepository.deleteAll();

        voterRequest = new VoterRegistrationRequest();
        voterRequest.setFirstName("Sadiq");
        voterRequest.setLastName("Ibrahim");
        voterRequest.setEmail("sadiq@moniepoint.edu");
        voterRequest.setMatricNumber("CSC/21/0001");
        voterRequest.setPassword("password123");

        voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Usman");
        voterRequest2.setEmail("aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
        voterRequest2.setPassword("password456");
    }
    @Test
    public void registerVoter_successTest() {
        assertEquals(0L, voterRepository.count());
        electionService.registerVoter(voterRequest);
        assertEquals(1L, voterRepository.count());
    }

    @Test
    public void registerTwoVoters_countIsTwoTest() {
        electionService.registerVoter(voterRequest);
        electionService.registerVoter(voterRequest2);
        assertEquals(2L, voterRepository.count());
    }

    @Test
    public void registerVoterWithSameEmail_throwsDuplicateVoterExceptionTest() {
        electionService.registerVoter(voterRequest);
        assertThrows(DuplicateVoterException.class, () -> electionService.registerVoter(voterRequest));
        assertEquals(1L, voterRepository.count());
    }

    @Test
    public void registerVoterWithSameMatricNumber_throwsDuplicateVoterExceptionTest() {
        electionService.registerVoter(voterRequest);

        VoterRegistrationRequest duplicate = new VoterRegistrationRequest();
        duplicate.setFirstName("Mahmoud");
        duplicate.setLastName("Bello");
        duplicate.setEmail("tunde@moniepoint.edu");
        duplicate.setMatricNumber("CSC/21/0001");

        assertThrows(DuplicateVoterException.class, () -> electionService.registerVoter(duplicate));
        assertEquals(1L, voterRepository.count());
    }


    @Test
    public void registerCandidate_successTest() {
        VoterResponse voter = electionService.registerVoter(voterRequest);
        assertEquals(0L, candidateRepository.count());

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter.getId());
        candidateRequest.setPosition(Position.PRESIDENT);

        electionService.registerCandidate(candidateRequest);
        assertEquals(1L, candidateRepository.count());
    }

    @Test
    public void registerCandidateWithUnknownVoterId_throwsVoterNotFoundExceptionTest() {
        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId("unknown-id");
        candidateRequest.setPosition(Position.PRESIDENT);

        assertThrows(VoterNotFoundException.class, () -> electionService.registerCandidate(candidateRequest));
        assertEquals(0L, candidateRepository.count());
    }

    @Test
    public void registerSameVoterTwiceForSamePosition_throwsCandidateAlreadyExistsExceptionTest() {
        VoterResponse voter = electionService.registerVoter(voterRequest);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter.getId());
        candidateRequest.setPosition(Position.PRESIDENT);

        electionService.registerCandidate(candidateRequest);
        assertThrows(CandidateAlreadyExistsException.class, () -> electionService.registerCandidate(candidateRequest));
        assertEquals(1L, candidateRepository.count());
    }
    @Test
    public void getAllCandidates_noRegisteredCandidates_emptyListTest() {
        List<CandidateResponse> candidates = electionService.getAllCandidates(Position.PRESIDENT);
        assertEquals(0, candidates.size());
    }

    @Test
    public void registerTwoCandidatesForPresident_getAllCandidates_returnsTwoTest() {
        VoterResponse voter1 = electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest request1 = new CandidateRegistrationRequest();
        request1.setVoterId(voter1.getId());
        request1.setPosition(Position.PRESIDENT);

        CandidateRegistrationRequest request2 = new CandidateRegistrationRequest();
        request2.setVoterId(voter2.getId());
        request2.setPosition(Position.PRESIDENT);

        electionService.registerCandidate(request1);
        electionService.registerCandidate(request2);

        List<CandidateResponse> candidates = electionService.getAllCandidates(Position.PRESIDENT);
        assertEquals(2, candidates.size());
    }

    @Test
    public void registerCandidateForPresident_getAllCandidatesForVP_emptyListTest() {
        VoterResponse voter1 = electionService.registerVoter(voterRequest);

        CandidateRegistrationRequest request = new CandidateRegistrationRequest();
        request.setVoterId(voter1.getId());
        request.setPosition(Position.PRESIDENT);
        electionService.registerCandidate(request);

        List<CandidateResponse> candidates = electionService.getAllCandidates(Position.VICE_PRESIDENT);
        assertEquals(0, candidates.size());
    }

    @Test
    public void registerSameVoterForDifferentPositions_successTest() {
        VoterResponse voter = electionService.registerVoter(voterRequest);

        CandidateRegistrationRequest presidentRequest = new CandidateRegistrationRequest();
        presidentRequest.setVoterId(voter.getId());
        presidentRequest.setPosition(Position.PRESIDENT);

        CandidateRegistrationRequest secretaryRequest = new CandidateRegistrationRequest();
        secretaryRequest.setVoterId(voter.getId());
        secretaryRequest.setPosition(Position.GENERAL_SECRETARY);

        electionService.registerCandidate(presidentRequest);
        electionService.registerCandidate(secretaryRequest);
        assertEquals(2L, candidateRepository.count());
    }
    @Test
    public void getAllVoters_emptyList_countIsZeroTest() {
        List<VoterResponse> voters = electionService.getAllVoters();
        assertEquals(0, voters.size());
    }

    @Test
    public void registerTwoVoters_getAllVoters_returnsTwoTest() {
        electionService.registerVoter(voterRequest);
        electionService.registerVoter(voterRequest2);
        List<VoterResponse> voters = electionService.getAllVoters();
        assertEquals(2, voters.size());
    }
    @Test
    public void castVote_successTest() {
        VoterResponse voter1 = electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        assertEquals(0L, voteRepository.count());

        electionService.login("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);

        electionService.castVote(voteRequest);
        assertEquals(1L, voteRepository.count());
    }

    @Test
    public void castVoteTwiceForSamePosition_throwsVoteAlreadyCastExceptionTest() {
        VoterResponse voter1 = electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        electionService.login("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);

        electionService.castVote(voteRequest);
        assertThrows(VoteAlreadyCastException.class, () -> electionService.castVote(voteRequest));
        assertEquals(1L, voteRepository.count());
    }

    @Test
    public void castVoteWithoutLogin_throwsVoterNotLoggedInExceptionTest() {
        VoterResponse voter1 = electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);

        assertThrows(VoterNotLoggedInException.class, () -> electionService.castVote(voteRequest));
        assertEquals(0L, voteRepository.count());
    }

    @Test
    public void loginThenCastVote_successTest() {
        VoterResponse voter1 = electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);

        CandidateRegistrationRequest candidateRequest = new CandidateRegistrationRequest();
        candidateRequest.setVoterId(voter1.getId());
        candidateRequest.setPosition(Position.PRESIDENT);
        CandidateResponse candidate = electionService.registerCandidate(candidateRequest);

        electionService.login("aliyu@moniepoint.edu", "password456");

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition(Position.PRESIDENT);

        electionService.castVote(voteRequest);
        assertEquals(1L, voteRepository.count());
    }
    @Test
    public void getVoter_successTest() {
        VoterResponse saved = electionService.registerVoter(voterRequest);
        VoterResponse found = electionService.getVoter(saved.getId());
        assertEquals(saved.getId(), found.getId());
        assertEquals("sadiq@moniepoint.edu", found.getEmail());
    }

    @Test
    public void getVoterWithUnknownId_throwsVoterNotFoundExceptionTest() {
        assertThrows(VoterNotFoundException.class,
                () -> electionService.getVoter("unknown-id"));
    }


    @Test
    public void getResults_noVotesCast_returnsNoVotesCastYetTest() {
        ElectionResultResponse result = electionService.getResults(Position.PRESIDENT);
        assertEquals(0, result.getTotalVotesCast());
        assertEquals("No votes cast yet", result.getWinnerName());
    }
    
    @Test
    public void registerVoter_loginVoter_voterIsLoggedInTest() {
        electionService.registerVoter(voterRequest);
        electionService.login("sadiq@moniepoint.edu", "password123");
        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void loginUnregisteredVoter_throwsInvalidLoginDetailsExceptionTest() {
        assertThrows(InvalidLoginDetailsException.class,
                () -> electionService.login("unknown@moniepoint.edu", "password123"));
    }

    @Test
    public void loginWithWrongPassword_throwsInvalidLoginDetailsExceptionTest() {
        electionService.registerVoter(voterRequest);
        assertThrows(InvalidLoginDetailsException.class,
                () -> electionService.login("sadiq@moniepoint.edu", "wrongpassword"));
        assertFalse(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void loginWithWrongPassword_voterRemainsLoggedOutTest() {
        electionService.registerVoter(voterRequest);
        assertThrows(InvalidLoginDetailsException.class,
                () -> electionService.login("sadiq@moniepoint.edu", "wrongpassword"));
        assertEquals(1L, voterRepository.count());
        assertFalse(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void loginTwoVoters_bothAreLoggedInTest() {
        electionService.registerVoter(voterRequest);
        electionService.registerVoter(voterRequest2);
        electionService.login("sadiq@moniepoint.edu", "password123");
        electionService.login("aliyu@moniepoint.edu", "password456");
        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
        assertTrue(voterRepository.findByEmail("aliyu@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void registerVoter_loginVoter_logoutVoter_voterIsLoggedOutTest() {
        electionService.registerVoter(voterRequest);
        electionService.login("sadiq@moniepoint.edu", "password123");
        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
        electionService.logout("sadiq@moniepoint.edu");
        assertFalse(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void logoutUnregisteredVoter_throwsInvalidLoginDetailsExceptionTest() {
        assertThrows(InvalidLoginDetailsException.class, () -> electionService.logout("unknown@moniepoint.edu"));
    }

    @Test
    public void logoutVoterWhoIsNotLoggedIn_throwsVoterNotLoggedInExceptionTest() {
        electionService.registerVoter(voterRequest);
        assertThrows(VoterNotLoggedInException.class, () -> electionService.logout("sadiq@moniepoint.edu"));
    }

    @Test
    public void loginVoter_logoutVoter_loginAgain_voterIsLoggedInTest() {
        electionService.registerVoter(voterRequest);
        electionService.login("sadiq@moniepoint.edu", "password123");
        electionService.logout("sadiq@moniepoint.edu");
        assertFalse(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
        electionService.login("sadiq@moniepoint.edu", "password123");
        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }
}
