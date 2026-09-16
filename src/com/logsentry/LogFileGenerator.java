package com.logsentry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class LogFileGenerator {

    private static final String[] LEVELS = {"INFO", "INFO", "INFO", "DEBUG", "WARN", "ERROR"};
    private static final String[] THREADS = {
        "main", "http-nio-8080-exec-1", "http-nio-8080-exec-2", 
        "http-nio-8080-exec-3", "worker-thread-1", "worker-thread-2", "scheduler-task"
    };

    private static final String[] INFO_MSGS = {
        "User authenticated successfully: user_",
        "HTTP GET /api/v1/products status=200 duration=",
        "Batch processing completed for job id=",
        "Cache hit for key: user_profile_",
        "Order placed successfully. OrderID: ORD-",
        "Database connection acquired from pool"
    };

    private static final String[] WARN_MSGS = {
        "High memory consumption detected: heap at 82%",
        "Slow query execution on table 'orders' exceeding threshold",
        "Rate limit threshold reached for client IP",
        "Retry attempt 2/3 for external payment gateway",
        "Deprecated API endpoint accessed: /api/v0/checkout"
    };

    private static final String[] ERROR_MSGS = {
        "ERR_500: Internal server error while processing checkout",
        "ERR_404: Resource not found for requested endpoint",
        "ERR_TIMEOUT: Database connection timed out after 5000ms",
        "ERR_AUTH_FAIL: Invalid JWT token signature",
        "ERR_DB_CONN: Could not acquire connection from Hikari pool",
        "ERR_OUT_OF_MEM: Garbage collection overhead limit exceeded"
    };

    private static final String[] DEBUG_MSGS = {
        "Executing query: SELECT * FROM users WHERE active = 1",
        "Parsing JSON payload from incoming request",
        "Cache miss, fetching data from repository",
        "Dispatching event to registered event listeners"
    };

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void generateFileByLines(String filePath, int totalLines) throws IOException {
        File file = new File(filePath);
        Random rand = new Random();
        LocalDateTime time = LocalDateTime.now().minusHours(5);

        // TODO: optimize buffer size if time permits (64KB works fine for now)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file), 64 * 1024)) {
            for (int i = 0; i < totalLines; i++) {
                time = time.plusSeconds(rand.nextInt(3) + 1);
                String level = LEVELS[rand.nextInt(LEVELS.length)];
                String thread = THREADS[rand.nextInt(THREADS.length)];
                String msg;

                switch (level) {
                    case "INFO":
                        msg = INFO_MSGS[rand.nextInt(INFO_MSGS.length)] + rand.nextInt(5000);
                        break;
                    case "WARN":
                        msg = WARN_MSGS[rand.nextInt(WARN_MSGS.length)];
                        break;
                    case "ERROR":
                        msg = ERROR_MSGS[rand.nextInt(ERROR_MSGS.length)];
                        break;
                    default:
                        msg = DEBUG_MSGS[rand.nextInt(DEBUG_MSGS.length)];
                        break;
                }

                writer.write("[" + time.format(FORMATTER) + "] [" + level + "] [" + thread + "] " + msg);
                writer.newLine();

                // inject an occasional malformed line to test parser resilience
                if (i % 5000 == 0 && i > 0) {
                    writer.write("    at com.logsentry.service.PaymentService.process(PaymentService.java:142)");
                    writer.newLine();
                }
            }
        }
    }

    public static void generateFileBySize(String filePath, long targetSizeMB) throws IOException {
        File file = new File(filePath);
        Random rand = new Random();
        LocalDateTime time = LocalDateTime.now().minusHours(12);
        long targetBytes = targetSizeMB * 1024 * 1024;
        long writtenBytes = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file), 128 * 1024)) {
            int lineIndex = 0;
            while (writtenBytes < targetBytes) {
                time = time.plusSeconds(rand.nextInt(2) + 1);
                String level = LEVELS[rand.nextInt(LEVELS.length)];
                String thread = THREADS[rand.nextInt(THREADS.length)];
                String msg;

                switch (level) {
                    case "INFO":
                        msg = INFO_MSGS[rand.nextInt(INFO_MSGS.length)] + rand.nextInt(10000);
                        break;
                    case "WARN":
                        msg = WARN_MSGS[rand.nextInt(WARN_MSGS.length)];
                        break;
                    case "ERROR":
                        msg = ERROR_MSGS[rand.nextInt(ERROR_MSGS.length)];
                        break;
                    default:
                        msg = DEBUG_MSGS[rand.nextInt(DEBUG_MSGS.length)];
                        break;
                }

                String line = "[" + time.format(FORMATTER) + "] [" + level + "] [" + thread + "] " + msg;
                writer.write(line);
                writer.newLine();
                writtenBytes += line.length() + 2; // rough estimate with newline
                lineIndex++;

                if (lineIndex % 4000 == 0) {
                    writer.write("    at com.logsentry.service.OrderService.execute(OrderService.java:88)");
                    writer.newLine();
                    writtenBytes += 72;
                }
            }
        }
    }

    public static void main(String[] args) {
        String path = "sample_logs.log";
        int lines = 50000; // default ~5MB

        if (args.length >= 1) {
            path = args[0];
        }
        if (args.length >= 2) {
            try {
                lines = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid line count, using default 50000.");
            }
        }

        System.out.println("Generating log file: " + path + " with " + lines + " lines...");
        long start = System.currentTimeMillis();
        try {
            generateFileByLines(path, lines);
            long time = System.currentTimeMillis() - start;
            File f = new File(path);
            System.out.println("Done! File size: " + (f.length() / 1024) + " KB in " + time + " ms.");
        } catch (IOException e) {
            System.err.println("Failed to generate file: " + e.getMessage());
        }
    }
}

