package services;

import data.models.AuditLog;
import data.models.Position;
import dtos.requests.LoginRequest;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.*;

import java.util.List;

public interface ElectionService {
    VoterResponse registerVoter(VoterRegistrationRequest request);
    CandidateResponse registerCandidate(CandidateRegistrationRequest request);
    VoteResponse castVote(VoteRequest request);
    ElectionResultResponse getResults(Position position);
    LoginResponse login(LoginRequest request);
    LogoutResponse logout(String email);
    VoterResponse getVoter(String id);
    List<VoterResponse> getAllVoters(int page, int size);
    List<CandidateResponse> getAllCandidates(Position position);

    String startElection();
    String endElection();
    String getElectionStatus();
    VoteReceiptResponse verifyVote(String receipt);
    StatsResponse getStats();
    List<AuditLog> getAuditLog(int page, int size);
}
