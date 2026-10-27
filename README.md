<div align="center">

<img src="https://capsule-render.vercel.app/api?type=venom&height=195&color=0:111827,50:334155,100:94a3b8&text=LogSentry&fontColor=ffffff&fontSize=40&fontAlignY=40&desc=Multithreaded%20Log%20Analyzer%20%7C%20Core%20Java&descAlignY=64&descSize=15" width="100%" alt="LogSentry banner"/>

![Core%20Java](https://img.shields.io/badge/Core%20Java-Concurrency-ED8B00?style=for-the-badge)
![Analysis](https://img.shields.io/badge/Analysis-Parallel%20Log%20Search-334155?style=for-the-badge)

[Repository](https://github.com/Chakaliprakash/LogSentry) · [Issues](https://github.com/Chakaliprakash/LogSentry/issues)

</div>

---

# LogSentry 🛡️
> A multithreaded log analyzer and search engine built from scratch using Core Java.

I built this project to get hands-on experience with Java concurrency and multithreading (`ExecutorService`, `Callable`, `Future`, and thread-safe collections) rather than just reading theory for backend interviews. 

When server logs grow into hundreds of megabytes or gigabytes, searching through them line-by-line with a single thread is slow. LogSentry splits large log files into byte chunks and processes them in parallel across worker threads, safely aggregating counts and benchmarking single-threaded vs multithreaded performance.

---

## What It Does
- **Parallel Chunk Processing**: Divides a log file into `N` byte ranges and assigns each chunk to a thread in an `ExecutorService` thread pool.
- **Boundary-Safe Reading**: Handles line cuts at arbitrary byte offsets so no log line is missed or duplicated across thread boundaries.
- **Flexible Search**: Search across millions of lines by **keyword**, **log level** (`INFO`, `WARN`, `ERROR`, `DEBUG`), or **error codes** (`ERR_500`, `ERR_404`, `ERR_TIMEOUT`, etc.).
- **Thread-Safe Metrics**: Uses `ConcurrentHashMap` with atomic updates (`merge`) and `Callable`/`Future` aggregation to collect match counts and categories without race conditions.
- **Built-in Benchmark**: Compares 1-thread execution against `N` threads using `System.currentTimeMillis()` to measure speedup.
- **Resilient Parsing**: Gracefully handles malformed lines and Java stack traces without crashing worker threads.
- **Synthetic Log Generator**: Includes a utility to generate realistic test logs of configurable size (5MB, 50MB, 500MB, etc.).
- **Zero Dependencies**: Pure Core Java (Standard Library only).

---

## Project Structure

```
LogSentry/
├── pom.xml                               # Optional Maven POM (no external dependencies)
├── README.md                             # Project documentation
├── sample_logs.log                       # Pre-generated sample log file (~5.5MB)
└── src/
    └── com/
        └── logsentry/
            ├── LogEntry.java             # POJO holding timestamp, level, thread, and message
            ├── LogParser.java            # Parses raw lines and handles malformed lines
            ├── LogFileGenerator.java     # Generates realistic server logs for testing
            ├── SearchTask.java           # Callable worker that searches a specific byte chunk
            ├── SearchResult.java         # Holder for per-thread match counts and samples
            ├── LogAnalyzerEngine.java    # Manages ExecutorService, chunk splitting, and aggregation
            └── Main.java                 # Interactive console CLI and benchmark runner
```

---

## How to Run

### Option 1: Direct `javac` (No Build Tools Required)

1. **Compile all Java files:**
   ```bash
   javac -d bin src/com/logsentry/*.java
   ```

2. **Run the interactive menu:**
   ```bash
   java -cp bin com.logsentry.Main
   ```

3. **(Optional) Generate a larger log file (e.g. 50MB or 200,000 lines):**
   ```bash
   java -cp bin com.logsentry.LogFileGenerator test_logs.log 200000
   ```

4. **(Optional) Run a quick command-line benchmark:**
   ```bash
   java -cp bin com.logsentry.Main --benchmark sample_logs.log 4 ERROR
   ```

### Option 2: Using Maven (Optional)

```bash
mvn clean compile
mvn package
java -jar target/logsentry-1.0.0.jar
```

---

## Sample Console Output

### 1. Multithreaded Search (Searching for `ERROR` with 4 threads)

```text
Active File: sample_logs.log [Found (5.47 MB)]
-------------------------------------------------
Enter level to search: ERROR
Enter number of threads to use (default: 8, detected cores): 4

Searching with 4 worker threads...

================ SEARCH RESULTS ================
Threads Used         : 4
Lines Processed      : 60011
Matches Found        : 9967
Time Elapsed         : 120 ms
Processing Speed     : ~500091 lines/sec

--- Matches by Category / Code ---
  ERR_500            : 1604
  ERR_404            : 1653
  ERR_DB_CONN        : 1766
  ERR_TIMEOUT        : 1638
  ERR_OUT_OF_MEM     : 1731
  ERROR              : 9967
  ERR_AUTH_FAIL      : 1575

--- Sample Matching Entries (First 15) ---
 [1] [2026-09-16 05:17:43] [ERROR] [http-nio-8080-exec-3] ERR_OUT_OF_MEM: Garbage collection overhead limit exceeded
 [2] [2026-09-16 05:17:51] [ERROR] [scheduler-task] ERR_OUT_OF_MEM: Garbage collection overhead limit exceeded
 [3] [2026-09-16 05:18:00] [ERROR] [worker-thread-1] ERR_DB_CONN: Could not acquire connection from Hikari pool
 [4] [2026-09-16 05:18:06] [ERROR] [worker-thread-1] ERR_404: Resource not found for requested endpoint
 [5] [2026-09-16 05:18:07] [ERROR] [scheduler-task] ERR_DB_CONN: Could not acquire connection from Hikari pool
================================================
```

### 2. Concurrency Benchmark (1 Thread vs 4 Threads)

```text
------------------------------------------------
Starting Benchmark Comparison on: sample_logs.log
File Size: 5.47 MB
Search Query: 'ERROR' (LEVEL)
------------------------------------------------
Running with 1 thread (Sequential baseline)...
-> 1 Thread took: 142 ms

Running with 4 threads (Parallel)...
-> 4 Threads took: 42 ms

=============== BENCHMARK SUMMARY ===============
Lines processed (1 Thread)  : 60011
Lines processed (4 Threads) : 60011
1 Thread Duration           : 142 ms
4 Threads Duration          : 42 ms
Speedup Factor              : 3.38x faster
=================================================
```

---

## What I Learned Building This

1. **How File Chunking Actually Works with Threads**:
   Splitting a file into byte offsets (`startByte` to `endByte`) is tricky because byte boundaries rarely fall neatly between newlines. If a thread starts in the middle of a line, it has to discard that partial line, while the previous thread must read across its boundary to complete it. Getting this right so that `1 Thread` and `4 Threads` process the exact same number of lines (60,011 in my tests) was one of the most rewarding parts to debug.

2. **Why `ExecutorService` and `Callable` beat raw `Thread`**:
   At first I thought about creating `new Thread()` manually, but managing thread lifecycles, joining, and getting return values would require messy shared state. `ExecutorService.invokeAll()` with `Callable<SearchResult>` made it clean to dispatch tasks and wait for structured `Future` results without boilerplate.

3. **Atomic Operations & `ConcurrentHashMap`**:
   When multiple threads find error codes (like `ERR_500`), incrementing a standard `HashMap` would cause race conditions or corrupt internal bucket pointers. Using `ConcurrentHashMap` with `.merge(code, 1, Integer::sum)` gives lock-free, atomic counter updates across worker threads.

4. **I/O Buffering Bottlenecks**:
   In my first attempt, I used standard `RandomAccessFile.readLine()`. It was surprisingly slow because standard `readLine()` reads byte-by-byte via native system calls. Implementing a 64KB internal buffer reduced execution time on 60,000 lines from over 8 seconds down to ~40 milliseconds!

