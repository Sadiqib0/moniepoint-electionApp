package data.repositories;

import data.models.Position;
import data.models.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VoteRepository extends MongoRepository<Vote, String>{
    boolean existsByVoterIdAndPosition(String voterId, Position position);
    List<Vote> findAllByPosition(Position position);
}
