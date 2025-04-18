# D-XOR: XOR-like Transcode in Decimal for Online Lossless Time Series Compression

---

## Project Structure

This project, constructed via Maven, encompasses a variety of compression algorithms 
housed within the `src/main/java/algorithms` directory. 

These algorithms inherit from a common parent class, `Algorithm`,
and are managed through `AlgorithmManager` and `AlgorithmEnums`.

Each algorithm features corresponding `Encoder` and `Decoder` classes for the supported data types (INT32, INT64, Float, Double), which facilitate compression and decompression, 
respectively. They are governed by the `Algorithm` class.

## How to start 

After obtaining the repository via Git, the project can be constructed using Maven:

`mvn clean install`

Upon completion of the build process, the resultant JAR file, which includes all dependencies, is located in the `target` directory.

`target/OL-TSC-1.0-jar-with-dependencies.jar`

Ensure that the runtime environment has `Java 8` installed, and the compression process can then commence.

## Parameter Introduction

Run this package with `java -jar` command, such as:

`java - jar target/OL-TSC-1.0-jar-with-dependencies.jar -in [INPUT_PATH] -out [OUTPUT_PATH] -log [LOG_PATH] -m [METHOD]`

You can identify this compression by setting these options:

`-in [INPUT_PATH]` The source of the files to be compressed, currently supporting files in CSV format with the structure of `<timestamp, value>` for compression.
Default is `./datasets/Overall`.

`-out [OUTPUT_PATH]` Where the compressed binary files stored.
Default is `./storage`.

`-log [LOG_PATH]` The results of experiment benchmarks will be located in `LOG_PATH`
Default is `./results`.

`-m [METHOD]` The `[METHOD]` parameter specifies the name of the compression algorithm to be used. Currently supported algorithms include `[Gorilla, Chimp, Chimp128, Elf, ElfPlus, Camel, DXOR]`. 
The algorithm names are case-insensitive.
Default is `DXOR`.

## Notified

The algorithms `[Elf, ElfPlus, Camel]` support only floating-point data types.

The application scenario of the **Lossless** `[Camel]` algorithm requires that the number of decimal places be between 1 and 4.