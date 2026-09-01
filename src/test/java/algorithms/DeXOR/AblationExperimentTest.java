package algorithms.DeXOR;

import algorithms.DeXOR.decoder.TemporaryAblationDecoder;
import algorithms.DeXOR.encoder.TemporaryAblationEncoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Standalone test for the four DeXOR module-ablation variants in Section 6.3.2.
 * It reports deterministic ACB and fails on the first raw-bit reconstruction error.
 */
public final class AblationExperimentTest {
    private static final Path DEFAULT_DATA_DIR = Paths.get(
            "C:/Users/52542/WorkHome/DeXOR+/scripts/old_experiments/datasets/Overall");
    private static final Path DEFAULT_OUTPUT = Paths.get(
            "C:/Users/52542/WorkHome/DeXOR+/code/de-xor/target/ablation-test/ablation_results.csv");
    private static final String DIAGNOSTIC_FILE = "exception_path_diagnostics.csv";

    private static final Variant[] VARIANTS = new Variant[] {
            new Variant("DeXOR", true, true),
            new Variant("w/o Excep.", true, false),
            new Variant("w/o DEC. XOR", false, true),
            new Variant("w/o Both", false, false)
    };

    private AblationExperimentTest() {}

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        Path dataDir = args.length > 0 ? Paths.get(args[0]) : DEFAULT_DATA_DIR;
        Path output = args.length > 1 ? Paths.get(args[1]) : DEFAULT_OUTPUT;
        if (!Files.isDirectory(dataDir)) {
            throw new IllegalArgumentException("Dataset directory does not exist: " + dataDir);
        }

        List<Path> datasets = new ArrayList<Path>();
        java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.csv");
        try {
            for (Path path : stream) {
                datasets.add(path);
            }
        } finally {
            stream.close();
        }
        Collections.sort(datasets, Comparator.comparing(path -> path.getFileName().toString()));
        if (datasets.size() != 22) {
            throw new IllegalStateException(
                    "Section 6.3.2 expects 22 Overall datasets, but found " + datasets.size());
        }

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        double[] sums = new double[VARIANTS.length];
        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8);
             BufferedWriter diagnosticWriter = Files.newBufferedWriter(
                     output.resolveSibling(DIAGNOSTIC_FILE), StandardCharsets.UTF_8)) {
            writer.write("Dataset,DeXOR,w/o Excep.,w/o DEC. XOR,w/o Both");
            writer.newLine();
            diagnosticWriter.write("Dataset,Variant,Total Values,Exception Count,Exception Rate,"
                    + "Exception Rate (%),Strict-only Count,Fast-rejected Count,Capacity-rejected Count");
            diagnosticWriter.newLine();

            for (Path dataset : datasets) {
                List<Double> values = readValueColumn(dataset);
                if (values.isEmpty()) {
                    throw new IllegalStateException("No numeric values in " + dataset);
                }
                String datasetName = shortName(dataset);
                writer.write(datasetName);
                System.out.println("[dataset] " + datasetName + " values=" + values.size());

                for (int i = 0; i < VARIANTS.length; i++) {
                    Result result = runVariant(datasetName, values, VARIANTS[i]);
                    sums[i] += result.acb;
                    writer.write(String.format(Locale.ROOT, ",%.6f", result.acb));
                    diagnosticWriter.write(String.format(Locale.ROOT,
                            "%s,%s,%d,%d,%.9f,%.6f,%d,%d,%d%n",
                            datasetName, VARIANTS[i].name, values.size(), result.exceptionCount,
                            result.exceptionRate, result.exceptionRate * 100.0,
                            result.strictOnlyExceptionCount, result.fastRejectedCount,
                            result.capacityRejectedCount));
                    System.out.printf(Locale.ROOT,
                            "  %-14s ACB=%.6f bits, exceptions=%d (%.6f%%), bitwise=OK%n",
                            VARIANTS[i].name, result.acb, result.exceptionCount,
                            result.exceptionRate * 100.0);
                }
                writer.newLine();
                writer.flush();
                diagnosticWriter.flush();
            }

            writer.write("Average");
            for (double sum : sums) {
                writer.write(String.format(Locale.ROOT, ",%.6f", sum / datasets.size()));
            }
            writer.newLine();
        }
        System.out.println("Ablation test completed: " + output);
    }

    private static Result runVariant(String dataset, List<Double> values, Variant variant)
            throws Exception {
        Path encoded = Files.createTempFile("dexor-ablation-", ".bin");
        try {
            TemporaryAblationEncoder encoder = new TemporaryAblationEncoder(
                    encoded.toString(), variant.useDecimalXor, variant.useAdaptiveException);
            long totalBits = 0;
            for (double value : values) {
                totalBits += encoder.encode(value);
            }
            totalBits += encoder.close();
            encoder.flush();

            TemporaryAblationDecoder decoder = new TemporaryAblationDecoder(
                    encoded.toString(), variant.useDecimalXor, variant.useAdaptiveException);
            for (int i = 0; i < values.size(); i++) {
                double expected = values.get(i);
                double actual = decoder.decodeDouble();
                if (Double.doubleToRawLongBits(expected) != Double.doubleToRawLongBits(actual)) {
                    throw new AssertionError(dataset + " / " + variant.name
                            + " raw-bit mismatch at value " + i
                            + ": expected=" + expected + ", actual=" + actual);
                }
            }
            return new Result(
                    (double) totalBits / values.size(),
                    encoder.getExceptionCount(),
                    encoder.getStrictOnlyExceptionCount(),
                    encoder.getFastRejectedCount(),
                    encoder.getCapacityRejectedCount(),
                    values.size());
        } finally {
            Files.deleteIfExists(encoded);
        }
    }

    private static List<Double> readValueColumn(Path csv) throws Exception {
        List<Double> values = new ArrayList<Double>();
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.toLowerCase(Locale.ROOT).contains("value")) {
                throw new IllegalArgumentException("Expected timestamp,value CSV: " + csv);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);
                if (columns.length < 2 || columns[1].trim().isEmpty()) {
                    continue;
                }
                values.add(Double.parseDouble(columns[1].trim()));
            }
        }
        return values;
    }

    private static String shortName(Path dataset) {
        String[] parts = dataset.getFileName().toString().split("_", 3);
        return parts.length >= 2 ? parts[1] : dataset.getFileName().toString();
    }

    private static final class Variant {
        private final String name;
        private final boolean useDecimalXor;
        private final boolean useAdaptiveException;

        private Variant(String name, boolean useDecimalXor, boolean useAdaptiveException) {
            this.name = name;
            this.useDecimalXor = useDecimalXor;
            this.useAdaptiveException = useAdaptiveException;
        }
    }

    private static final class Result {
        private final double acb;
        private final long exceptionCount;
        private final long strictOnlyExceptionCount;
        private final long fastRejectedCount;
        private final long capacityRejectedCount;
        private final double exceptionRate;

        private Result(
                double acb,
                long exceptionCount,
                long strictOnlyExceptionCount,
                long fastRejectedCount,
                long capacityRejectedCount,
                long totalValues) {
            this.acb = acb;
            this.exceptionCount = exceptionCount;
            this.strictOnlyExceptionCount = strictOnlyExceptionCount;
            this.fastRejectedCount = fastRejectedCount;
            this.capacityRejectedCount = capacityRejectedCount;
            this.exceptionRate = totalValues == 0 ? 0 : (double) exceptionCount / totalValues;
        }
    }
}
