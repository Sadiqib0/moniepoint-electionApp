package services;

import data.models.*;
import data.repositories.*;
import dtos.requests.LoginRequest;
import dtos.requests.CandidateRegistrationRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.responses.*;
import exceptions.*;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import utils.Mapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Mapper.*;

@Service
public class ElectionServiceImpl implements ElectionService {

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String ELECTION_ID = "ELECTION";

    @Override
    public VoterResponse registerVoter(VoterRegistrationRequest request) {
        if (voterRepository.findByEmail(request.getEmail()).isPresent())
            throw new DuplicateVoterException("A voter with email " + request.getEmail() + " is already registered.");
        if (voterRepository.findByMatricNumber(request.getMatricNumber()).isPresent())
            throw new DuplicateVoterException("A voter with matric number " + request.getMatricNumber() + " is already registered.");

        Voter voter = map(request);
        voter.setPassword(passwordEncoder.encode(request.getPassword()));
        Voter savedVoter = voterRepository.save(voter);
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
        ElectionStatus currentStatus = getCurrentElectionStatus();
        if (currentStatus != ElectionStatus.ONGOING) {
            String reason = currentStatus == ElectionStatus.NOT_STARTED
                    ? "The election has not started yet."
                    : "The election has ended.";
            saveAuditLog(request.getVoterId(), request.getPosition(), "ELECTION_NOT_ACTIVE", reason);
            throw new ElectionNotActiveException(reason);
        }

        Optional<Voter> optionalVoter = voterRepository.findById(request.getVoterId());
        if (optionalVoter.isEmpty()) {
            saveAuditLog(request.getVoterId(), request.getPosition(), "VOTER_NOT_FOUND", "Voter ID not found");
            throw new VoterNotFoundException("Voter with ID " + request.getVoterId() + " not found.");
        }

        Voter voter = optionalVoter.get();

        if (!voter.isLoggedIn()) {
            saveAuditLog(voter.getId(), request.getPosition(), "VOTER_NOT_LOGGED_IN",
                    voter.getFirstName() + " is not logged in");
            throw new VoterNotLoggedInException(voter.getFirstName() + " " + voter.getLastName() + " must be logged in to cast a vote.");
        }

        if (voter.getToken() == null || !voter.getToken().equals(request.getToken())) {
            saveAuditLog(voter.getId(), request.getPosition(), "INVALID_TOKEN",
                    voter.getFirstName() + " provided an invalid session token");
            throw new InvalidLoginDetailsException("Invalid session token. Please log in again.");
        }

        Optional<Candidate> optionalCandidate = candidateRepository.findById(request.getCandidateId());
        if (optionalCandidate.isEmpty()) {
            saveAuditLog(voter.getId(), request.getPosition(), "INVALID_CANDIDATE", "Candidate ID not found");
            throw new CandidateNotFoundException("Candidate with ID " + request.getCandidateId() + " not found.");
        }

        Candidate candidate = optionalCandidate.get();

        if (!candidate.getPosition().equals(request.getPosition())) {
            saveAuditLog(voter.getId(), request.getPosition(), "INVALID_CANDIDATE",
                    candidate.getFullName() + " is not contesting for " + request.getPosition());
            throw new InvalidVoteException("Candidate " + candidate.getFullName() + " is not contesting for " + request.getPosition() + ".");
        }

        if (voteRepository.existsByVoterIdAndPosition(request.getVoterId(), request.getPosition())) {
            saveAuditLog(voter.getId(), request.getPosition(), "DUPLICATE_VOTE",
                    voter.getFirstName() + " already voted for " + request.getPosition());
            throw new VoteAlreadyCastException(voter.getFirstName() + " " + voter.getLastName() + " has already voted for " + request.getPosition() + ".");
        }

        Vote vote = new Vote();
        vote.setVoterId(request.getVoterId());
        vote.setCandidateId(request.getCandidateId());
        vote.setPosition(request.getPosition());
        vote.setReceipt(UUID.randomUUID().toString());

        Vote savedVote = voteRepository.save(vote);
        voter.getVotedPositions().add(request.getPosition());
        voterRepository.save(voter);

        saveAuditLog(voter.getId(), request.getPosition(), "SUCCESS",
                voter.getFirstName() + " voted for " + candidate.getFullName());

        return mapToVoteResponse(savedVote, candidate);
    }

