package dtos.responses;

import lombok.Data;

import java.util.List;

@Data
public class ElectionResultResponse {
    private int totalVotesCast;
    private String winnerName;
    private boolean tied;
    private List<CandidateResult> breakdown;

    @Data
    public static class CandidateResult {
        private String candidateName;
        private long voteCount;
    }
}
