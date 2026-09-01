package algorithms.ElfStar;

import algorithms.ElfStar.decoder.DoubleElfStarDecoder;
import algorithms.ElfStar.encoder.DoubleElfStarEncoder;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ElfStarWindowRegressionTest {
    private static double valueAt(int index) {
        switch (index % 11) {
            case 0: return 0.0;
            case 1: return -0.0;
            case 2: return index * 0.01;
            case 3: return -index * 0.125;
            case 4: return 50.12 + (index % 10) * 0.01;
            case 5: return -99.0;
            case 6: return 100.2;
            case 7: return 0.0001 * (index % 10);
            case 8: return 87.91657 + (index % 10) * 0.00001;
            case 9: return -1.23;
            default: return 42.0;
        }
    }

    private static void roundTrip(int count) throws Exception {
        Path encoded = Files.createTempFile("elfstar-window-", ".bin");
        try {
            DoubleElfStarEncoder encoder = new DoubleElfStarEncoder(encoded.toString());
            double[] expected = new double[count];
            for (int i = 0; i < count; i++) {
                expected[i] = valueAt(i);
                encoder.encode(expected[i]);
            }
            encoder.close();
            encoder.flush();

            DoubleElfStarDecoder decoder = new DoubleElfStarDecoder(encoded.toString());
            for (int i = 0; i < count; i++) {
                double actual = decoder.decodeDouble();
                long expectedBits = Double.doubleToRawLongBits(expected[i]);
                long actualBits = Double.doubleToRawLongBits(actual);
                if (expectedBits != actualBits) {
                    throw new AssertionError("count=" + count + ", index=" + i
                            + ", expectedBits=" + Long.toHexString(expectedBits)
                            + ", actualBits=" + Long.toHexString(actualBits));
                }
            }
        } finally {
            Files.deleteIfExists(encoded);
        }
    }

    public static void main(String[] args) throws Exception {
        for (int count : new int[]{1, 2, 999, 1000, 1001, 1680, 2000, 2001}) {
            roundTrip(count);
        }
        System.out.println("ElfStar window regression tests passed.");
    }
}
