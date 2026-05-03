package data.repositories;

import data.models.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VoteRepository extends MongoRepository<Vote, String> {
    boolean existsByVoterIdAndPosition(String voterId, String position);
    Optional<Vote> findByReceipt(String receipt);
    long countByPosition(String position);
}
