package com.logsentry;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    private static String currentFilePath = "sample_logs.log";
    private static final LogAnalyzerEngine engine = new LogAnalyzerEngine();

    public static void main(String[] args) {
        // quick headless benchmark mode if CLI arguments are provided
        if (args.length >= 1 && "--benchmark".equalsIgnoreCase(args[0])) {
            String file = args.length >= 2 ? args[1] : currentFilePath;
            int threads = args.length >= 3 ? Integer.parseInt(args[2]) : 4;
            String query = args.length >= 4 ? args[3] : "ERROR";
            String type = "KEYWORD";
            if (query.equalsIgnoreCase("INFO") || query.equalsIgnoreCase("WARN") 
                || query.equalsIgnoreCase("ERROR") || query.equalsIgnoreCase("DEBUG")) {
                type = "LEVEL";
            } else if (query.toUpperCase().startsWith("ERR_")) {
                type = "ERROR_CODE";
            }
            engine.runBenchmark(new File(file), query, type, threads);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=================================================");
        System.out.println("  LogSentry - Multithreaded Log Analyzer Engine  ");
        System.out.println("=================================================");

        while (running) {
            File f = new File(currentFilePath);
            String status = f.exists() ? String.format("Found (%.2f MB)", f.length() / (1024.0 * 1024.0)) : "Not Found";

            System.out.println("\nActive File: " + currentFilePath + " [" + status + "]");
            System.out.println("-------------------------------------------------");
            System.out.println("1. Search by Keyword (case-insensitive)");
            System.out.println("2. Filter by Log Level (INFO, WARN, ERROR, DEBUG)");
            System.out.println("3. Search by Error Code (ERR_500, ERR_404, etc.)");
            System.out.println("4. Analyze Full File & Summarize Distribution");
            System.out.println("5. Run Thread Performance Benchmark (1 vs N Threads)");
            System.out.println("6. Generate / Recreate Sample Log File");
            System.out.println("7. Change Target Log File Path");
            System.out.println("8. Exit");
            System.out.print("Enter your choice (1-8): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleSearch(scanner, "KEYWORD");
                    break;
                case "2":
                    handleSearch(scanner, "LEVEL");
                    break;
                case "3":
                    handleSearch(scanner, "ERROR_CODE");
                    break;
                case "4":
                    handleFullScan(scanner);
                    break;
                case "5":
                    handleBenchmark(scanner);
                    break;
                case "6":
                    handleFileGeneration(scanner);
                    break;
                case "7":
                    System.out.print("Enter new log file path: ");
                    String newPath = scanner.nextLine().trim();
                    if (!newPath.isEmpty()) {
                        currentFilePath = newPath;
                        System.out.println("Active file updated to: " + currentFilePath);
                    }
                    break;
                case "8":
                    System.out.println("Exiting LogSentry. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please enter a number between 1 and 8.");
                    break;
            }
        }
        scanner.close();
    }

    private static void handleSearch(Scanner scanner, String searchType) {
        File file = new File(currentFilePath);
        if (!file.exists()) {
            System.out.println("Log file not found! Use Option 6 to generate a sample file first.");
            return;
        }

        System.out.print("Enter " + searchType.toLowerCase() + " to search: ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            System.out.println("Search term cannot be empty.");
            return;
        }

        int threads = promptThreadCount(scanner);
        System.out.println("\nSearching with " + threads + " worker threads...");
        LogAnalyzerEngine.AnalysisReport report = engine.analyze(file, query, searchType, threads);
        report.printSummary();
    }

    private static void handleFullScan(Scanner scanner) {
        File file = new File(currentFilePath);
        if (!file.exists()) {
            System.out.println("Log file not found! Use Option 6 to generate a sample file first.");
            return;
        }

        int threads = promptThreadCount(scanner);
        System.out.println("\nScanning entire file with " + threads + " worker threads...");
        LogAnalyzerEngine.AnalysisReport report = engine.analyze(file, "", "ALL", threads);
        report.printSummary();
    }

    private static void handleBenchmark(Scanner scanner) {
        File file = new File(currentFilePath);
        if (!file.exists()) {
            System.out.println("Log file not found! Use Option 6 to generate a sample file first.");
            return;
        }

        System.out.print("Enter search term for benchmark (default: ERROR): ");
        String query = scanner.nextLine().trim();
        if (query.isEmpty()) {
            query = "ERROR";
        }

        int threads = promptThreadCount(scanner);
        engine.runBenchmark(file, query, "KEYWORD", threads);
    }

    private static void handleFileGeneration(Scanner scanner) {
        System.out.print("Enter target file name (default: " + currentFilePath + "): ");
        String filename = scanner.nextLine().trim();
        if (filename.isEmpty()) {
            filename = currentFilePath;
        }

        System.out.print("Generate by (1) Line count or (2) File size in MB? [1/2]: ");
        String mode = scanner.nextLine().trim();

        try {
            if ("2".equals(mode)) {
                System.out.print("Enter target size in MB (e.g. 10, 50, 200): ");
                long mb = Long.parseLong(scanner.nextLine().trim());
                System.out.println("Generating ~" + mb + " MB log file...");
                long start = System.currentTimeMillis();
                LogFileGenerator.generateFileBySize(filename, mb);
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("File created successfully in " + elapsed + " ms!");
            } else {
                System.out.print("Enter number of lines (default: 50000 ~5MB): ");
                String input = scanner.nextLine().trim();
                int lines = input.isEmpty() ? 50000 : Integer.parseInt(input);
                System.out.println("Generating " + lines + " lines...");
                long start = System.currentTimeMillis();
                LogFileGenerator.generateFileByLines(filename, lines);
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("File created successfully in " + elapsed + " ms!");
            }
            currentFilePath = filename;
        } catch (Exception e) {
            System.err.println("Error generating file: " + e.getMessage());
        }
    }

    private static int promptThreadCount(Scanner scanner) {
        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.print("Enter number of threads to use (default: " + availableCores + ", detected cores): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return availableCores;
        }
        try {
            int t = Integer.parseInt(input);
            return (t > 0) ? t : availableCores;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input, defaulting to " + availableCores + " threads.");
            return availableCores;
        }
    }
}

