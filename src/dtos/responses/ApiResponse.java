package dtos.responses;
import lombok.Data;

@Data
public class ApiResponse {
        private boolean status;
        private Object data;

        public ApiResponse(boolean status, Object data) {
            this.status = status;
            this.data = data;
        }
    }
