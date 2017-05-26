package code.sma.util;

import java.util.Comparator;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ujmp.core.util.MathUtil;

import com.google.common.collect.MinMaxPriorityQueue;

import code.sma.core.DynIntArr;
import code.sma.core.Tuples;
import code.sma.core.impl.SparseMatrix;
import code.sma.core.impl.SparseVector;
import code.sma.recmmd.Recommender;

/**
 * This is a unified class providing evaluation metrics,
 * including comparison of predicted ratings and rank-based metrics, etc.
 * 
 * @author Chao.Chen
 * @version $Id: EvaluationMetrics.java, v 0.1 2016年9月29日 下午2:09:04 Chao.Chen Exp $
 */
public class EvaluationMetrics {
    /** Top-N recommendations*/
    private int         N = -1;
    /** Mean Absoulte Error (MAE) */
    private double      mae;
    /** Mean Squared Error (MSE) */
    private double      mse;
    /** Rank-based Normalized Discounted Cumulative Gain (NDCG) */
    private double      ndcg;
    private double      recall;
    /** Average Precision */
    private double      avgPrecision;
    /** Recommender to produce prediction*/
    private Recommender recmmd;

    public EvaluationMetrics(Recommender recmmd, Tuples ttMatrix) {
        super();
        this.recmmd = recmmd;
        build(ttMatrix, null);
    }

    public EvaluationMetrics(Recommender recmmd, Tuples ttMatrix,
                             Tuples trMatrix, int N) {
        super();
        this.recmmd = recmmd;
        this.N = N;
        build(ttMatrix, trMatrix);
    }

    /**
     * compute all the evaluations
     * 
     * @param ttMatrix  test data
     */
    protected void build(Tuples ttMatrix, Tuples trMatrix) {
        // Rating Prediction evaluation
        int totlCount = ttMatrix.getNnz();
        int[] uIndx = ttMatrix.getRowIndx();
        int[] iIndx = ttMatrix.getColIndx();
        double[] Auis = ttMatrix.getVals();
        for (int numSeq = 0; numSeq < totlCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double realVal = Auis[numSeq];
            double predVal = recmmd.predict(u, i);
            mae += Math.abs(realVal - predVal);
            mse += Math.pow(realVal - predVal, 2.0d);

        }
        mae /= totlCount;
        mse /= totlCount;

        // Top-N evaluation
        if (N > 0) {
            int userCount = recmmd.userCount;
            int itemCount = recmmd.itemCount;
            DynIntArr[] dArr = new DynIntArr[userCount];
            for (int numSeq = 0; numSeq < totlCount; numSeq++) {
                if (dArr[uIndx[numSeq]] == null) {
                    dArr[uIndx[numSeq]] = new DynIntArr(N);
                }
                dArr[uIndx[numSeq]].addValue(numSeq);
            }

            SparseMatrix trainMatrix = trMatrix.toSparseMatrix(userCount, itemCount);

            int avgPEffectiveUserCount = 0;
            for (int u = 0; u < userCount; u++) {
                if (dArr[u] == null || dArr[u].size() < N) {
                    continue;
                }
                avgPEffectiveUserCount++;

                // get Top-N recommendations
                int[] topNRcmdn = new int[N];
                {
                    MinMaxPriorityQueue<Pair<Integer, Double>> fpQue = MinMaxPriorityQueue
                        .orderedBy(new Comparator<Pair<Integer, Double>>() {
                            @Override
                            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                                return (int) Math.signum(o2.getValue() - o1.getValue());
                            }

                        }).maximumSize(N).create();

                    // filtering training data
                    for (int i = 0; i < itemCount; i++) {
                        if (trainMatrix.getValue(u, i) != 0.0d) {
                            continue;
                        }

                        Pair<Integer, Double> uidVSratg = new ImmutablePair<Integer, Double>(i,
                            recmmd.predict(u, i));
                        fpQue.add(uidVSratg);
                    }

                    for (int s = 0; s < N; s++) {
                        topNRcmdn[s] = fpQue.poll().getKey();
                    }
                }

                // get user real ratings

                SparseVector realVs = new SparseVector(itemCount);
                for (int n : dArr[u].getArr()) {
                    realVs.setValue(iIndx[n], Auis[n]);

                }

                double lRecl = 0.0d;
                for (int s = 0; s < N; s++) {
                    int rcmmdtn = topNRcmdn[s];
                    if (realVs.getValue(rcmmdtn) != 0.0d) {
                        avgPrecision++;
                        lRecl++;
                        ndcg += 1 / MathUtil.log2(s + 2);
                    }
                }
                recall += lRecl / realVs.length();
            }

            double iNDCG = 0.0d;
            for (int s = 0; s < N; s++) {
                iNDCG += 1 / MathUtil.log2(s + 2);
            }
            recall /= avgPEffectiveUserCount;
            ndcg /= iNDCG * avgPEffectiveUserCount;
            avgPrecision /= N * avgPEffectiveUserCount;
        }
    }

    public double getRecall() {
        return recall;
    }

    /**
     * Getter method for property <tt>mae</tt>.
     * 
     * @return property value of mae
     */
    public double getMAE() {
        return mae;
    }

    /**
     * Getter method for property <tt>mse</tt>.
     * 
     * @return property value of mse
     */
    public double getRMSE() {
        return Math.sqrt(mse);
    }

    /**
     * Getter method for property <tt>ndcg</tt>.
     * 
     * @return property value of ndcg
     */
    public double getNDCG() {
        return ndcg;
    }

    /**
     * Getter method for property <tt>avgPrecision</tt>.
     * 
     * @return property value of avgPrecision
     */
    public double getAvgPrecision() {
        return avgPrecision;
    }

    /**
     * Print all loss values in one line.
     * 
     * @return The one-line string to be printed.
     */
    public String printOneLine() {
        return String.format("MAE(%.6f) RMSE(%.6f) NDCG@%d(%.6f) AP(%.6f) Recall(%.6f)",
            this.getMAE(), this.getRMSE(), N, this.getNDCG(), this.getAvgPrecision(),
            this.getRecall());
    }
}
