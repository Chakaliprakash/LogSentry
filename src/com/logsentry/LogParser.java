package com.logsentry;

public class LogParser {

    // parses format: [TIMESTAMP] [LEVEL] [THREAD] Message
    public static LogEntry parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        try {
            int t1 = line.indexOf('[');
            int t2 = line.indexOf(']', t1);
            if (t1 == -1 || t2 == -1) {
                // malformed or stacktrace line, wrap it as raw entry
                return new LogEntry("N/A", "UNKNOWN", "N/A", line.trim(), line);
            }

            int l1 = line.indexOf('[', t2 + 1);
            int l2 = line.indexOf(']', l1);
            if (l1 == -1 || l2 == -1) {
                return new LogEntry("N/A", "UNKNOWN", "N/A", line.trim(), line);
            }

            int th1 = line.indexOf('[', l2 + 1);
            int th2 = line.indexOf(']', th1);
            if (th1 == -1 || th2 == -1) {
                return new LogEntry("N/A", "UNKNOWN", "N/A", line.trim(), line);
            }

            String timestamp = line.substring(t1 + 1, t2).trim();
            String level = line.substring(l1 + 1, l2).trim().toUpperCase();
            String threadName = line.substring(th1 + 1, th2).trim();
            String message = (th2 + 1 < line.length()) ? line.substring(th2 + 1).trim() : "";

            return new LogEntry(timestamp, level, threadName, message, line);
        } catch (Exception e) {
            // fallback in case of unexpected line format
            return new LogEntry("N/A", "UNKNOWN", "N/A", line, line);
        }
    }
}

