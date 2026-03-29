package services;

import data.models.Position;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
import dtos.responses.ElectionResultResponse;
import dtos.responses.LoginResponse;
import dtos.responses.LogoutResponse;
import dtos.responses.VoteResponse;
import dtos.responses.VoterResponse;

public interface ElectionService {
    VoterResponse registerVoter(VoterRegistrationRequest request);
    CandidateResponse registerCandidate(CandidateRegistrationRequest request);
    VoteResponse castVote(VoteRequest request);
    ElectionResultResponse getResults(Position position);
    LoginResponse login(String email, String password);
    LogoutResponse logout(String email);
}
