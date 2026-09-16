package com.logsentry;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class SearchTask implements Callable<SearchResult> {

    private final File file;
    private final long startByte;
    private final long endByte;
    private final String query;
    private final String searchType; // "KEYWORD", "LEVEL", "ERROR_CODE", "ALL"
    private final ConcurrentHashMap<String, Integer> globalCategoryCounts;

    public SearchTask(File file, long startByte, long endByte, String query, String searchType, 
                      ConcurrentHashMap<String, Integer> globalCategoryCounts) {
        this.file = file;
        this.startByte = startByte;
        this.endByte = endByte;
        this.query = query;
        this.searchType = searchType;
        this.globalCategoryCounts = globalCategoryCounts;
    }

    @Override
    public SearchResult call() throws Exception {
        SearchResult result = new SearchResult();
        long startTime = System.currentTimeMillis();

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            boolean skipFirst = false;

            // if not the first chunk, check if startByte landed in the middle of a line.
            // if previous character was not a newline, we must skip the rest of this line
            // because the previous worker thread will read across the boundary and finish it.
            if (startByte > 0) {
                raf.seek(startByte - 1);
                int prev = raf.read();
                if (prev != '\n') {
                    skipFirst = true;
                }
            }

            FastChunkReader reader = new FastChunkReader(raf, startByte);
            if (skipFirst) {
                reader.readLine(); // discard rest of partial line
            }

            String line;
            int linesRead = 0;

            // read lines until chunk boundary is reached
            while (reader.getOffset() < endByte && (line = reader.readLine()) != null) {
                linesRead++;
                processLine(line, result);
            }

            result.setLinesProcessed(linesRead);
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private void processLine(String line, SearchResult result) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }

        LogEntry entry = LogParser.parse(line);
        boolean matches = false;

        if ("LEVEL".equalsIgnoreCase(searchType)) {
            matches = entry != null && entry.getLevel().equalsIgnoreCase(query);
        } else if ("ERROR_CODE".equalsIgnoreCase(searchType)) {
            matches = line.contains(query);
        } else if ("ALL".equalsIgnoreCase(searchType)) {
            matches = true;
        } else {
            // KEYWORD search (case-insensitive)
            matches = line.toLowerCase().contains(query.toLowerCase());
        }

        if (matches) {
            result.setMatchCount(result.getMatchCount() + 1);
            if (entry != null) {
                result.addSampleMatch(entry);

                // group matches by log level
                String level = entry.getLevel();
                result.incrementCategory(level);
                if (globalCategoryCounts != null) {
                    globalCategoryCounts.merge(level, 1, Integer::sum);
                }

                // also tally error codes if present
                if (line.contains("ERR_")) {
                    tallyErrorCode(line, result);
                }
            }
        }
    }

    private void tallyErrorCode(String line, SearchResult result) {
        int idx = line.indexOf("ERR_");
        if (idx != -1) {
            int end = line.indexOf(':', idx);
            if (end == -1) {
                end = line.indexOf(' ', idx);
            }
            if (end == -1) {
                end = line.length();
            }
            String code = line.substring(idx, end).trim();
            result.incrementCategory(code);
            if (globalCategoryCounts != null) {
                globalCategoryCounts.merge(code, 1, Integer::sum);
            }
        }
    }

    // simple 64KB buffered reader over RandomAccessFile.
    // avoids the massive overhead of standard raf.readLine() which reads byte-by-byte via native JNI.
    private static class FastChunkReader {
        private final RandomAccessFile raf;
        private final byte[] buffer = new byte[64 * 1024];
        private int pos = 0;
        private int count = 0;
        private long fileOffset;

        public FastChunkReader(RandomAccessFile raf, long start) throws IOException {
            this.raf = raf;
            this.fileOffset = start;
            raf.seek(start);
        }

        public long getOffset() {
            return fileOffset - (count - pos);
        }

        public String readLine() throws IOException {
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= count) {
                    count = raf.read(buffer);
                    pos = 0;
                    if (count == -1) {
                        return sb.length() > 0 ? sb.toString() : null;
                    }
                    fileOffset += count;
                }

                byte b = buffer[pos++];
                if (b == '\n') {
                    break;
                } else if (b == '\r') {
                    // ignore carriage return
                    continue;
                } else {
                    sb.append((char) b);
                }
            }
            return sb.toString();
        }
    }
}

