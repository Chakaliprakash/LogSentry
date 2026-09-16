package com.logsentry;

public class LogEntry {
    private String timestamp;
    private String level;
    private String threadName;
    private String message;
    private String rawLine;

    public LogEntry(String timestamp, String level, String threadName, String message, String rawLine) {
        this.timestamp = timestamp;
        this.level = level;
        this.threadName = threadName;
        this.message = message;
        this.rawLine = rawLine;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getLevel() {
        return level;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }

    public String getRawLine() {
        return rawLine;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] [" + level + "] [" + threadName + "] " + message;
    }
}

