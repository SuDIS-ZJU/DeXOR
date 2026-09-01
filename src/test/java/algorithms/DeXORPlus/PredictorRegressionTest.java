package algorithms.DeXORPlus;

import algorithms.DeXORPlus.decoder.DoubleDeXORPlusDecoder;
import algorithms.DeXORPlus.encoder.DoubleDeXORPlusEncoder;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PredictorRegressionTest {
    private static void assertClose(double expected, double actual, double tolerance, String message) {
        if (!Double.isFinite(actual) || Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void testMovingAverage() {
        Predictor.MAE predictor = new Predictor.MAE(3);
        assertClose(0, predictor.get(), 0, "empty MAE");
        predictor.insert(3);
        assertClose(3, predictor.get(), 0, "MAE warm-up 1");
        predictor.insert(6);
        assertClose(4.5, predictor.get(), 0, "MAE warm-up 2");
        predictor.insert(9);
        assertClose(6, predictor.get(), 0, "MAE full window");
        predictor.insert(12);
        assertClose(9, predictor.get(), 0, "MAE rolling window");
    }

    private static void testArimaD0Rolling() {
        ARIMA arima = new ARIMA(1, 0, 0);
        arima.fit(new double[]{10, 10, 10, 10, 10, 10, 10, 10});
        arima.startRolling();
        double prediction = arima.predictRolling();
        if (!Double.isFinite(prediction) || Math.abs(prediction) < 1) {
            throw new AssertionError("ARIMA(1,0,0) rolling prediction unexpectedly collapsed to " + prediction);
        }
        arima.updateWithActual(10);
        if (!Double.isFinite(arima.predictRolling())) {
            throw new AssertionError("ARIMA rolling prediction became non-finite after update");
        }
    }

    private static void testPredictorStateSymmetry() {
        double[] values = {1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 2.75, 3.0};
        Predictor[] left = {new Predictor.DELTA(), new Predictor.MAE(3), new Predictor.ARIMA(5)};
        Predictor[] right = {new Predictor.DELTA(), new Predictor.MAE(3), new Predictor.ARIMA(5)};
        for (int p = 0; p < left.length; p++) {
            for (double value : values) {
                assertClose(left[p].get(), right[p].get(), 0, "predictor state before insert");
                left[p].insert(value);
                right[p].insert(value);
            }
            assertClose(left[p].get(), right[p].get(), 0, "predictor state after sequence");
        }
    }

    private static void testCodecRoundTrip(String predictor, int size) throws Exception {
        Path encoded = Files.createTempFile("edexor-predictor-", ".bin");
        String config = "{decimal_places:2,predictor:" + predictor
                + ",predictor_size:" + size + ",cross_alpha_reuse:true}";
        double[] values = new double[80];
        for (int i = 0; i < values.length; i++)
            values[i] = 50.0 + 8.0 * Math.sin(i * 0.37) + (i % 5) * 0.1;
        try {
            DoubleDeXORPlusEncoder encoder = new DoubleDeXORPlusEncoder(encoded.toString(), config);
            for (double value : values) encoder.encode(value);
            encoder.close();
            encoder.flush();

            DoubleDeXORPlusDecoder decoder = new DoubleDeXORPlusDecoder(encoded.toString(), config);
            for (int i = 0; i < values.length; i++) {
                double actual = decoder.decodeDouble();
                double tolerance = Math.max(0.01, Math.ulp(values[i]));
                assertClose(values[i], actual, tolerance,
                        predictor + " codec round trip at index " + i);
            }
        } finally {
            Files.deleteIfExists(encoded);
        }
    }

    private static void testRealisticArimaRoundTrip() throws Exception {
        double[] pattern = {64.2, 49.4, 48.8, 46.4, 47.9, 48.7, 48.9, 49.1,
                49.0, 51.9, 51.7, 51.3, 47.0, 46.9, 47.5, 45.9, 44.5, 50.7,
                54.0, 52.6, 54.2, 51.0, 53.5, 54.2, 54.2, 52.6, 55.5, 53.8,
                54.3, 57.4, 56.9, 50.4, 50.1, 54.1, 49.1, 48.8, 50.7, 51.6,
                52.6, 53.2};
        Path encoded = Files.createTempFile("edexor-arima-realistic-", ".bin");
        String config = "{decimal_places:2,predictor:ARIMA,predictor_size:10}";
        try {
            DoubleDeXORPlusEncoder encoder = new DoubleDeXORPlusEncoder(encoded.toString(), config);
            for (double value : pattern) encoder.encode(value);
            encoder.close();
            encoder.flush();
            DoubleDeXORPlusDecoder decoder = new DoubleDeXORPlusDecoder(encoded.toString(), config);
            for (int i = 0; i < pattern.length; i++) {
                double actual = decoder.decodeDouble();
                assertClose(pattern[i], actual, 0.01,
                        "ARIMA realistic codec round trip at index " + i);
            }
        } finally {
            Files.deleteIfExists(encoded);
        }
    }

    public static void main(String[] args) throws Exception {
        testMovingAverage();
        testArimaD0Rolling();
        testPredictorStateSymmetry();
        testCodecRoundTrip("DELTA", 1);
        for (int size : new int[]{10, 20, 30, 50}) {
            testCodecRoundTrip("MAE", size);
            testCodecRoundTrip("ARIMA", size);
        }
        testRealisticArimaRoundTrip();
        System.out.println("Predictor regression tests passed.");
    }
}
