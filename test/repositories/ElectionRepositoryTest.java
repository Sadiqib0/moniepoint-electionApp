package repositories;

import data.models.Candidate;
import data.models.Position;
import data.models.Voter;
import data.models.Vote;
import data.repositories.CandidateRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = app.Main.class)
public class ElectionRepositoryTest {

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoteRepository voteRepository;

    @BeforeEach
    public void setUp() {
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        voterRepository.deleteAll();
    }

    @Test
    public void newVoterRepository_countIsZero() {
        assertEquals(0L, voterRepository.count());
    }

    @Test
    public void saveVoter_countIsOneTest() {
        Voter voter = new Voter();
        voter.setEmail("sadiq@moniepoint.edu");
        voter.setMatricNumber("CSC/21/0001");
        voterRepository.save(voter);
        assertEquals(1L, voterRepository.count());
    }

    @Test
    public void saveVoter_findByEmail_returnsSavedVoterTest() {
        Voter voter = new Voter();
        voter.setFirstName("Sadiq");
        voter.setEmail("sadiq@moniepoint.edu");
        voter.setMatricNumber("CSC/21/0001");
        voterRepository.save(voter);

        Optional<Voter> found = voterRepository.findByEmail("sadiq@moniepoint.edu");
        assertTrue(found.isPresent());
        assertEquals("Sadiq", found.get().getFirstName());
    }

    @Test
    public void saveVoter_findByMatricNumber_returnsSavedVoterTest() {
        Voter voter = new Voter();
        voter.setLastName("Ibrahim");
        voter.setEmail("sadiq@moniepoint.edu");
        voter.setMatricNumber("CSC/21/0001");
        voterRepository.save(voter);

        Optional<Voter> found = voterRepository.findByMatricNumber("CSC/21/0001");
        assertTrue(found.isPresent());
        assertEquals("Ibrahim", found.get().getLastName());
    }

    @Test
    public void findByEmailThatDoesNotExist_returnsEmptyTest() {
        Optional<Voter> found = voterRepository.findByEmail("random@moniepoint.edu");
        assertFalse(found.isPresent());
    }
    
    @Test
    public void newCandidateRepository_countIsZeroTest() {
        assertEquals(0L, candidateRepository.count());
    }

    @Test
    public void saveCandidate_countIsOneTest() {
        Candidate candidate = new Candidate();
        candidate.setVoterId("voter-001");
        candidate.setFullName("Sadiq Ibrahim");
        candidate.setPosition(Position.PRESIDENT);
        candidateRepository.save(candidate);
        assertEquals(1L, candidateRepository.count());
    }

    @Test
    public void saveCandidate_existsByVoterIdAndPosition_returnsTrueTest() {
        Candidate candidate = new Candidate();
        candidate.setVoterId("voter-001");
        candidate.setFullName("Sadiq Ibrahim");
        candidate.setPosition(Position.PRESIDENT);
        candidateRepository.save(candidate);

        assertTrue(candidateRepository.existsByVoterIdAndPosition("voter-001", Position.PRESIDENT));
    }

    @Test
    public void noCandidateSaved_existsByVoterIdAndPosition_returnsFalseTest() {
        assertFalse(candidateRepository.existsByVoterIdAndPosition("voter-001", Position.PRESIDENT));
    }

    @Test
    public void saveTwoCandidatesForSamePosition_findAllByPosition_returnsBothTest() {
        Candidate c1 = new Candidate();
        c1.setVoterId("voter-001");
        c1.setFullName("Sadiq Ibrahim");
        c1.setPosition(Position.PRESIDENT);
        candidateRepository.save(c1);

        Candidate c2 = new Candidate();
        c2.setVoterId("voter-002");
        c2.setFullName("Aliyu Usman");
        c2.setPosition(Position.PRESIDENT);
        candidateRepository.save(c2);

        List<Candidate> presidents = candidateRepository.findAllByPosition(Position.PRESIDENT);
        assertEquals(2, presidents.size());
    }
    @Test
    public void newVoteRepository_countIsZeroTest() {
        assertEquals(0L, voteRepository.count());
    }

    @Test
    public void saveVote_countIsOneTest() {
        Vote vote = new Vote();
        vote.setVoterId("voter-001");
        vote.setCandidateId("candidate-001");
        vote.setPosition(Position.PRESIDENT);
        vote.setReceipt(java.util.UUID.randomUUID().toString());
        voteRepository.save(vote);
        assertEquals(1L, voteRepository.count());
    }

    @Test
    public void saveVote_existsByVoterIdAndPosition_returnsTrueTest() {
        Vote vote = new Vote();
        vote.setVoterId("voter-001");
        vote.setCandidateId("candidate-001");
        vote.setPosition(Position.PRESIDENT);
        vote.setReceipt(java.util.UUID.randomUUID().toString());
        voteRepository.save(vote);

        assertTrue(voteRepository.existsByVoterIdAndPosition("voter-001", Position.PRESIDENT));
    }

    @Test
    public void noVoteSaved_existsByVoterIdAndPosition_returnsFalseTest() {
        assertFalse(voteRepository.existsByVoterIdAndPosition("voter-001", Position.PRESIDENT));
    }

}

