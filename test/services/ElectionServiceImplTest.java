package services;

import data.models.AuditLog;
import data.repositories.AdminRepository;
import data.repositories.AuditLogRepository;
import data.repositories.CandidateRepository;
import data.repositories.ElectionRepository;
import data.repositories.VoteRepository;
import data.repositories.VoterRepository;
import dtos.requests.*;
import dtos.responses.*;
import exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = app.Main.class)
public class ElectionServiceImplTest {

    @Autowired private ElectionService electionService;
    @Autowired private AdminService adminService;
    @Autowired private VoterRepository voterRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private ElectionRepository electionRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private VoterRegistrationRequest voterRequest;
    private VoterRegistrationRequest voterRequest2;
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

    // ── Admin registration / login / logout ───────────────────────────────────

    @Test
    public void registerAdmin_successTest() {
        AdminRegistrationRequest req = new AdminRegistrationRequest();
        req.setUsername("admin2");
        req.setPassword("pass2");
        adminService.register(req);
        assertEquals(2L, adminRepository.count());
    }

    @Test
    public void registerAdminWithDuplicateUsername_throwsExceptionTest() {
        AdminRegistrationRequest req = new AdminRegistrationRequest();
        req.setUsername("admin");
        req.setPassword("adminpass");
        assertThrows(ElectionException.class, () -> adminService.register(req));
        assertEquals(1L, adminRepository.count());
    }

