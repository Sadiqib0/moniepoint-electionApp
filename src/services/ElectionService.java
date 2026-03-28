package services;

import data.models.Position;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
import dtos.responses.ElectionResultResponse;
import dtos.responses.VoteResponse;
import dtos.responses.VoterResponse;

public interface ElectionService {
    VoterResponse registerVoter(VoterRegistrationRequest request);
    CandidateResponse registerCandidate(CandidateRegistrationRequest request);
}
