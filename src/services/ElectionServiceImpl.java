package services;

import data.models.Candidate;
import data.models.Voter;
import data.repositories.CandidateRepository;
import data.repositories.VoterRepository;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.responses.CandidateResponse;
import dtos.responses.VoterResponse;
import exceptions.CandidateAlreadyExistsException;
import exceptions.DuplicateVoterException;
import exceptions.VoterNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static utils.Mapper.*;

@Service
public class ElectionServiceImpl implements ElectionService {

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private CandidateRepository candidateRepository;


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

}