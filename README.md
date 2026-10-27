<div align="center">

<img src="https://capsule-render.vercel.app/api?type=venom&height=195&color=0:111827,50:334155,100:94a3b8&text=LogSentry&fontColor=ffffff&fontSize=40&fontAlignY=40&desc=Multithreaded%20Log%20Analyzer%20%7C%20Core%20Java&descAlignY=64&descSize=15" width="100%" alt="LogSentry banner"/>

![Core%20Java](https://img.shields.io/badge/Core%20Java-Concurrency-ED8B00?style=for-the-badge)
![Analysis](https://img.shields.io/badge/Analysis-Parallel%20Log%20Search-334155?style=for-the-badge)

[Repository](https://github.com/Chakaliprakash/LogSentry) · [Issues](https://github.com/Chakaliprakash/LogSentry/issues)

</div>

---

# LogSentry

LogSentry is a command-line log analysis tool written in Core Java. It uses a thread pool to search and summarize log files in parallel.

## Features

- Search log entries by keyword.
- Filter by log level such as `INFO`, `WARN`, `ERROR`, and `DEBUG`.
- Search by error code.
- Summarize log-level and error-code counts.
- Compare single-threaded and multithreaded processing.
- Generate sample log data for testing.

## Technology

- Java 17
- Maven
- `ExecutorService`, `Future`, and concurrent result aggregation

## Build

```bash
mvn package
```

## Run

Start the interactive console:

```bash
java -jar target/logsentry-1.0.0.jar
```

To run a benchmark against a log file:

```bash
java -jar target/logsentry-1.0.0.jar --benchmark sample_logs.log 4 ERROR
```

The benchmark arguments are the file path, thread count, and search query. Generate a sample file from the interactive menu if one is not present.

## Author

[Prakash Chakali](https://github.com/Chakaliprakash)
