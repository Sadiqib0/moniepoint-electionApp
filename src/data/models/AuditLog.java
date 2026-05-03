package data.models;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document
public class AuditLog {
    @Id
    private String id;
    private String voterId;
    private String position;
    private String outcome;
    private String details;
    @CreatedDate
    private LocalDateTime timestamp = LocalDateTime.now();
}
