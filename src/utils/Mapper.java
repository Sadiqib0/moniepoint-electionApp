package utils;

import data.models.Candidate;
import data.models.Voter;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
import dtos.responses.VoterResponse;

public class Mapper {

    public static Voter map(VoterRegistrationRequest request) {
        Voter voter = new Voter();
        voter.setFirstName(request.getFirstName());
        voter.setLastName(request.getLastName());
        voter.setEmail(request.getEmail());
        voter.setMatricNumber(request.getMatricNumber());
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
}
