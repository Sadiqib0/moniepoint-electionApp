package data.models;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document
@CompoundIndex(name = "one_vote_per_position", def = "{'voterId': 1, 'position': 1}", unique = true)
public class Vote {
    @Id
    private String id;
    private String voterId;
    private String candidateId;
    private String position;
    @Indexed(unique = true)
    private String receipt;
    @CreatedDate
    private LocalDateTime timestamp = LocalDateTime.now();
}
