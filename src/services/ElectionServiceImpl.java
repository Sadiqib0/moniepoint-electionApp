package services;

import data.models.*;
import data.repositories.*;
import dtos.requests.AdminNominateRequest;
import dtos.requests.CreateElectionRequest;
import exceptions.UnauthorizedException;
import dtos.requests.LoginRequest;
import dtos.requests.VoterRegistrationRequest;
import dtos.requests.VoteRequest;
import dtos.responses.*;
import exceptions.*;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private AdminRepository adminRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String ELECTION_ID = "ELECTION";

    @Override
    public String createElection(CreateElectionRequest request, String adminToken) {
        validateAdminToken(adminToken);
        Optional<Election> existing = electionRepository.findById(ELECTION_ID);
        if (existing.isPresent() && existing.get().getStatus() == ElectionStatus.ONGOING)
            throw new ElectionException("An election is currently ongoing. End it before creating a new one.");
        if (existing.isPresent()) {
            electionRepository.deleteById(ELECTION_ID);
            candidateRepository.deleteAll();
            voteRepository.deleteAll();
            mongoTemplate.updateMulti(new Query(), new Update().set("votedPositions", new HashSet<>()), Voter.class);
        }
        Election election = new Election();
        election.setId(ELECTION_ID);
        election.setName(request.getName());
        election.setPositions(request.getPositions());
        electionRepository.save(election);
        return "Election '" + request.getName() + "' created with positions: " + String.join(", ", request.getPositions());
    }

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
    public CandidateResponse nominateCandidate(AdminNominateRequest request, String adminToken) {
        validateAdminToken(adminToken);
        Election election = electionRepository.findById(ELECTION_ID)
                .orElseThrow(() -> new ElectionException("No election has been created yet."));
        if (election.getStatus() != ElectionStatus.NOT_STARTED)
            throw new ElectionException("Nominations are closed. The election has already " +
                    (election.getStatus() == ElectionStatus.ONGOING ? "started." : "ended."));
        String position = request.getPosition().toUpperCase();
        boolean validPosition = election.getPositions().stream()
                .map(String::toUpperCase)
                .anyMatch(p -> p.equals(position));
        if (!validPosition)
            throw new ElectionException("Position '" + request.getPosition() + "' is not valid. Valid: " +
                    String.join(", ", election.getPositions()));
        if (candidateRepository.existsByFullNameIgnoreCaseAndPosition(request.getFullName(), position))
            throw new ElectionException(request.getFullName() + " is already nominated for " + position + ".");
        Candidate candidate = new Candidate();
        candidate.setVoterId("ADMIN_" + UUID.randomUUID());
        candidate.setPosition(position);
        candidate.setFullName(request.getFullName());
        return map(candidateRepository.save(candidate));
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
        String position = request.getPosition().toUpperCase();

        if (!candidate.getPosition().equalsIgnoreCase(position)) {
            saveAuditLog(voter.getId(), position, "INVALID_CANDIDATE",
                    candidate.getFullName() + " is not contesting for " + position);
            throw new InvalidVoteException("Candidate " + candidate.getFullName() + " is not contesting for " + position + ".");
        }

        if (voteRepository.existsByVoterIdAndPosition(request.getVoterId(), position)) {
            saveAuditLog(voter.getId(), position, "DUPLICATE_VOTE",
                    voter.getFirstName() + " already voted for " + position);
            throw new VoteAlreadyCastException(voter.getFirstName() + " " + voter.getLastName() + " has already voted for " + position + ".");
        }

        Vote vote = new Vote();
        vote.setVoterId(request.getVoterId());
        vote.setCandidateId(request.getCandidateId());
        vote.setPosition(position);
        vote.setReceipt(UUID.randomUUID().toString());

        Vote savedVote = voteRepository.save(vote);
        voter.getVotedPositions().add(position);
        voterRepository.save(voter);

        saveAuditLog(voter.getId(), position, "SUCCESS",
                voter.getFirstName() + " voted for " + candidate.getFullName());

        return mapToVoteResponse(savedVote);
    }

    @Override
    public ElectionResultResponse getResults(String position) {
        String normalizedPosition = position.toUpperCase();
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("position").is(normalizedPosition)),
                Aggregation.group("candidateId").count().as("voteCount"),
                Aggregation.sort(Sort.Direction.DESC, "voteCount")
        );
        AggregationResults<Document> aggResults = mongoTemplate.aggregate(
                aggregation, "vote", Document.class
        );

        List<Candidate> candidates = candidateRepository.findAllByPosition(normalizedPosition);
        Map<String, String> candidateNames = candidates.stream()
                .collect(Collectors.toMap(Candidate::getId, Candidate::getFullName));

        List<ElectionResultResponse.CandidateResult> breakdown = new ArrayList<>();
        long totalVotes = 0;
        for (Document doc : aggResults.getMappedResults()) {
            String candidateId = doc.getString("_id");
            long count = ((Number) doc.get("voteCount")).longValue();
            totalVotes += count;
            ElectionResultResponse.CandidateResult result = new ElectionResultResponse.CandidateResult();
            result.setCandidateName(candidateNames.getOrDefault(candidateId, "Unknown"));
            result.setVoteCount(count);
            breakdown.add(result);
        }

        ElectionResultResponse response = new ElectionResultResponse();
        response.setTotalVotesCast((int) totalVotes);
        response.setBreakdown(breakdown);

        if (breakdown.isEmpty()) {
            response.setWinnerName("No votes cast yet");
            response.setTied(false);
        } else {
            long topCount = breakdown.get(0).getVoteCount();
            List<ElectionResultResponse.CandidateResult> winners = breakdown.stream()
                    .filter(r -> r.getVoteCount() == topCount)
                    .collect(Collectors.toList());
            boolean tied = winners.size() > 1;
            response.setTied(tied);
            response.setWinnerName(tied ? "TIE — " + winners.stream()
                    .map(ElectionResultResponse.CandidateResult::getCandidateName)
                    .collect(Collectors.joining(", ")) : winners.get(0).getCandidateName());
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
    public String logout(String email) {
        Optional<Voter> optionalVoter = voterRepository.findByEmail(email);
        if (optionalVoter.isEmpty())
            throw new InvalidLoginDetailsException("Voter with email " + email + " does not exist.");
        Voter voter = optionalVoter.get();
        if (!voter.isLoggedIn())
            throw new VoterNotLoggedInException(voter.getFirstName() + " is not currently logged in.");
        voter.setLoggedIn(false);
        voter.setToken(null);
        voterRepository.save(voter);
        return "Goodbye " + voter.getFirstName() + ". You have been logged out.";
    }

    @Override
    public List<CandidateResponse> getAllCandidates(String position) {
        return candidateRepository.findAllByPosition(position.toUpperCase())
                .stream()
                .map(Mapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public String startElection(String adminToken) {
        validateAdminToken(adminToken);
        Election election = electionRepository.findById(ELECTION_ID)
                .orElseThrow(() -> new ElectionException("No election has been created yet."));
        if (election.getStatus() == ElectionStatus.ENDED)
            throw new ElectionNotActiveException("Election has already ended and cannot be restarted.");
        election.setStatus(ElectionStatus.ONGOING);
        electionRepository.save(election);
        return "Election is now ONGOING.";
    }

    @Override
    public String endElection(String adminToken) {
        validateAdminToken(adminToken);
        Election election = electionRepository.findById(ELECTION_ID)
                .orElseThrow(() -> new ElectionException("No election has been created yet."));
        if (election.getStatus() != ElectionStatus.ONGOING)
            throw new ElectionNotActiveException("Election is not currently ongoing.");

        archiveAndClear(election);
        return "Election has ended. Results archived and data cleared.";
    }

    private void archiveAndClear(Election election) {
        String electionName = election.getName() != null ? election.getName() : "Election";

        for (String position : election.getPositions()) {
            List<Candidate> candidates = candidateRepository.findAllByPosition(position);
            Map<String, String> nameMap = candidates.stream()
                    .collect(Collectors.toMap(Candidate::getId, Candidate::getFullName));

            long totalVotes = voteRepository.countByPosition(position);

            StringBuilder details = new StringBuilder();
            details.append(electionName).append(" | Position: ").append(position)
                    .append(" | Total votes: ").append(totalVotes).append(" | Results: ");

            Aggregation agg = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("position").is(position)),
                    Aggregation.group("candidateId").count().as("count"),
                    Aggregation.sort(Sort.Direction.DESC, "count")
            );
            AggregationResults<Document> aggResults = mongoTemplate.aggregate(agg, "vote", Document.class);
            List<String> breakdown = new ArrayList<>();
            for (Document doc : aggResults.getMappedResults()) {
                String candidateId = doc.getString("_id");
                long count = ((Number) doc.get("count")).longValue();
                breakdown.add(nameMap.getOrDefault(candidateId, "Unknown") + " (" + count + " votes)");
            }
            details.append(breakdown.isEmpty() ? "No votes cast" : String.join(", ", breakdown));

            AuditLog log = new AuditLog();
            log.setVoterId("SYSTEM");
            log.setPosition(position);
            log.setOutcome("ELECTION_ARCHIVED");
            log.setDetails(details.toString());
            log.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(log);
        }

        mongoTemplate.remove(
                Query.query(Criteria.where("outcome").ne("ELECTION_ARCHIVED")),
                AuditLog.class
        );
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        mongoTemplate.updateMulti(new Query(), new Update().set("votedPositions", new HashSet<>()), Voter.class);
        electionRepository.deleteById(ELECTION_ID);
    }

    @Override
    public ElectionResponse getElection() {
        Election election = electionRepository.findById(ELECTION_ID)
                .orElseThrow(() -> new ElectionException("No election has been created yet."));
        ElectionResponse response = new ElectionResponse();
        response.setName(election.getName());
        response.setStatus(election.getStatus().name());
        response.setPositions(election.getPositions());
        return response;
    }

    @Override
    public List<VoterResponse> getVoters(int page, int size, String adminToken) {
        validateAdminToken(adminToken);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firstName"));
        return voterRepository.findAll(pageRequest).getContent()
                .stream()
                .map(Mapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public String removeCandidate(String id, String adminToken) {
        validateAdminToken(adminToken);
        Election election = electionRepository.findById(ELECTION_ID)
                .orElseThrow(() -> new ElectionException("No election has been created yet."));
        if (election.getStatus() != ElectionStatus.NOT_STARTED)
            throw new ElectionException("Candidates can only be removed before the election starts.");
        if (!candidateRepository.existsById(id))
            throw new CandidateNotFoundException("Candidate with ID " + id + " not found.");
        candidateRepository.deleteById(id);
        return "Candidate removed successfully.";
    }

    @Override
    public String getElectionStatus() {
        return electionRepository.findById(ELECTION_ID)
                .map(e -> e.getStatus().name())
                .orElse("NOT_CREATED");
    }

    @Override
    public List<String> getElectionPositions() {
        return electionRepository.findById(ELECTION_ID)
                .map(Election::getPositions)
                .orElse(List.of());
    }

    @Override
    public VoteReceiptResponse verifyVote(String receipt) {
        Optional<Vote> optionalVote = voteRepository.findByReceipt(receipt);
        if (optionalVote.isEmpty())
            throw new InvalidVoteException("No vote found for receipt: " + receipt);
        Vote vote = optionalVote.get();
        VoteReceiptResponse response = new VoteReceiptResponse();
        response.setPosition(vote.getPosition());
        response.setTimestamp(vote.getTimestamp());
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
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findAllByOutcome("ELECTION_ARCHIVED", pageRequest).getContent();
    }

    private ElectionStatus getCurrentElectionStatus() {
        return electionRepository.findById(ELECTION_ID)
                .map(Election::getStatus)
                .orElse(ElectionStatus.NOT_STARTED);
    }

    private void validateAdminToken(String token) {
        if (token == null || token.isBlank())
            throw new UnauthorizedException("Admin token is required.");
        adminRepository.findByToken(token)
                .filter(Admin::isLoggedIn)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired admin token. Please log in."));
    }

    private void saveAuditLog(String voterId, String position, String outcome, String details) {
        AuditLog log = new AuditLog();
        log.setVoterId(voterId);
        log.setPosition(position);
        log.setOutcome(outcome);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
