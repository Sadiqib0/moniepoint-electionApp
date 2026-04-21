package data.repositories;

import data.models.Position;
import data.models.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VoteRepository extends MongoRepository<Vote, String>{
    boolean existsByVoterIdAndPosition(String voterId, Position position);
    Optional<Vote> findByReceipt(String receipt);
}
