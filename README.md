
# DeXOR: Enabling XOR in Decimal Space for Streaming Lossless Compression of Floating-point Data

---

This repository contains the code, datasets, and supplementary material for the paper titled "DeXOR: Enabling XOR in Decimal Space for Streaming Lossless Compression of Floating-point Data".📚

# <span style="color: red;">Supplemental Material</span>

More technical details, supporting theory and proofs, and additional experimental details can be found in the [Appendix](document/Appendix.pdf). Downloading the file to a local PDF viewer is recommended for better readability. 📖

# Project Structure

This project is constructed using **Maven** and includes a variety of compression algorithms housed within the `src/main/java/algorithms` directory. These algorithms inherit from a common parent class, `Algorithm`, and are managed through `AlgorithmManager` and `AlgorithmEnums`. Each algorithm features corresponding `Encoder` and `Decoder` classes for the supported data types (INT32, INT64, Float, Double), which facilitate compression and decompression, respectively. 🛠️

# Getting Started

We recommend using **IntelliJ IDEA** to build this project. After cloning this repository, the project can be built using **Maven**:

```bash
  mvn clean install
```

Once the build process is complete, the resultant JAR file, which includes all dependencies, will be located in the `target` directory:

```bash
  target/OL-TSC-1.0-jar-with-dependencies.jar
```

Ensure that your runtime environment has **Java 8** installed before starting the compression process. 💻


* To rigorously replicate the experiments reported herein, one must first pre-install Apache IoTDB’s native columnar storage engine, **TSFile**(https://github.com/apache/tsfile), and subsequently augment the project’s `pom.xml` with the requisite dependency.

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.apache.tsfile</groupId>
  <artifactId>tsfile</artifactId>
  <version>2.2.0-SNAPSHOT</version>
</dependency>
```

* The TSFile implementation incorporating DeXOR resides within this project, compile it according to the provided tutorial:
  [Building Tsfile with Java](./tsfile/java/tsfile/README.md)


# Parameter Introduction

To run this package, use the `java -jar` command, as shown below:

```bash
  java -jar target/OL-TSC-1.0-jar-with-dependencies.jar -in [INPUT_PATH] -out [OUTPUT_PATH] -log [LOG_PATH] -m [METHOD] -config [CONFIG_PATH]
```

The following options are available for customizing the compression process:

- `-in [INPUT_PATH]`: The source of the files to be compressed, currently supporting files in CSV format with the structure of `<timestamp, value>`. Default is `./datasets/Overall`.
- `-out [OUTPUT_PATH]`: The directory where the compressed binary files will be stored. Default is `./storage`.
- `-log [LOG_PATH]`: The directory where the results of the experiment benchmarks will be saved. Default is `./results`.
- `-config [CONFIG_PATH]`: If a configuration file is specified, it can be used to define the global settings for compression and decompression of a certain class of algorithms. For instance, the available settings for DeXOR include `rho`, `skip_available`, and `buffer_bits`. Default is `null`.
    - **rho**: A parameter within the DeXOR **Exception Handler** module.
    - **skip_available**: Specifies the number of consecutive exceptions after which the main process is abandoned in favor of entering the exception control directly.
    - **buffer_bits**: Declares the number of bits used for expanding the buffer.

  **Note**: The settings `buffer_bits` and `skip_available` cannot be used simultaneously.

- `-m [METHOD]`: The name of the compression algorithm to be used. Currently supported algorithms include `Gorilla`, `Chimp`, `Chimp128`, `Elf`, `ElfPlus`, `Camel`, `DeXOR`, `ALP`, `Elf*`, and `SElf*`. The algorithm names are case-insensitive. Default is `DeXOR`.

You can test multiple algorithms in a manner similar to the example provided:

``` bash
  java -jar OL-TSC-1.0-jar-with-dependencies.jar -in ./datasets/Overall -m DeXOR Gorilla Chimp Chimp128 Elf ElfPlus
```

# Notified

- Currently, only the `double` data type is supported. 

- The application scenario of the **Lossless** `[Camel]` algorithm requires that the number of decimal places be between 1 and 4. ⚠️

