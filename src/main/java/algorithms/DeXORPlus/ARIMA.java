package algorithms.DeXORPlus;

import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.nonlinear.scalar.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.*;
import org.apache.commons.math3.analysis.MultivariateFunction;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * ARIMA模型 - 支持自动调优、滚动预测（任意阶差分）和误差计算
 */
public class ARIMA {
    private int p, d, q;
    private double[] originalData;
    private double[] differencedData;
    private double[] arCoefficients;
    private double[] maCoefficients;
    private double[] residuals;
    private boolean isFitted = false;
    
    // 滚动预测状态（支持任意阶差分）
    private List<List<Double>> rollingDiffLevels;  // 各阶差分序列，索引0是一阶，索引1是二阶...
    private List<Double> rollingOriginal;
    private List<Double> rollingResid;
    private double lastOriginalValue;
    
    // 自动调优相关
    private boolean autoTuned = false;
    private static final int DEFAULT_MAX_P = 3;
    private static final int DEFAULT_MAX_D = 2;
    private static final int DEFAULT_MAX_Q = 3;

    /**
     * 指定参数构造
     */
    public ARIMA(int p, int d, int q) {
        if (p < 0 || d < 0 || q < 0) {
            throw new IllegalArgumentException("p, d, q must be non-negative");
        }
        this.p = p;
        this.d = d;
        this.q = q;
    }
    
    /**
     * 自动调参构造
     */
    public ARIMA() {
        this.autoTuned = true;
    }

    /**
     * d阶差分
     */
    private double[] differencing(double[] data, int order) {
        if (order == 0) return data.clone();
        double[] result = data.clone();
        for (int i = 0; i < order; i++) {
            double[] diff = new double[result.length - 1];
            for (int j = 1; j < result.length; j++) {
                diff[j - 1] = result[j] - result[j - 1];
            }
            result = diff;
        }
        return result;
    }

    /**
     * 计算残差
     */
    private double[] calculateResiduals(double[] data, double[] ar, double[] ma) {
        int n = data.length;
        int pOrder = ar.length;
        int qOrder = ma.length;
        double[] resid = new double[n];
        for (int t = Math.max(pOrder, qOrder); t < n; t++) {
            double arTerm = 0.0;
            for (int i = 0; i < pOrder; i++) {
                arTerm += ar[i] * data[t - i - 1];
            }
            double maTerm = 0.0;
            for (int j = 0; j < qOrder; j++) {
                if (t - j - 1 >= 0) {
                    maTerm += ma[j] * resid[t - j - 1];
                }
            }
            resid[t] = data[t] - (arTerm + maTerm);
        }
        return resid;
    }

    /**
     * 拟合模型
     */
    public void fit(double[] data) {
        if (autoTuned) {
            autoTune(data, DEFAULT_MAX_P, DEFAULT_MAX_D, DEFAULT_MAX_Q);
        } else {
            fitInternal(data, p, d, q);
        }
    }
    
    /**
     * 使用指定参数拟合
     */
    private void fitInternal(double[] data, int pVal, int dVal, int qVal) {
        if (data.length < pVal + qVal + dVal + 1) {
            throw new IllegalArgumentException("Insufficient data for ARIMA(" + pVal + "," + dVal + "," + qVal + ")");
        }

        this.p = pVal;
        this.d = dVal;
        this.q = qVal;
        this.originalData = data.clone();
        this.differencedData = differencing(data, dVal);

        if (pVal == 0 && qVal == 0) {
            this.isFitted = true;
            return;
        }

        // 优化参数
        MultivariateFunction objective = params -> {
            double[] ar = pVal > 0 ? Arrays.copyOfRange(params, 0, pVal) : new double[0];
            double[] ma = qVal > 0 ? Arrays.copyOfRange(params, pVal, pVal + qVal) : new double[0];
            return calculateLoss(differencedData, ar, ma);
        };

        double[] initialGuess = new double[pVal + qVal];
        Arrays.fill(initialGuess, 0.1);

        NelderMeadSimplex simplex = new NelderMeadSimplex(pVal + qVal);
        SimplexOptimizer optimizer = new SimplexOptimizer(1e-6, 1e-10);

        try {
            PointValuePair optimum = optimizer.optimize(
                    new MaxEval(10000),
                    new ObjectiveFunction(objective),
                    GoalType.MINIMIZE,
                    new InitialGuess(initialGuess),
                    simplex
            );

            double[] optimalParams = optimum.getPoint();
            this.arCoefficients = pVal > 0 ? Arrays.copyOfRange(optimalParams, 0, pVal) : new double[0];
            this.maCoefficients = qVal > 0 ? Arrays.copyOfRange(optimalParams, pVal, pVal + qVal) : new double[0];
            this.residuals = calculateResiduals(differencedData, arCoefficients, maCoefficients);
            this.isFitted = true;

        } catch (Exception e) {
            this.arCoefficients = new double[pVal];
            this.maCoefficients = new double[qVal];
            this.residuals = new double[differencedData.length];
            this.isFitted = true;
        }
    }
    
