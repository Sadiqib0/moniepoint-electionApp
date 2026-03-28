package services;

import data.models.Position;
import data.repositories.CandidateRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.VoterResponse;
import exceptions.CandidateAlreadyExistsException;
import exceptions.DuplicateVoterException;
import exceptions.VoterNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

        voterRequest2 = new VoterRegistrationRequest();
        voterRequest2.setFirstName("Aliyu");
        voterRequest2.setLastName("Usman");
        voterRequest2.setEmail("Aliyu@moniepoint.edu");
        voterRequest2.setMatricNumber("CSC/21/0002");
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
}
