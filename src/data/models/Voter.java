package data.models;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Document
public class Voter {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    @Indexed (unique=true)
    private String email;
    @Indexed (unique=true)
    private String matricNumber;
    private Set<Position> votedPositions = new HashSet<>();
    @CreatedDate
    private LocalDateTime registeredAt;

}