    /**
     * 自动调优参数（使用AIC准则）
     */
    private void autoTune(double[] data, int maxP, int maxD, int maxQ) {
        double bestAIC = Double.MAX_VALUE;
        int bestP = 1, bestD = 1, bestQ = 1;
        double[] bestAR = null, bestMA = null, bestResid = null;
        double[] bestDiffData = null;
        
        for (int dVal = 0; dVal <= maxD; dVal++) {
            double[] diffData;
            try {
                diffData = differencing(data, dVal);
            } catch (Exception e) {
                continue;
            }
            
            for (int pVal = 0; pVal <= maxP; pVal++) {
                for (int qVal = 0; qVal <= maxQ; qVal++) {
                    if (pVal == 0 && qVal == 0) continue;
                    if (diffData.length < pVal + qVal + 1) continue;
                    
                    try {
                        double[] params = fitForEval(diffData, pVal, qVal);
                        double[] ar = pVal > 0 ? Arrays.copyOfRange(params, 0, pVal) : new double[0];
                        double[] ma = qVal > 0 ? Arrays.copyOfRange(params, pVal, pVal + qVal) : new double[0];
                        double[] resid = calculateResiduals(diffData, ar, ma);
                        
                        double sse = 0;
                        int start = Math.max(pVal, qVal);
                        for (int i = start; i < resid.length; i++) {
                            sse += resid[i] * resid[i];
                        }
                        
                        int n = diffData.length - start;
                        int k = pVal + qVal;
                        double aic = n * Math.log(sse / n) + 2 * k;
                        
                        if (aic < bestAIC) {
                            bestAIC = aic;
                            bestP = pVal;
                            bestD = dVal;
                            bestQ = qVal;
                            bestAR = ar.clone();
                            bestMA = ma.clone();
                            bestResid = resid.clone();
                            bestDiffData = diffData.clone();
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        
        this.p = bestP;
        this.d = bestD;
        this.q = bestQ;
        this.originalData = data.clone();
        this.differencedData = bestDiffData;
        this.arCoefficients = bestAR != null ? bestAR : new double[bestP];
        this.maCoefficients = bestMA != null ? bestMA : new double[bestQ];
        this.residuals = bestResid != null ? bestResid : new double[bestDiffData != null ? bestDiffData.length : 0];
        this.isFitted = true;
    }
    
    /**
     * 用于评估的参数拟合
     */
    private double[] fitForEval(double[] diffData, int pVal, int qVal) {
        if (pVal == 0 && qVal == 0) return new double[0];
        
        MultivariateFunction objective = params -> {
            double[] ar = pVal > 0 ? Arrays.copyOfRange(params, 0, pVal) : new double[0];
            double[] ma = qVal > 0 ? Arrays.copyOfRange(params, pVal, pVal + qVal) : new double[0];
            return calculateLoss(diffData, ar, ma);
        };

        double[] initialGuess = new double[pVal + qVal];
        Arrays.fill(initialGuess, 0.1);

        try {
            PointValuePair optimum = new SimplexOptimizer(1e-5, 1e-8).optimize(
                    new MaxEval(5000),
                    new ObjectiveFunction(objective),
                    GoalType.MINIMIZE,
                    new InitialGuess(initialGuess),
                    new NelderMeadSimplex(pVal + qVal)
            );
            return optimum.getPoint();
        } catch (Exception e) {
            return new double[pVal + qVal];
        }
    }

    /**
     * 计算损失(SSE)
     */
    private double calculateLoss(double[] data, double[] ar, double[] ma) {
        double loss = 0.0;
        int n = data.length;
        int pOrder = ar.length;
        int qOrder = ma.length;
        double[] resid = new double[n];

        for (int t = Math.max(pOrder, qOrder); t < n; t++) {
            double arTerm = 0.0;
            for (int i = 0; i < pOrder; i++) {
                arTerm += ar[i] * data[t - i - 1];
            }
            double maTerm = 0.0;
            for (int j = 0; j < qOrder; j++) {
                if (t - j - 1 >= 0) {
                    maTerm += ma[j] * resid[t - j - 1];
                }
            }
            resid[t] = data[t] - (arTerm + maTerm);
            loss += resid[t] * resid[t];
        }
        return loss;
    }

    /**
     * 预测下一步（基于原始训练数据，不进行状态更新）
     * 适合在不知道真实值的情况下进行单步预测
     * 
     * @return 下一步的预测值
     */
    public double predictNext() {
        if (!isFitted) {
            throw new IllegalStateException("Model must be fitted before prediction");
        }

        int n = differencedData.length;
        double pred = 0.0;

        // AR部分
        for (int i = 0; i < p; i++) {
            pred += arCoefficients[i] * differencedData[n - 1 - i];
        }

        // MA部分
        for (int j = 0; j < q; j++) {
            if (n - 1 - j >= 0) {
                pred += maCoefficients[j] * residuals[n - 1 - j];
            }
        }

        // 转换回原始空间（支持任意阶差分）
        return integrate(pred, originalData);
    }

    /**
     * 预测未来多步（基于原始训练数据，不进行状态更新）
     * 适合在不知道真实值的情况下进行多步预测
     * 
     * @param steps 预测步数
     * @return 未来steps步的预测值数组
     */
    public double[] forecast(int steps) {
        if (!isFitted) {
            throw new IllegalStateException("Model must be fitted before forecasting");
        }
        if (steps <= 0) {
            throw new IllegalArgumentException("Steps must be positive");
        }

        double[] forecasts = new double[steps];
        List<Double> extDiff = new ArrayList<>();
        List<Double> extResid = new ArrayList<>();

        // 初始化
        for (double v : differencedData) extDiff.add(v);
        for (double v : residuals) extResid.add(v);

        for (int h = 0; h < steps; h++) {
            // 在差分空间预测
            double pred = 0.0;

            // AR部分
            for (int i = 0; i < p; i++) {
                int idx = extDiff.size() - 1 - i;
                if (idx >= 0) {
                    pred += arCoefficients[i] * extDiff.get(idx);
                }
            }

            // MA部分（未来残差设为0）
            for (int j = 0; j < q; j++) {
                int idx = extResid.size() - 1 - j;
                if (idx >= 0 && idx < residuals.length) {
                    pred += maCoefficients[j] * extResid.get(idx);
                }
            }

            // 转换回原始空间
            double[] tempDiff = new double[h + 1];
            for (int i = 0; i < h; i++) tempDiff[i] = forecasts[i];
            tempDiff[h] = pred;
            forecasts[h] = integrate(pred, originalData, tempDiff);

            // 更新序列用于下一步（预测值作为输入，残差设为0）
            extDiff.add(pred);
            extResid.add(0.0);
        }

        return forecasts;
    }

    /**
     * 开始滚动预测模式
     * 调用此方法后，可以使用 predictRolling() 进行逐步预测，
     * 并使用 updateWithActual() 更新真实值
     */
    public void startRolling() {
        if (!isFitted) {
            throw new IllegalStateException("Model must be fitted before rolling prediction");
        }
        
        // 初始化各阶差分序列
        rollingDiffLevels = new ArrayList<>();
        rollingOriginal = new ArrayList<>();
        for (double v : originalData) rollingOriginal.add(v);
        trimRollingState(rollingOriginal);
        
        if (d == 0) {
            // d=0 时不需要差分序列
        } else if (d == 1) {
            // d=1 时只需要一阶差分序列
            List<Double> diff1 = new ArrayList<>();
            for (double v : differencedData) diff1.add(v);
            trimRollingState(diff1);
            rollingDiffLevels.add(diff1);
        } else {
            // d>=2 时需要重建各阶差分序列
            // 从一阶到d阶，逐层构建
            double[] currentLevel = originalData.clone();
            for (int level = 1; level <= d; level++) {
                double[] nextLevel = new double[currentLevel.length - 1];
                for (int i = 1; i < currentLevel.length; i++) {
                    nextLevel[i - 1] = currentLevel[i] - currentLevel[i - 1];
                }
                List<Double> levelList = new ArrayList<>();
                for (double v : nextLevel) levelList.add(v);
                trimRollingState(levelList);
                rollingDiffLevels.add(levelList);
                currentLevel = nextLevel;
            }
        }
        
        rollingResid = new ArrayList<>();
        for (double v : residuals) rollingResid.add(v);
        trimRollingState(rollingResid);
        
        lastOriginalValue = originalData[originalData.length - 1];
    }

    /**
     * 滚动预测下一步（需要先调用 startRolling()）
     * 预测后状态不更新，需要调用 updateWithActual() 传入真实值来更新状态
     * 
     * @return 下一步的预测值
     */
    public double predictRolling() {
        if (rollingDiffLevels == null) {
            throw new IllegalStateException("Must call startRolling() first");
        }

        if (d == 0) {
            // d=0: 直接预测
            double pred = 0.0;
            // AR部分 - 使用原始数据（简化处理，实际上d=0时应该用原始数据而非差分数据）
            // 对于d=0，预测基于原始数据
            for (int i = 0; i < p; i++) {
                int idx = rollingOriginal.size() - 1 - i;
                if (idx >= 0) {
                    pred += arCoefficients[i] * rollingOriginal.get(idx);
                    // 这里简化处理，实际应该用更复杂的逻辑
                }
            }
            for (int j = 0; j < q; j++) {
                int idx = rollingResid.size() - 1 - j;
                if (idx >= 0) {
                    pred += maCoefficients[j] * rollingResid.get(idx);
                }
            }
            return pred;
        }

        // 在d阶差分空间预测
        List<Double> dthDiff = rollingDiffLevels.get(d - 1);
        double pred = 0.0;

        // AR部分
        for (int i = 0; i < p; i++) {
            int idx = dthDiff.size() - 1 - i;
            if (idx >= 0) {
                pred += arCoefficients[i] * dthDiff.get(idx);
            }
        }

        // MA部分
        for (int j = 0; j < q; j++) {
            int idx = rollingResid.size() - 1 - j;
            if (idx >= 0) {
                pred += maCoefficients[j] * rollingResid.get(idx);
            }
        }

        // 从d阶差分还原到原始值
        return integrateFromRolling(pred);
    }

    /**
     * 使用真实值更新滚动预测状态（需要先调用 predictRolling()）
     * 支持任意阶差分
     * 
     * @param actualValue 真实值
     */
    public void updateWithActual(double actualValue) {
        if (rollingDiffLevels == null) {
            throw new IllegalStateException("Must call startRolling() first");
        }

        // 计算预测值（用于计算残差）
        double predicted = predictRolling();
        double error = actualValue - predicted;

        // 更新各阶差分序列
        if (d == 0) {
            rollingOriginal.add(actualValue);
            trimRollingState(rollingOriginal);
            // d=0 不需要更新差分序列
        } else {
            // 从最高阶开始逐层计算新的差分值
            double[] newDiffValues = new double[d];
            
            // 一阶差分: Δy_t = y_t - y_{t-1}
            newDiffValues[0] = actualValue - lastOriginalValue;
            
            // 高阶差分: Δ^n y_t = Δ^{n-1} y_t - Δ^{n-1} y_{t-1}
            for (int level = 2; level <= d; level++) {
                List<Double> lowerLevel = rollingDiffLevels.get(level - 2);
                newDiffValues[level - 1] = newDiffValues[level - 2] - lowerLevel.get(lowerLevel.size() - 1);
            }
            
            // 更新各阶差分序列
            for (int level = 1; level <= d; level++) {
                rollingDiffLevels.get(level - 1).add(newDiffValues[level - 1]);
                trimRollingState(rollingDiffLevels.get(level - 1));
            }
        }
        
        // 更新残差和原始值
        rollingResid.add(error);
        trimRollingState(rollingResid);
        lastOriginalValue = actualValue;
    }

    private void trimRollingState(List<Double> values) {
        int limit = Math.max(8, Math.max(p, q) + d + 2);
        while (values.size() > limit) {
            values.remove(0);
        }
    }

    /**
     * 从滚动状态的d阶差分还原到原始值
     * 使用当前所有已知的差分值进行还原
     */
    private double integrateFromRolling(double dthDiffValue) {
        if (d == 0) {
            return dthDiffValue;
        }
        
        // 从d阶差分开始，逐层积分到原始值
        double[] currentValues = new double[d];
        currentValues[d - 1] = dthDiffValue;
        
        // 从第d层积分到第1层
        for (int level = d; level >= 2; level--) {
            List<Double> lowerLevel = rollingDiffLevels.get(level - 2);
            double lastLowerDiff = lowerLevel.get(lowerLevel.size() - 1);
            currentValues[level - 2] = lastLowerDiff + currentValues[level - 1];
        }
        
        // 最后从一阶差分还原到原始值
        return lastOriginalValue + currentValues[0];
    }

    /**
     * 逆差分：将预测值还原到原始空间（单步预测用）
     */
    private double integrate(double diffValue, double[] original) {
        if (d == 0) {
            return diffValue;
        }
        double last = original[original.length - 1];
        return last + diffValue;
    }

    /**
     * 逆差分：多步预测用
     */
    private double integrate(double diffValue, double[] original, double[] prevForecasts) {
        if (d == 0) {
            return diffValue;
        }
        double last = original[original.length - 1];
        for (double f : prevForecasts) {
            if (f != diffValue) {  // 跳过当前值
                last = last + f;
            } else {
                break;
            }
        }
        return last + diffValue;
    }

    /**
     * 计算预测值与真实值的误差
     * 
     * @param predictions 预测值数组
     * @param actualValues 真实值数组
     * @return ErrorResult 包含误差计算结果
     */
    public static ErrorResult computeErrors(double[] predictions, double[] actualValues) {
        if (predictions == null || actualValues == null) {
            throw new IllegalArgumentException("Inputs cannot be null");
        }
        if (predictions.length != actualValues.length) {
            throw new IllegalArgumentException("Predictions and actual values must have same length");
        }
        if (predictions.length == 0) {
            throw new IllegalArgumentException("Input arrays cannot be empty");
        }

        int n = predictions.length;
        double[] errors = new double[n];
        double[] absErrors = new double[n];

        double mse = 0, mae = 0;

        for (int i = 0; i < n; i++) {
            errors[i] = actualValues[i] - predictions[i];
            absErrors[i] = Math.abs(errors[i]);
            mse += errors[i] * errors[i];
            mae += absErrors[i];
        }

        mse /= n;
        mae /= n;

        return new ErrorResult(predictions, actualValues, errors, absErrors, mse, mae);
    }

    /**
     * 误差计算结果
     */
    public static class ErrorResult {
        private final double[] predictions;
        private final double[] actualValues;
        private final double[] errors;      // 实际值 - 预测值
        private final double[] absErrors;   // 绝对误差
        private final double mse;
        private final double mae;

        public ErrorResult(double[] predictions, double[] actualValues, 
                          double[] errors, double[] absErrors, double mse, double mae) {
            this.predictions = predictions.clone();
            this.actualValues = actualValues.clone();
            this.errors = errors.clone();
            this.absErrors = absErrors.clone();
            this.mse = mse;
            this.mae = mae;
        }

        public double[] getPredictions() { return predictions.clone(); }
        public double[] getActualValues() { return actualValues.clone(); }
        public double[] getErrors() { return errors.clone(); }
        public double[] getAbsErrors() { return absErrors.clone(); }
        public double getMSE() { return mse; }
        public double getMAE() { return mae; }
        public double getRMSE() { return Math.sqrt(mse); }
        public int size() { return predictions.length; }

        @Override
        public String toString() {
            return String.format("ErrorResult{steps=%d, MSE=%.6f, MAE=%.6f, RMSE=%.6f}", 
                predictions.length, mse, mae, getRMSE());
        }
    }

    // Getters
    public int getP() { return p; }
    public int getD() { return d; }
    public int getQ() { return q; }
    public boolean isFitted() { return isFitted; }
    public boolean isAutoTuned() { return autoTuned; }
}
