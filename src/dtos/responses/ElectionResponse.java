package dtos.responses;

import lombok.Data;
import java.util.List;

@Data
public class ElectionResponse {
    private String name;
    private String status;
    private List<String> positions;
}
