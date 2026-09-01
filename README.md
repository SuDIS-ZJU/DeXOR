# DeXOR: Decimal-XOR Time-Series Compression

This repository contains the Java implementation and evaluation driver for
DeXOR, together with its error-bounded extension E-DeXOR. The internal algorithm
name `DeXORPlus` denotes E-DeXOR in the command-line interface and source tree.

The `extension` release is a source snapshot accompanying the extended DeXOR
study. It includes the runnable compression code, regression and ablation tests,
the Apache TsFile integration sources, and the VLDB 2026 poster and slides.

## Repository layout

- `src/main/java/`: compression algorithms, codecs, configuration handling, and
  the command-line entry point.
- `src/test/`: regression tests and experiment-specific test codecs.
- `tsfile/`: Apache TsFile integration, including DeXOR and E-DeXOR encoders and
  decoders.
- `datasets/Overall/`: compact datasets retained for local functional tests.
- `config.txt`: global validation settings and per-algorithm configuration.
- `VLDB_presentation/`: `DeXOR_VLDB_2026.pptx` and the VLDB 2026 poster PDF.

Large multi-source datasets, generated benchmark results, compressed output,
logs, IDE metadata, and build products are intentionally excluded from version
control. They can be supplied separately when reproducing full experiments.

### Excluded large-dataset provenance

The omitted files under `datasets/test/1_multi/` can be reconstructed from the
following public sources:

- `airlinedelaycauses_DelayedFlights.csv`: the 2008 `DelayedFlights.csv` file
  from Kaggle's [Airlines Delay](https://www.kaggle.com/giovamata/airlinedelaycauses/data)
  dataset, originally derived from U.S. Bureau of Transportation Statistics
  on-time performance data.
- `uspollution_pollution_us_2000_2016.csv`: Kaggle's
  [U.S. Pollution Data](https://www.kaggle.com/datasets/sogun3/uspollution),
  compiled from the U.S. EPA Air Quality System.
- `household_power_consumption.csv`: the UCI
  [Individual Household Electric Power Consumption](https://archive.ics.uci.edu/dataset/235/individual+household+electric+power+consumption)
  file, converted from semicolon-separated text to CSV without changing the
  observations.
- `merged_ECG.csv`: the 20 subject files `S01_ECG.csv` through `S20_ECG.csv`
  from PhysioNet's
  [Respiratory and heart rate monitoring dataset from aeration study](https://physionet.org/content/respiratory-heartrate-dataset/1.0.0/HRM_rawData/ECG/),
  concatenated once in subject order into a single sequence.

## Requirements and build

- Java 8 or later
- Maven 3

Build the executable JAR with all dependencies:

```bash
mvn clean package
```

The resulting executable is:

```text
target/DEXOR-1.0-jar-with-dependencies.jar
```

## Quick start

Run strict DeXOR with raw IEEE-754 bitwise validation:

```bash
java -jar target/DEXOR-1.0-jar-with-dependencies.jar \
  -in datasets/Overall \
  -out storage \
  -log results \
  -config config.txt \
  -m DeXOR \
  -verify-mode bitwise
```

Run E-DeXOR at two decimal places and validate at the reported precision:

```bash
java -jar target/DEXOR-1.0-jar-with-dependencies.jar \
  -in datasets/Overall \
  -out storage \
  -log results \
  -config config.txt \
  -m DeXORPlus \
  -lossy 2 \
  -verify-mode precision
```

Multiple algorithms may be listed after `-m`, for example:

```bash
java -jar target/DEXOR-1.0-jar-with-dependencies.jar \
  -in datasets/Overall \
  -m DeXOR DeXORPlus Gorilla Chimp128 ElfStar ALP
```

Input files are CSV sequences whose first two columns are interpreted as
`timestamp,value`. Algorithm names are case-insensitive.

## Command-line options

- `-in <path>`: input CSV file or directory; default: `./datasets/test`.
- `-out <path>`: compressed output directory; default: `./storage`.
- `-log <path>`: benchmark result directory; default: `./results`.
- `-config <path>`: configuration file; default: `./config.txt`.
- `-m <algorithms...>`: one or more algorithms; default: `DeXOR`.
- `-lossy <decimal_places>`: requested decimal precision for lossy algorithms.
- `-verify-mode <mode>`: `bitwise`, `precision`, or `none`.
- `-d <file>` / `-decompress <file>`: standalone decompression of a generated
  compressed file.

Implemented algorithm identifiers include `DeXOR`, `DeXORPlus`, `Gorilla`,
`Chimp`, `Chimp128`, `Elf`, `ElfPlus`, `ElfStar`, `SElfStar`, `Camel`, `ALP`,
`LZ4`, `Zstd`, `Snappy`, `SimPiece`, `MixPiece`, `SZ3`, `SZ3v2`, and `Axe`.

## DeXOR modes and validation

The default `config.txt` selects strict DeXOR:

```text
DeXOR{mode:strict,buffer_bits:0,rho:8,skip_available:-1}
```

- `mode:strict` enables the bitwise-safe fallback path.
- `mode:fast` uses decimal validation and avoids strict fallback overhead.
- `verification_mode:bitwise` compares raw IEEE-754 representations.
- `verification_mode:precision` compares values at the reported precision.
- `verification_mode:none` disables reconstruction validation.

Command-line `-verify-mode` and `-lossy` values override their global
configuration counterparts. Legacy `verify:true/false` remains supported.

## VLDB 2026 materials

- Slides: [`VLDB_presentation/DeXOR_VLDB_2026.pptx`](VLDB_presentation/DeXOR_VLDB_2026.pptx)
- Poster: [`VLDB_presentation/R12-1245-DeXOR.pdf`](VLDB_presentation/R12-1245-DeXOR.pdf)
