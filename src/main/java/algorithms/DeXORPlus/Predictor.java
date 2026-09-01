package algorithms.DeXORPlus;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.lang.reflect.Array;
import java.util.*;

public abstract class Predictor {
    public void insert(double value) {
        return;
    }

    public double get() {
        return 0;
    }

    public double getError(double value) {
        return Math.abs(value - get());
    }

    public static class DELTA extends Predictor {
        private final double[] buffer;

        public DELTA() {
            buffer = new double[1];
        }

        @Override
        public void insert(double value) {
            buffer[0] = value;
        }

        @Override
        public double get() {
            return buffer[0];
        }
    }

    public static class MAE extends Predictor {
        private final double[] buffer;
        private double sum;
        private final int size;
        private int count;
        private int point;

        public MAE(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("MAE predictor size must be positive");
            }
            buffer = new double[size];
            this.size = size;
            this.sum = 0;
            this.count = 0;
            this.point = 0;
        }

        @Override
        public void insert(double value) {
            sum -= buffer[point];
            buffer[point] = value;
            sum += value;
            if (count < size) count++;
            point++;
            point %= size;
        }

        @Override
        public double get() {
            return count == 0 ? 0 : sum / count;
        }


    }

    public static class ARIMA extends Predictor {
        private Deque<Double> slidingWindow;
        private int size;
        private boolean needFit = true;
        private algorithms.DeXORPlus.ARIMA arima;


        public ARIMA(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("ARIMA predictor size must be positive");
            }
            this.size = size;
            arima = new algorithms.DeXORPlus.ARIMA();
            slidingWindow = new ArrayDeque<>();
            this.needFit = true;
        }

        private double[] cast(){
            double[] arr = new double[slidingWindow.size()];
            int i = 0;
            for (Double d : slidingWindow) {
                arr[i++] = d;
            }
            return arr;
        }

        private void initModel() {
            double[] data = cast();
            arima.fit(data);
            arima.startRolling();
            this.needFit = false;
        }

        @Override
        public void insert(double value) {
            // 如果有上次的预测，计算误差并检查是否需要重训练
            slidingWindow.addLast(value);
            if (slidingWindow.size() > size) {
                slidingWindow.removeFirst();
            } else if (slidingWindow.size() < size) {
                return;
            }

            if (needFit) {
                initModel();
            }else{
                arima.updateWithActual(value);
            }
        }

        @Override
        public double get() {
            // 窗口未满，返回最后一个值（或0）
            if (slidingWindow.size() < size) {
                return slidingWindow.isEmpty() ? 0 : slidingWindow.getLast();
            }

            double prediction = arima.predictRolling();
            double last = slidingWindow.getLast();
            if (!Double.isFinite(prediction)) {
                return last;
            }

            // An unconstrained fit over a short window can occasionally
            // produce an explosive forecast.  The predictor is only a
            // reference for Decimal XOR.  Preserve ordinary extrapolation,
            // but fall back to the last reconstructed value when the result
            // is far outside the recent dynamic range.
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (double value : slidingWindow) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            double scale = Math.max(Math.abs(min), Math.abs(max));
            double margin = Math.max((max - min) * 8.0, Math.ulp(scale) * 16.0);
            return prediction < min - margin || prediction > max + margin ? last : prediction;
        }
    }
}
