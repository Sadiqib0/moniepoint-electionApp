package services;

import data.models.Candidate;
import data.models.Voter;
import data.models.Vote;
import data.models.Position;
import data.repositories.CandidateRepository;
import data.repositories.VoterRepository;
import data.repositories.VoteRepository;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.responses.*;
import exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static utils.Mapper.*;

@Service
public class ElectionServiceImpl implements ElectionService {

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoteRepository voteRepository;


    @Override
    public VoterResponse registerVoter(VoterRegistrationRequest request) {
        if (voterRepository.findByEmail(request.getEmail()).isPresent())
            throw new DuplicateVoterException("A voter with email " + request.getEmail() + " is already registered.");
        if (voterRepository.findByMatricNumber(request.getMatricNumber()).isPresent())
            throw new DuplicateVoterException("A voter with matric number " + request.getMatricNumber() + " is already registered.");

        Voter savedVoter = voterRepository.save(map(request));
        return map(savedVoter);
    }

    @Override
    public CandidateResponse registerCandidate(CandidateRegistrationRequest request) {
        Optional<Voter> optionalVoter = voterRepository.findById(request.getVoterId());

        if (optionalVoter.isEmpty())
            throw new VoterNotFoundException("Voter with ID " + request.getVoterId() + " not found.");

        Voter voter = optionalVoter.get();

        if (candidateRepository.existsByVoterIdAndPosition(request.getVoterId(), request.getPosition()))
            throw new CandidateAlreadyExistsException(voter.getFirstName() + " " + voter.getLastName() + " is already a candidate for " + request.getPosition() + ".");
        Candidate savedCandidate = candidateRepository.save(map(request, voter));
        return map(savedCandidate);
    }
    @Override
    public VoteResponse castVote(VoteRequest request) {
        Optional<Voter> optionalVoter = voterRepository.findById(request.getVoterId());
        if (optionalVoter.isEmpty())
            throw new VoterNotFoundException("Voter with ID " + request.getVoterId() + " not found.");

        Voter voter = optionalVoter.get();
        Optional<Candidate> optionalCandidate = candidateRepository.findById(request.getCandidateId());

        if (optionalCandidate.isEmpty())
            throw new CandidateNotFoundException("Candidate with ID " + request.getCandidateId() + " not found.");

        Candidate candidate = optionalCandidate.get();

        if (!candidate.getPosition().equals(request.getPosition()))
            throw new InvalidVoteException("Candidate " + candidate.getFullName() + " is not contesting for " + request.getPosition() + ".");
        if (voteRepository.existsByVoterIdAndPosition(request.getVoterId(), request.getPosition()))
            throw new VoteAlreadyCastException(voter.getFirstName() + " " + voter.getLastName() + " has already voted for " + request.getPosition() + ".");

        Vote vote = new Vote();
        vote.setVoterId(request.getVoterId());
        vote.setCandidateId(request.getCandidateId());
        vote.setPosition(request.getPosition());

        Vote savedVote = voteRepository.save(vote);
        voter.getVotedPositions().add(request.getPosition());
        voterRepository.save(voter);

        return mapToVoteResponse(savedVote, candidate);
    }

    @Override
    public ElectionResultResponse getResults(Position position) {
        List<Candidate> candidates = candidateRepository.findAllByPosition(position);
        List<Vote> votes = voteRepository.findAllByPosition(position);
        return mapToElectionResult(position, votes, candidates);
    }
    @Override
    public LoginResponse login(String email, String password) {
        Optional<Voter> optionalVoter = voterRepository.findByEmail(email);
        if (optionalVoter.isEmpty())
            throw new InvalidLoginDetailsException("Voter with email " + email + " does not exist.");
        Voter voter = optionalVoter.get();
        if (!voter.getPassword().equals(password))
            throw new InvalidLoginDetailsException("Invalid password. Please try again.");
        voter.setLoggedIn(true);
        Voter savedVoter = voterRepository.save(voter);
        return mapToLoginResponse(savedVoter);
    }

    @Override
    public LogoutResponse logout(String email) {
        Optional<Voter> optionalVoter = voterRepository.findByEmail(email);
        if (optionalVoter.isEmpty())
            throw new InvalidLoginDetailsException("Voter with email " + email + " does not exist.");
        Voter voter = optionalVoter.get();
        if (!voter.isLoggedIn())
            throw new VoterNotLoggedInException(voter.getFirstName() + " is not currently logged in.");
        voter.setLoggedIn(false);
        Voter savedVoter = voterRepository.save(voter);
        return mapToLogoutResponse(savedVoter);
    }


}