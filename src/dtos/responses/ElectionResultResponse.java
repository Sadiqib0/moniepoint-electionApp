package dtos.responses;

import data.models.Position;
import lombok.Data;

import java.util.List;

@Data
public class ElectionResultResponse {
    private Position position;
    private int totalVotesCast;
    private String winnerName;
    private long winnerVoteCount;
    private boolean tied;
    private List<CandidateResult> winners;
    private List<CandidateResult> breakdown;

    @Data
    public static class CandidateResult {
        private String candidateId;
        private String candidateName;
        private long voteCount;
    }
}
