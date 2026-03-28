package data.repositories;

import data.models.Candidate;
import data.models.Position;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends MongoRepository<Candidate, String>{
    boolean existsByVoterIdAndPosition(String voterId, Position position);
    List<Candidate> findAllByPosition(Position position);
    Optional<Candidate> findByIdAndPosition(String id, Position position);
}

