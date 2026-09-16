package com.logsentry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LogAnalyzerEngine {

    // helper class to hold aggregated report data
    public static class AnalysisReport {
        private int totalLines;
        private int totalMatches;
        private long durationMs;
        private int threadsUsed;
        private Map<String, Integer> categoryCounts;
        private List<LogEntry> sampleMatches;

        public AnalysisReport(int totalLines, int totalMatches, long durationMs, int threadsUsed, 
                              Map<String, Integer> categoryCounts, List<LogEntry> sampleMatches) {
            this.totalLines = totalLines;
            this.totalMatches = totalMatches;
            this.durationMs = durationMs;
            this.threadsUsed = threadsUsed;
            this.categoryCounts = categoryCounts;
            this.sampleMatches = sampleMatches;
        }

        public int getTotalLines() { return totalLines; }
        public int getTotalMatches() { return totalMatches; }
        public long getDurationMs() { return durationMs; }
        public int getThreadsUsed() { return threadsUsed; }
        public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
        public List<LogEntry> getSampleMatches() { return sampleMatches; }

        public void printSummary() {
            System.out.println("\n================ SEARCH RESULTS ================");
            System.out.println("Threads Used         : " + threadsUsed);
            System.out.println("Lines Processed      : " + totalLines);
            System.out.println("Matches Found        : " + totalMatches);
            System.out.println("Time Elapsed         : " + durationMs + " ms");
            if (durationMs > 0) {
                long throughput = (totalLines * 1000L) / durationMs;
                System.out.println("Processing Speed     : ~" + throughput + " lines/sec");
            }

            if (!categoryCounts.isEmpty()) {
                System.out.println("\n--- Matches by Category / Code ---");
                for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                    System.out.printf("  %-18s : %d%n", entry.getKey(), entry.getValue());
                }
            }

            if (!sampleMatches.isEmpty()) {
                System.out.println("\n--- Sample Matching Entries (First " + sampleMatches.size() + ") ---");
                for (int i = 0; i < sampleMatches.size(); i++) {
                    System.out.println(" [" + (i + 1) + "] " + sampleMatches.get(i).getRawLine());
                }
            } else {
                System.out.println("\nNo matching log lines found.");
            }
            System.out.println("================================================\n");
        }
    }

    public AnalysisReport analyze(File file, String query, String searchType, int numThreads) {
        if (!file.exists() || file.length() == 0) {
            System.out.println("Error: File does not exist or is empty.");
            return new AnalysisReport(0, 0, 0, numThreads, new ConcurrentHashMap<>(), new ArrayList<>());
        }

        // fallback if user passes invalid thread count
        if (numThreads <= 0) {
            numThreads = 4; // default hardcoded thread count
        }

        long fileLength = file.length();
        // TODO: dynamically adjust chunk size based on available memory if needed
        long chunkSize = fileLength / numThreads;

        ConcurrentHashMap<String, Integer> globalCounts = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<SearchTask> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            long start = i * chunkSize;
            long end = (i == numThreads - 1) ? fileLength : (i + 1) * chunkSize;
            tasks.add(new SearchTask(file, start, end, query, searchType, globalCounts));
        }

        long startTime = System.currentTimeMillis();
        int totalLines = 0;
        int totalMatches = 0;
        List<LogEntry> sampleMatches = new ArrayList<>();

        try {
            // submit all tasks and wait for them to finish
            List<Future<SearchResult>> futures = executor.invokeAll(tasks);

            for (Future<SearchResult> future : futures) {
                SearchResult r = future.get(); // blocks until thread completes
                totalLines += r.getLinesProcessed();
                totalMatches += r.getMatchCount();

                for (LogEntry entry : r.getSampleMatches()) {
                    if (sampleMatches.size() < 15) {
                        sampleMatches.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error during parallel log search: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        long duration = System.currentTimeMillis() - startTime;
        return new AnalysisReport(totalLines, totalMatches, duration, numThreads, globalCounts, sampleMatches);
    }

    public void runBenchmark(File file, String query, String searchType, int numThreads) {
        System.out.println("\n------------------------------------------------");
        System.out.println("Starting Benchmark Comparison on: " + file.getName());
        System.out.printf("File Size: %.2f MB%n", file.length() / (1024.0 * 1024.0));
        System.out.println("Search Query: '" + query + "' (" + searchType + ")");
        System.out.println("------------------------------------------------");

        // 1. Single-threaded run
        System.out.println("Running with 1 thread (Sequential baseline)...");
        AnalysisReport singleReport = analyze(file, query, searchType, 1);
        System.out.println("-> 1 Thread took: " + singleReport.getDurationMs() + " ms");

        // 2. Multi-threaded run
        System.out.println("\nRunning with " + numThreads + " threads (Parallel)...");
        AnalysisReport multiReport = analyze(file, query, searchType, numThreads);
        System.out.println("-> " + numThreads + " Threads took: " + multiReport.getDurationMs() + " ms");

        // Print benchmark comparison
        System.out.println("\n=============== BENCHMARK SUMMARY ===============");
        System.out.printf("Lines processed (1 Thread)  : %d%n", singleReport.getTotalLines());
        System.out.printf("Lines processed (%d Threads) : %d%n", numThreads, multiReport.getTotalLines());
        System.out.printf("1 Thread Duration           : %d ms%n", singleReport.getDurationMs());
        System.out.printf("%d Threads Duration          : %d ms%n", numThreads, multiReport.getDurationMs());

        if (multiReport.getDurationMs() > 0) {
            double speedup = (double) singleReport.getDurationMs() / multiReport.getDurationMs();
            System.out.printf("Speedup Factor              : %.2fx faster%n", speedup);
        }
        System.out.println("=================================================\n");
    }
}

