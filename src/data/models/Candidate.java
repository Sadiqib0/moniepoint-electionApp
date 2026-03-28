package data.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@CompoundIndex(name = "voter_position_unique", def = "{'voterId': 1, 'position': 1}", unique = true)
@Data
@Document
public class Candidate {
    @Id
    private String id;
    private String voterId;
    private Position position;
    private String fullName;
    private LocalDateTime nominatedAt;
}
