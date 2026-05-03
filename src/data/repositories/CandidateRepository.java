package data.repositories;

import data.models.Candidate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CandidateRepository extends MongoRepository<Candidate, String> {
    boolean existsByVoterIdAndPosition(String voterId, String position);
    boolean existsByFullNameIgnoreCaseAndPosition(String fullName, String position);
    List<Candidate> findAllByPosition(String position);
}
