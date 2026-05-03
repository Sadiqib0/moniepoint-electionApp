package services;

import data.models.AuditLog;
import dtos.requests.CreateElectionRequest;
import dtos.requests.LoginRequest;
import dtos.requests.AdminNominateRequest;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
import dtos.responses.ElectionResultResponse;
import dtos.responses.LoginResponse;
import dtos.responses.StatsResponse;
import dtos.responses.VoteReceiptResponse;
import dtos.responses.VoteResponse;
import dtos.responses.VoterResponse;

import java.util.List;

public interface ElectionService {
    VoterResponse registerVoter(VoterRegistrationRequest request);
    CandidateResponse registerCandidate(CandidateRegistrationRequest request);
    CandidateResponse nominateCandidate(AdminNominateRequest request, String adminToken);
    VoteResponse castVote(VoteRequest request);
    ElectionResultResponse getResults(String position);
    LoginResponse login(LoginRequest request);
    String logout(String email);
    List<CandidateResponse> getAllCandidates(String position);

    String createElection(CreateElectionRequest request, String adminToken);
    String startElection(String adminToken);
    String endElection(String adminToken);
    String getElectionStatus();
    List<String> getElectionPositions();
    VoteReceiptResponse verifyVote(String receipt);
    StatsResponse getStats();
    List<AuditLog> getAuditLog(int page, int size);
}