    @Test
    public void loginAdmin_returnsUsernameAndTokenTest() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("adminpass");
        AdminLoginResponse response = adminService.login(req);
        assertEquals("admin", response.getUsername());
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isBlank());
    }

    @Test
    public void loginAdmin_wrongPassword_throwsExceptionTest() {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("wrongpass");
        assertThrows(InvalidLoginDetailsException.class, () -> adminService.login(req));
    }

    @Test
    public void logoutAdmin_clearsTokenAndLoggedInFlagTest() {
        adminService.logout(adminToken);
        assertNull(adminRepository.findByUsername("admin").get().getToken());
        assertFalse(adminRepository.findByUsername("admin").get().isLoggedIn());
    }

    @Test
    public void logoutAdmin_invalidToken_throwsUnauthorizedExceptionTest() {
        assertThrows(UnauthorizedException.class, () -> adminService.logout("invalid-token"));
    }

    // ── Election lifecycle ────────────────────────────────────────────────────

    @Test
    public void getElectionStatus_afterCreate_returnsNotStartedTest() {
        assertEquals("NOT_STARTED", electionService.getElectionStatus());
    }

    @Test
    public void getElectionStatus_afterStart_returnsOngoingTest() {
        startElection();
        assertEquals("ONGOING", electionService.getElectionStatus());
    }

    @Test
    public void getElectionStatus_noElection_returnsNotCreatedTest() {
        electionRepository.deleteAll();
        assertEquals("NOT_CREATED", electionService.getElectionStatus());
    }

    @Test
    public void getElectionPositions_returnsAllFourPositionsTest() {
        List<String> positions = electionService.getElectionPositions();
        assertEquals(4, positions.size());
        assertTrue(positions.contains("PRESIDENT"));
        assertTrue(positions.contains("VICE_PRESIDENT"));
        assertTrue(positions.contains("GENERAL_SECRETARY"));
        assertTrue(positions.contains("TREASURER"));
    }

    @Test
    public void createElection_withInvalidToken_throwsUnauthorizedExceptionTest() {
        CreateElectionRequest req = new CreateElectionRequest();
        req.setName("Another Election");
        req.setPositions(List.of("PRESIDENT"));
        assertThrows(UnauthorizedException.class, () -> electionService.createElection(req, "bad-token"));
    }

    @Test
    public void startElection_withInvalidToken_throwsUnauthorizedExceptionTest() {
        assertThrows(UnauthorizedException.class, () -> electionService.startElection("bad-token"));
    }

    @Test
    public void endElection_successTest() {
        startElection();
        assertEquals("ONGOING", electionService.getElectionStatus());
        electionService.endElection(adminToken);
        assertEquals("NOT_CREATED", electionService.getElectionStatus());
    }

    @Test
    public void endElection_withInvalidToken_throwsUnauthorizedExceptionTest() {
        startElection();
        assertThrows(UnauthorizedException.class, () -> electionService.endElection("bad-token"));
    }

    @Test
    public void endElection_archivesAuditLogForEachPositionTest() {
        startElection();
        electionService.endElection(adminToken);
        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(4, logs.size());
        logs.forEach(log -> assertEquals("ELECTION_ARCHIVED", log.getOutcome()));
    }

    // ── Admin candidate nomination ────────────────────────────────────────────

    @Test
    public void nominateCandidate_successTest() {
        AdminNominateRequest req = new AdminNominateRequest();
        req.setFullName("John Doe");
        req.setPosition("PRESIDENT");
        CandidateResponse response = electionService.nominateCandidate(req, adminToken);
        assertEquals("John Doe", response.getFullName());
        assertEquals("PRESIDENT", response.getPosition());
        assertEquals(1L, candidateRepository.count());
    }

    @Test
    public void nominateCandidate_withInvalidToken_throwsUnauthorizedExceptionTest() {
        AdminNominateRequest req = new AdminNominateRequest();
        req.setFullName("John Doe");
        req.setPosition("PRESIDENT");
        assertThrows(UnauthorizedException.class, () -> electionService.nominateCandidate(req, "bad-token"));
    }

    @Test
    public void nominateCandidate_duplicate_throwsExceptionTest() {
        AdminNominateRequest req = new AdminNominateRequest();
        req.setFullName("John Doe");
        req.setPosition("PRESIDENT");
        electionService.nominateCandidate(req, adminToken);
        assertThrows(ElectionException.class, () -> electionService.nominateCandidate(req, adminToken));
        assertEquals(1L, candidateRepository.count());
    }

    @Test
    public void nominateCandidate_invalidPosition_throwsExceptionTest() {
        AdminNominateRequest req = new AdminNominateRequest();
        req.setFullName("John Doe");
        req.setPosition("INVALID_POSITION");
        assertThrows(ElectionException.class, () -> electionService.nominateCandidate(req, adminToken));
    }

    @Test
    public void nominateCandidate_afterElectionStarted_throwsExceptionTest() {
        startElection();
        AdminNominateRequest req = new AdminNominateRequest();
        req.setFullName("John Doe");
        req.setPosition("PRESIDENT");
        assertThrows(ElectionException.class, () -> electionService.nominateCandidate(req, adminToken));
    }

    // ── Voter registration ────────────────────────────────────────────────────

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
    public void registerVoter_passwordIsHashedInDatabaseTest() {
        electionService.registerVoter(voterRequest);
        String storedPassword = voterRepository.findByEmail("sadiq@moniepoint.edu").get().getPassword();
        assertNotEquals("password123", storedPassword);
        assertTrue(storedPassword.startsWith("$2a$"));
    }

    // ── Voter candidate registration ──────────────────────────────────────────

    @Test
    public void getAllCandidates_noRegisteredCandidates_emptyListTest() {
        List<CandidateResponse> candidates = electionService.getAllCandidates("PRESIDENT");
        assertEquals(0, candidates.size());
    }

    @Test
    public void registerTwoCandidatesForPresident_getAllCandidates_returnsTwoTest() {
        AdminNominateRequest nominateReq1 = new AdminNominateRequest();
        nominateReq1.setFullName("Sadiq Ibrahim");
        nominateReq1.setPosition("PRESIDENT");
        electionService.nominateCandidate(nominateReq1, adminToken);
        AdminNominateRequest nominateReq2 = new AdminNominateRequest();
        nominateReq2.setFullName("Aliyu Usman");
        nominateReq2.setPosition("PRESIDENT");
        electionService.nominateCandidate(nominateReq2, adminToken);
        assertEquals(2, electionService.getAllCandidates("PRESIDENT").size());
    }

    @Test
    public void registerCandidateForPresident_getAllCandidatesForVP_emptyListTest() {
        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        electionService.nominateCandidate(nominateReq, adminToken);
        assertEquals(0, electionService.getAllCandidates("VICE_PRESIDENT").size());
    }

    @Test
    public void registerSameVoterForDifferentPositions_successTest() {
        AdminNominateRequest nominateReq1 = new AdminNominateRequest();
        nominateReq1.setFullName("Sadiq Ibrahim");
        nominateReq1.setPosition("PRESIDENT");
        electionService.nominateCandidate(nominateReq1, adminToken);
        AdminNominateRequest nominateReq2 = new AdminNominateRequest();
        nominateReq2.setFullName("Sadiq Ibrahim");
        nominateReq2.setPosition("GENERAL_SECRETARY");
        electionService.nominateCandidate(nominateReq2, adminToken);
        assertEquals(2L, candidateRepository.count());
    }

    // ── Voter login / logout ──────────────────────────────────────────────────

    @Test
    public void registerVoter_loginVoter_voterIsLoggedInTest() {
        electionService.registerVoter(voterRequest);
        loginVoter("sadiq@moniepoint.edu", "password123");
        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void loginUnregisteredVoter_throwsInvalidLoginDetailsExceptionTest() {
        assertThrows(InvalidLoginDetailsException.class,
                () -> loginVoter("unknown@moniepoint.edu", "password123"));
    }

    @Test
    public void loginWithWrongPassword_throwsInvalidLoginDetailsExceptionTest() {
        electionService.registerVoter(voterRequest);
        assertThrows(InvalidLoginDetailsException.class,
                () -> loginVoter("sadiq@moniepoint.edu", "wrongpassword"));
        assertFalse(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void loginWithWrongPassword_voterRemainsLoggedOutTest() {
        electionService.registerVoter(voterRequest);
        assertThrows(InvalidLoginDetailsException.class,
                () -> loginVoter("sadiq@moniepoint.edu", "wrongpassword"));
        assertFalse(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void loginTwoVoters_bothAreLoggedInTest() {
        electionService.registerVoter(voterRequest);
        electionService.registerVoter(voterRequest2);
        loginVoter("sadiq@moniepoint.edu", "password123");
        loginVoter("aliyu@moniepoint.edu", "password456");
        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
        assertTrue(voterRepository.findByEmail("aliyu@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void loginVoter_logoutVoter_voterIsLoggedOutTest() {
        electionService.registerVoter(voterRequest);
        loginVoter("sadiq@moniepoint.edu", "password123");
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
        loginVoter("sadiq@moniepoint.edu", "password123");
        electionService.logout("sadiq@moniepoint.edu");
        loginVoter("sadiq@moniepoint.edu", "password123");
        assertTrue(voterRepository.findByEmail("sadiq@moniepoint.edu").get().isLoggedIn());
    }

    @Test
    public void login_returnsNonNullTokenTest() {
        electionService.registerVoter(voterRequest);
        String token = loginVoter("sadiq@moniepoint.edu", "password123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    public void login_tokenStoredOnVoterInDatabaseTest() {
        electionService.registerVoter(voterRequest);
        String token = loginVoter("sadiq@moniepoint.edu", "password123");
        String storedToken = voterRepository.findByEmail("sadiq@moniepoint.edu").get().getToken();
        assertEquals(token, storedToken);
    }

    @Test
    public void logout_clearsTokenFromDatabaseTest() {
        electionService.registerVoter(voterRequest);
        loginVoter("sadiq@moniepoint.edu", "password123");
        electionService.logout("sadiq@moniepoint.edu");
        assertNull(voterRepository.findByEmail("sadiq@moniepoint.edu").get().getToken());
    }

    @Test
    public void loginAgain_generatesNewTokenTest() {
        electionService.registerVoter(voterRequest);
        String firstToken = loginVoter("sadiq@moniepoint.edu", "password123");
        electionService.logout("sadiq@moniepoint.edu");
        String secondToken = loginVoter("sadiq@moniepoint.edu", "password123");
        assertNotEquals(firstToken, secondToken);
    }

    // ── Vote casting ──────────────────────────────────────────────────────────

    @Test
    public void castVote_successTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
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
        assertEquals(1L, voteRepository.count());
    }

    @Test
    public void castVoteTwiceForSamePosition_throwsVoteAlreadyCastExceptionTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
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
        assertThrows(VoteAlreadyCastException.class, () -> electionService.castVote(voteRequest));
        assertEquals(1L, voteRepository.count());
    }

    @Test
    public void castVoteWithoutLogin_throwsVoterNotLoggedInExceptionTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        CandidateResponse candidate = electionService.nominateCandidate(nominateReq, adminToken);
        startElection();
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition("PRESIDENT");
        assertThrows(VoterNotLoggedInException.class, () -> electionService.castVote(voteRequest));
        assertEquals(0L, voteRepository.count());
    }

    @Test
    public void castVoteWithWrongToken_throwsInvalidLoginDetailsExceptionTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
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
        assertThrows(InvalidLoginDetailsException.class, () -> electionService.castVote(voteRequest));
        assertEquals(0L, voteRepository.count());
    }

    @Test
    public void castVoteWithNullToken_throwsInvalidLoginDetailsExceptionTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
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
        voteRequest.setToken(null);
        assertThrows(InvalidLoginDetailsException.class, () -> electionService.castVote(voteRequest));
        assertEquals(0L, voteRepository.count());
    }

    @Test
    public void loginTwice_oldTokenNoLongerWorksTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
        AdminNominateRequest nominateReq = new AdminNominateRequest();
        nominateReq.setFullName("Sadiq Ibrahim");
        nominateReq.setPosition("PRESIDENT");
        CandidateResponse candidate = electionService.nominateCandidate(nominateReq, adminToken);
        startElection();
        String oldToken = loginVoter("aliyu@moniepoint.edu", "password456");
        electionService.logout("aliyu@moniepoint.edu");
        loginVoter("aliyu@moniepoint.edu", "password456");
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setVoterId(voter2.getId());
        voteRequest.setCandidateId(candidate.getId());
        voteRequest.setPosition("PRESIDENT");
        voteRequest.setToken(oldToken);
        assertThrows(InvalidLoginDetailsException.class, () -> electionService.castVote(voteRequest));
    }

    // ── Vote verification ─────────────────────────────────────────────────────

    @Test
    public void verifyVote_successTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
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
        VoteReceiptResponse receipt = electionService.verifyVote(voteResponse.getReceipt());
        assertEquals("PRESIDENT", receipt.getPosition());
        assertNotNull(receipt.getTimestamp());
    }

    @Test
    public void verifyVote_invalidReceipt_throwsInvalidVoteExceptionTest() {
        assertThrows(InvalidVoteException.class, () -> electionService.verifyVote("fake-receipt-code"));
    }

    // ── Results ───────────────────────────────────────────────────────────────

    @Test
    public void getResults_noVotesCast_returnsNoVotesCastYetTest() {
        ElectionResultResponse result = electionService.getResults("PRESIDENT");
        assertEquals(0, result.getTotalVotesCast());
        assertEquals("No votes cast yet", result.getWinnerName());
    }

    @Test
    public void getResults_afterVoting_returnsCorrectWinnerTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
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
        ElectionResultResponse result = electionService.getResults("PRESIDENT");
        assertEquals(1, result.getTotalVotesCast());
        assertEquals("Sadiq Ibrahim", result.getWinnerName());
        assertFalse(result.isTied());
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Test
    public void getStats_noVoters_returnsZerosTest() {
        StatsResponse stats = electionService.getStats();
        assertEquals(0L, stats.getTotalVoters());
        assertEquals(0L, stats.getTotalVoted());
        assertEquals(0.0, stats.getTurnoutPercentage());
    }

    @Test
    public void getStats_afterOneVoteOutOfTwo_returnsFiftyPercentTurnoutTest() {
        electionService.registerVoter(voterRequest);
        VoterResponse voter2 = electionService.registerVoter(voterRequest2);
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
        StatsResponse stats = electionService.getStats();
        assertEquals(2L, stats.getTotalVoters());
        assertEquals(1L, stats.getTotalVoted());
        assertEquals(50.0, stats.getTurnoutPercentage());
        assertEquals(1L, stats.getVotesByPosition().get("PRESIDENT"));
    }

    // ── Audit log ─────────────────────────────────────────────────────────────

    @Test
    public void getAuditLog_beforeEndElection_returnsEmptyListTest() {
        List<AuditLog> logs = electionService.getAuditLog(0, 20);
        assertTrue(logs.isEmpty());
    }

    @Test
    public void getAuditLog_afterEndElection_returnsOneLogPerPositionTest() {
        startElection();
        electionService.endElection(adminToken);
        List<AuditLog> logs = electionService.getAuditLog(0, 20);
        assertEquals(4, logs.size());
        logs.forEach(log -> assertEquals("ELECTION_ARCHIVED", log.getOutcome()));
    }
}
