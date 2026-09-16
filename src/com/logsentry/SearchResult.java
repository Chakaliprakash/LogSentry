package com.logsentry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResult {
    private int linesProcessed;
    private int matchCount;
    private List<LogEntry> sampleMatches;
    private Map<String, Integer> categoryCounts;
    private long executionTimeMs;

    public SearchResult() {
        this.linesProcessed = 0;
        this.matchCount = 0;
        this.sampleMatches = new ArrayList<>();
        this.categoryCounts = new HashMap<>();
        this.executionTimeMs = 0;
    }

    public int getLinesProcessed() {
        return linesProcessed;
    }

    public void setLinesProcessed(int linesProcessed) {
        this.linesProcessed = linesProcessed;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    public List<LogEntry> getSampleMatches() {
        return sampleMatches;
    }

    public void addSampleMatch(LogEntry entry) {
        // limit sample matches to avoid filling up memory
        if (sampleMatches.size() < 25) {
            sampleMatches.add(entry);
        }
    }

    public Map<String, Integer> getCategoryCounts() {
        return categoryCounts;
    }

    public void incrementCategory(String category) {
        categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}

