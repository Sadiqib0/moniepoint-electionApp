package utils;

import data.models.Candidate;
import data.models.Voter;
import data.models.Vote;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.*;

public class Mapper {

    public static Voter map(VoterRegistrationRequest request) {
        Voter voter = new Voter();
        voter.setFirstName(request.getFirstName());
        voter.setLastName(request.getLastName());
        voter.setEmail(request.getEmail());
        voter.setMatricNumber(request.getMatricNumber());
        voter.setPassword(request.getPassword());
        return voter;
    }

    public static VoterResponse map(Voter voter) {
        VoterResponse response = new VoterResponse();
        response.setId(voter.getId());
        response.setFirstName(voter.getFirstName());
        response.setLastName(voter.getLastName());
        response.setEmail(voter.getEmail());
        response.setMatricNumber(voter.getMatricNumber());
        response.setVotedPositions(voter.getVotedPositions());
        return response;
    }

    public static CandidateResponse map(Candidate candidate) {
        CandidateResponse response = new CandidateResponse();
        response.setId(candidate.getId());
        response.setFullName(candidate.getFullName());
        response.setPosition(candidate.getPosition());
        return response;
    }
    public static VoteResponse mapToVoteResponse(Vote vote) {
        VoteResponse response = new VoteResponse();
        response.setReceipt(vote.getReceipt());
        return response;
    }

    public static LoginResponse mapToLoginResponse(Voter voter) {
        LoginResponse response = new LoginResponse();
        response.setId(voter.getId());
        response.setFirstName(voter.getFirstName());
        response.setLastName(voter.getLastName());
        response.setEmail(voter.getEmail());
        response.setToken(voter.getToken());
        response.setVotedPositions(voter.getVotedPositions());
        return response;
    }

}
