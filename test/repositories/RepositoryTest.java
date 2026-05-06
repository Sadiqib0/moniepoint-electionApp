package repositories;

import app.Main;
import data.models.Admin;
import data.models.AuditLog;
import data.models.Candidate;
import data.models.Vote;
import data.models.Voter;
import data.repositories.AdminRepository;
import data.repositories.AuditLogRepository;
import data.repositories.CandidateRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Main.class)
public class RepositoryTest {

    @Autowired private VoterRepository voterRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @BeforeEach
    public void setUp() {
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        voterRepository.deleteAll();
        adminRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    @Test
    public void voterRepository_findByEmailAndMatricAndCountByVotedPositionsNotEmptyTest() {
        Voter voter1 = new Voter();
        voter1.setFirstName("Sadiq");
        voter1.setLastName("Ibrahim");
        voter1.setEmail("sadiq@moniepoint.edu");
        voter1.setMatricNumber("CSC/21/0001");
        voter1.setPassword("password");
        voter1.setVotedPositions(Set.of("PRESIDENT"));
        voterRepository.save(voter1);

        Voter voter2 = new Voter();
        voter2.setFirstName("Aliyu");
        voter2.setLastName("Usman");
        voter2.setEmail("aliyu@moniepoint.edu");
        voter2.setMatricNumber("CSC/21/0002");
        voter2.setPassword("password");
        voterRepository.save(voter2);

        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").isPresent());
        assertTrue(voterRepository.findByMatricNumber("CSC/21/0001").isPresent());
        assertEquals(1L, voterRepository.countByVotedPositionsNotEmpty());
    }

    @Test
    public void candidateRepository_customQueriesTest() {
        Candidate candidate = new Candidate();
        candidate.setVoterId("VOTER_1");
        candidate.setFullName("John Doe");
        candidate.setPosition("PRESIDENT");
        candidateRepository.save(candidate);

        assertTrue(candidateRepository.existsByVoterIdAndPosition("VOTER_1", "PRESIDENT"));
        assertTrue(candidateRepository.existsByFullNameIgnoreCaseAndPosition("john doe", "PRESIDENT"));
        assertEquals(1, candidateRepository.findAllByPosition("PRESIDENT").size());
    }

    @Test
    public void voteRepository_customQueriesTest() {
        Vote vote = new Vote();
        vote.setVoterId("VOTER_1");
        vote.setCandidateId("CAND_1");
        vote.setPosition("PRESIDENT");
        vote.setReceipt("RECEIPT_123");
        voteRepository.save(vote);

        assertTrue(voteRepository.existsByVoterIdAndPosition("VOTER_1", "PRESIDENT"));
        assertTrue(voteRepository.findByReceipt("RECEIPT_123").isPresent());
        assertEquals(1L, voteRepository.countByPosition("PRESIDENT"));
    }

    @Test
    public void adminRepository_findByUsernameAndTokenTest() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword("encoded-password");
        admin.setToken("TOKEN_123");
        admin.setLoggedIn(true);
        adminRepository.save(admin);

        assertTrue(adminRepository.findByUsername("admin").isPresent());
        assertTrue(adminRepository.findByToken("TOKEN_123").isPresent());
    }

    @Test
    public void auditLogRepository_findAllByOutcomeTest() {
        AuditLog successLog = new AuditLog();
        successLog.setVoterId("VOTER_1");
        successLog.setPosition("PRESIDENT");
        successLog.setOutcome("SUCCESS");
        successLog.setDetails("Vote cast successfully");
        auditLogRepository.save(successLog);

        AuditLog failedLog = new AuditLog();
        failedLog.setVoterId("VOTER_2");
        failedLog.setPosition("PRESIDENT");
        failedLog.setOutcome("INVALID_TOKEN");
        failedLog.setDetails("Invalid token");
        auditLogRepository.save(failedLog);

        Page<AuditLog> page = auditLogRepository.findAllByOutcome("SUCCESS", PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
        assertEquals("SUCCESS", page.getContent().get(0).getOutcome());
    }
}
