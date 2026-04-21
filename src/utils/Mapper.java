package utils;

import data.models.Candidate;
import data.models.Voter;
import data.models.Vote;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.*;

import java.util.List;


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
        response.setRegisteredAt(voter.getRegisteredAt());
        return response;
    }

    public static Candidate map(CandidateRegistrationRequest request, Voter voter) {
        Candidate candidate = new Candidate();
        candidate.setVoterId(voter.getId());
        candidate.setPosition(request.getPosition());
        candidate.setFullName(voter.getFirstName() + " " + voter.getLastName());
        return candidate;
    }

    public static CandidateResponse map(Candidate candidate) {
        CandidateResponse response = new CandidateResponse();
        response.setId(candidate.getId());
        response.setVoterId(candidate.getVoterId());
        response.setFullName(candidate.getFullName());
        response.setPosition(candidate.getPosition());
        response.setNominatedAt(candidate.getNominatedAt());
        return response;
    }
    public static VoteResponse mapToVoteResponse(Vote vote, Candidate candidate) {
        VoteResponse response = new VoteResponse();
        response.setId(vote.getId());
        response.setVoterId(vote.getVoterId());
        response.setCandidateId(vote.getCandidateId());
        response.setPosition(vote.getPosition());
        response.setReceipt(vote.getReceipt());
        response.setTimestamp(vote.getTimestamp());
        response.setMessage("vote for " + candidate.getFullName() +
                " (" + vote.getPosition() + ") recorded. Keep your receipt to verify your vote.");
        return response;
    }

    public static LoginResponse mapToLoginResponse(Voter voter) {
        LoginResponse response = new LoginResponse();
        response.setId(voter.getId());
        response.setEmail(voter.getEmail());
        response.setLoggedIn(voter.isLoggedIn());
        response.setToken(voter.getToken());
        return response;
    }

    public static LogoutResponse mapToLogoutResponse(Voter voter) {
        LogoutResponse response = new LogoutResponse();
        response.setEmail(voter.getEmail());
        response.setLoggedIn(voter.isLoggedIn());
        response.setMessage("Goodbye " + voter.getFirstName() + ". You have been logged out.");
        return response;
    }
}
