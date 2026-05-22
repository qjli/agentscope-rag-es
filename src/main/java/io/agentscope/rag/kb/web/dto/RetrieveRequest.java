package io.agentscope.rag.kb.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RetrieveRequest {

    @NotBlank
    @Size(max = 4000)
    private String query;

    /**
     * Top-K。ElasticsearchStore 内部 numCandidates = max(limit*2, 50)，ES 要求 numCandidates ≤ 10000。
     */
    @Min(1)
    @Max(100)
    private Integer limit;

    private Double scoreThreshold;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }
}
