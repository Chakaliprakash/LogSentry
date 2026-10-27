<div align="center">

<img src="https://capsule-render.vercel.app/api?type=venom&height=195&color=0:111827,50:334155,100:94a3b8&text=LogSentry&fontColor=ffffff&fontSize=40&fontAlignY=40&desc=Multithreaded%20Log%20Analyzer%20%7C%20Core%20Java&descAlignY=64&descSize=15" width="100%" alt="LogSentry banner"/>

![Core%20Java](https://img.shields.io/badge/Core%20Java-Concurrency-ED8B00?style=for-the-badge)
![Analysis](https://img.shields.io/badge/Analysis-Parallel%20Log%20Search-334155?style=for-the-badge)

[Repository](https://github.com/Chakaliprakash/LogSentry) · [Issues](https://github.com/Chakaliprakash/LogSentry/issues)

</div>

---

# LogSentry

LogSentry is a Core Java log analyzer that uses multithreading to process large log files in parallel.

## Key Features

- Splits log files into chunks for parallel processing.
- Uses Java concurrency utilities to manage worker tasks.
- Handles chunk boundaries when reading file data.
- Compares single-threaded and multithreaded processing.

## Concepts Demonstrated

- `ExecutorService`
- `Callable` and `Future`
- Thread-safe result aggregation
- File processing and performance measurement

## Getting Started

Review the source files for the entry point and supported command-line arguments. Use a JDK compatible with the project configuration.

## Author

[Prakash Chakali](https://github.com/Chakaliprakash)