    @Override
    public ElectionResultResponse getResults(Position position) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("position").is(position.name())),
                Aggregation.group("candidateId").count().as("voteCount"),
                Aggregation.sort(Sort.Direction.DESC, "voteCount")
        );
        AggregationResults<Document> aggResults = mongoTemplate.aggregate(
                aggregation, "vote", Document.class
        );

        List<Candidate> candidates = candidateRepository.findAllByPosition(position);
        Map<String, String> candidateNames = candidates.stream()
                .collect(Collectors.toMap(Candidate::getId, Candidate::getFullName));

        List<ElectionResultResponse.CandidateResult> breakdown = new ArrayList<>();
        long totalVotes = 0;
        for (Document doc : aggResults.getMappedResults()) {
            String candidateId = doc.getString("_id");
            long count = ((Number) doc.get("voteCount")).longValue();
            totalVotes += count;
            ElectionResultResponse.CandidateResult result = new ElectionResultResponse.CandidateResult();
            result.setCandidateId(candidateId);
            result.setCandidateName(candidateNames.getOrDefault(candidateId, "Unknown"));
            result.setVoteCount(count);
            breakdown.add(result);
        }

        ElectionResultResponse response = new ElectionResultResponse();
        response.setPosition(position);
        response.setTotalVotesCast((int) totalVotes);
        response.setBreakdown(breakdown);

        if (breakdown.isEmpty()) {
            response.setWinnerName("No votes cast yet");
            response.setTied(false);
            response.setWinners(new ArrayList<>());
            response.setWinnerVoteCount(0);
        } else {
            long topCount = breakdown.get(0).getVoteCount();
            List<ElectionResultResponse.CandidateResult> winners = breakdown.stream()
                    .filter(r -> r.getVoteCount() == topCount)
                    .collect(Collectors.toList());
            boolean tied = winners.size() > 1;
            response.setTied(tied);
            response.setWinners(winners);
            response.setWinnerName(tied ? "TIE — " + winners.stream()
                    .map(ElectionResultResponse.CandidateResult::getCandidateName)
                    .collect(Collectors.joining(", ")) : winners.get(0).getCandidateName());
            response.setWinnerVoteCount(topCount);
        }

        return response;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<Voter> optionalVoter = voterRepository.findByEmail(request.getEmail());
        if (optionalVoter.isEmpty())
            throw new InvalidLoginDetailsException("Voter with email " + request.getEmail() + " does not exist.");
        Voter voter = optionalVoter.get();
        if (!passwordEncoder.matches(request.getPassword(), voter.getPassword()))
            throw new InvalidLoginDetailsException("Invalid password. Please try again.");
        voter.setLoggedIn(true);
        voter.setToken(UUID.randomUUID().toString());
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
        voter.setToken(null);
        Voter savedVoter = voterRepository.save(voter);
        return mapToLogoutResponse(savedVoter);
    }

    @Override
    public VoterResponse getVoter(String id) {
        Optional<Voter> optionalVoter = voterRepository.findById(id);
        if (optionalVoter.isEmpty())
            throw new VoterNotFoundException("Voter with ID " + id + " not found.");
        return map(optionalVoter.get());
    }

    @Override
    public List<VoterResponse> getAllVoters(int page, int size) {
        return voterRepository.findAll(PageRequest.of(page, size))
                .stream()
                .map(Mapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<CandidateResponse> getAllCandidates(Position position) {
        return candidateRepository.findAllByPosition(position)
                .stream()
                .map(Mapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public String startElection() {
        Election election = getOrCreateElection();
        if (election.getStatus() == ElectionStatus.ENDED)
            throw new ElectionNotActiveException("Election has already ended and cannot be restarted.");
        election.setStatus(ElectionStatus.ONGOING);
        electionRepository.save(election);
        return "Election is now ONGOING.";
    }

    @Override
    public String endElection() {
        Election election = getOrCreateElection();
        if (election.getStatus() != ElectionStatus.ONGOING)
            throw new ElectionNotActiveException("Election is not currently ongoing.");
        election.setStatus(ElectionStatus.ENDED);
        electionRepository.save(election);
        return "Election has ENDED.";
    }

    @Override
    public String getElectionStatus() {
        return getOrCreateElection().getStatus().name();
    }

    @Override
    public VoteReceiptResponse verifyVote(String receipt) {
        Optional<Vote> optionalVote = voteRepository.findByReceipt(receipt);
        if (optionalVote.isEmpty())
            throw new InvalidVoteException("No vote found for receipt: " + receipt);
        Vote vote = optionalVote.get();
        VoteReceiptResponse response = new VoteReceiptResponse();
        response.setReceipt(receipt);
        response.setPosition(vote.getPosition());
        response.setTimestamp(vote.getTimestamp());
        response.setMessage("Your vote for " + vote.getPosition() + " has been recorded.");
        return response;
    }

    @Override
    public StatsResponse getStats() {
        long totalVoters = voterRepository.count();
        long totalVoted = voterRepository.countByVotedPositionsNotEmpty();
        double turnout = totalVoters == 0 ? 0.0 : Math.round((totalVoted * 100.0 / totalVoters) * 100.0) / 100.0;

        Aggregation positionAgg = Aggregation.newAggregation(
                Aggregation.group("position").count().as("count")
        );
        AggregationResults<Document> positionResults = mongoTemplate.aggregate(
                positionAgg, "vote", Document.class
        );

        Map<String, Long> votesByPosition = new LinkedHashMap<>();
        for (Document doc : positionResults.getMappedResults()) {
            votesByPosition.put(doc.getString("_id"), ((Number) doc.get("count")).longValue());
        }

        StatsResponse stats = new StatsResponse();
        stats.setTotalVoters(totalVoters);
        stats.setTotalVoted(totalVoted);
        stats.setTurnoutPercentage(turnout);
        stats.setVotesByPosition(votesByPosition);
        return stats;
    }

    @Override
    public List<AuditLog> getAuditLog(int page, int size) {
        return auditLogRepository.findAll(PageRequest.of(page, size)).getContent();
    }


    private Election getOrCreateElection() {
        return electionRepository.findById(ELECTION_ID).orElseGet(() -> {
            Election election = new Election();
            election.setId(ELECTION_ID);
            return electionRepository.save(election);
        });
    }

    private ElectionStatus getCurrentElectionStatus() {
        return electionRepository.findById(ELECTION_ID)
                .map(Election::getStatus)
                .orElse(ElectionStatus.NOT_STARTED);
    }

    private void saveAuditLog(String voterId, Position position, String outcome, String details) {
        AuditLog log = new AuditLog();
        log.setVoterId(voterId);
        log.setPosition(position);
        log.setOutcome(outcome);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
