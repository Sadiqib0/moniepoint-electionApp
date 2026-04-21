package dtos.responses;

import lombok.Data;

import java.util.Map;

@Data
public class StatsResponse {
    private long totalVoters;
    private long totalVoted;
    private double turnoutPercentage;
    private Map<String, Long> votesByPosition;
}
