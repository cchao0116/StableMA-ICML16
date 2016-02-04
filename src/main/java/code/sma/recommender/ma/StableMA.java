package code.sma.recommender.ma;

import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.util.LoggerUtil;

/**
 * 
 * @author Hanke
 * @version $Id: StableSVD.java, v 0.1 Dec 22, 2015 11:43:15 AM Exp $
 */
public class StableMA extends MatrixFactorizationRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /** number of user/item clusters */
    protected int k;
    protected int numSet = 0;

    /** User profile in low-rank matrix form. */
    public int rMode;

    /*========================================
     * Constructors
     *========================================*/
    /**
     * Construct a matrix-factorization model with the given data.<br/>
     * 
     * Mode = 1, assign ratings based on user partitions <br/>
     * Mode = 2, split training data into k groups in uniformly <br/>
     * Mode = 3, make a subset of the data which containing 1/k ratings <br/> 
     * Mode = 4 or 104,, make k subsets where every subset contains 1 - 1\k data <br/> 
     * 
     * @param uc The number of users in the dataset.
     * @param ic The number of items in the dataset.
     * @param max The maximum rating value in the dataset.
     * @param min The minimum rating value in the dataset.
     * @param fc The number of features used for describing user and item profiles.
     * @param lr Learning rate for gradient-based or iterative optimization.
     * @param r Controlling factor for the degree of regularization. 
     * @param m Momentum used in gradient-based or iterative optimization.
     * @param iter The maximum number of iterations.
     * @param mode              the assignment mode
     */
    public StableMA(int uc, int ic, double max, double min, int fc, double lr, double r, double m,
                    int iter, int k, boolean verbose, int rMode) {
        super(uc, ic, max, min, fc, lr, r, m, iter, verbose);
        this.k = k;
        this.rMode = rMode;
    }

    /** 
     * @see edu.tongji.ml.matrix.MatrixFactorizationRecommender#buildModel(edu.tongji.data.MatlabFasionSparseMatrix, edu.tongji.data.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildModel(MatlabFasionSparseMatrix rateMatrix, MatlabFasionSparseMatrix tMatrix) {
        super.buildModel(rateMatrix, tMatrix);

        // Gradient Descent:
        int round = 0;
        int rateCount = rateMatrix.getNnz();
        double prevErr = 99999;
        double currErr = 9999;

        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        double[] Auis = rateMatrix.getVals();

        // rating assignment
        // pre-compute the rmse
        boolean[][] rAssigmnt = new boolean[k][rateCount];
        double[] errors = new double[rateCount];
        double[] seInSubset = new double[k];
        int[] numInSubset = new int[k];
        double se = assignStable(rateMatrix, rMode, rAssigmnt, errors, seInSubset, numInSubset);

        // update model
        boolean isCollaps = false;
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter && !isCollaps) {

            // compute essential errors
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];

                double AuiReal = Auis[numSeq];
                double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures);
                double err = AuiReal - AuiEst;

                // compute all rmse-s
                double rmse = Math.sqrt((se - errors[numSeq] + err * err) / rateCount);
                double[] subRMSES = new double[k];
                for (int kIndx = 0; kIndx < k; kIndx++) {
                    if (rAssigmnt[kIndx][numSeq] == false) {
                        continue;
                    }

                    subRMSES[kIndx] = Math.sqrt(
                        (seInSubset[kIndx] - errors[numSeq] + err * err) / numInSubset[kIndx]);
                    seInSubset[kIndx] = seInSubset[kIndx] - errors[numSeq] + err * err;
                }
                se = se - errors[numSeq] + err * err;
                errors[numSeq] = err * err;

                // stochastic gradient descend
                for (int s = 0; s < featureCount; s++) {
                    double Fus = userDenseFeatures.getValue(u, s);
                    double Gis = itemDenseFeatures.getValue(s, i);

                    double uGrad = err * Gis / rmse - regularizer * Fus;
                    double iGrad = err * Fus / rmse - regularizer * Gis;

                    for (int kIndx = 0; kIndx < k; kIndx++) {
                        if (rAssigmnt[kIndx][numSeq] == false) {
                            continue;
                        }

                        uGrad += err * Gis / (2 * k * subRMSES[kIndx]);
                        iGrad += err * Fus / (2 * k * subRMSES[kIndx]);
                    }

                    userDenseFeatures.setValue(u, s, Fus + learningRate * uGrad);
                    itemDenseFeatures.setValue(s, i, Gis + learningRate * iGrad);
                }
            }

            // Show progress:
            prevErr = currErr;
            currErr = Math.sqrt(se / rateCount);
            round++;

            if (showProgress & (round % 5 == 0) && tMatrix != null) {
                StringBuilder logStr = new StringBuilder();
                logStr.append(round + "\t" + String.format("%.4f", currErr));
                for (int gIndx = 0; gIndx < k; gIndx++) {
                    logStr.append('\t').append(
                        String.format("%.4f", Math.sqrt(seInSubset[gIndx] / numInSubset[gIndx])));
                }

                double prmse = this.evaluate(tMatrix);
                if (bestRMSE >= prmse) {
                    bestRMSE = prmse;
                } else {
                    isCollaps = true;
                }
                logStr.append('\t').append(String.format("%.4f", prmse));
                LoggerUtil.info(runningLogger, logStr.toString());
            } else {
                LoggerUtil.info(runningLogger, round + "\t" + String.format("%.4f", currErr));
            }

        }

        // final result
        double prmse = this.evaluate(tMatrix);
        bestRMSE = bestRMSE < prmse ? bestRMSE : prmse;
        LoggerUtil.info(resultLogger,
            "Param: FC: " + featureCount + "\tLR: " + learningRate + "\tR: " + regularizer + "\tk: "
                                      + k + "\tMODE: " + rMode + "\tnumSet: " + numSet + "\tRMSE: "
                                      + String.format("%.6f", bestRMSE));
    }

    /**
     * Assign ratings to different groups <br/>
     * Mode = 1, assign ratings based on user partitions <br/>
     * Mode = 2, split training data into k groups in uniformly <br/>
     * Mode = 3, make a subset of the data which containing 1/k ratings <br/> 
     * Mode = 4 or 104, make k subsets where every subset contains 1 - 1\k data <br/> 
     * 
     * @param rateMatrix        the matrix containing training data
     * @param mode              the assignment mode
     * @param rAssigmnt         rating assignment table
     * @param errors            the error for every ratings
     * @param seInSubset        the square sum of errors in different partitions
     * @param numInSubset       the number of ratings in different partitions
     * @return
     */
    protected double assignStable(MatlabFasionSparseMatrix rateMatrix, int mode,
                                  boolean[][] rAssigmnt, double[] errors, double[] seInSubset,
                                  int[] numInSubset) {
        int rateCount = rateMatrix.getNnz();

        if (mode == 2) {
            // Mode = 2, split training data into k groups in uniformly
            for (int kIndx = 0; kIndx < k; kIndx++) {
                Arrays.fill(rAssigmnt[kIndx], false);
            }

            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                double ran = Math.random();
                rAssigmnt[(int) (k * ran)][numSeq] = true;
            }
        } else if (mode == 3) {
            // Mode = 3, make a subset of the data which containing 1/k ratings
            for (int kIndx = 0; kIndx < k; kIndx++) {
                Arrays.fill(rAssigmnt[kIndx], false);
            }

            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                double ran = Math.random();
                if (ran * k >= 1) {
                    rAssigmnt[0][numSeq] = true;
                }
            }
        } else if (mode == 5) {
            // build RSVD model
            RegularizedSVD recmmd = new RegularizedSVD(userCount, itemCount, maxValue, minValue, 20,
                0.01, 0.001, 0, 30, false);
            recmmd.buildModel(rateMatrix, null);
            double recRMSE = recmmd.evaluate(rateMatrix);

            // compute a probability for every rating
            int[] uIndx = rateMatrix.getRowIndx();
            int[] iIndx = rateMatrix.getColIndx();
            double[] Auis = rateMatrix.getVals();

            double[] diffArr = new double[rateCount];
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];
                double AuiReal = Auis[numSeq];
                double AuiEst = recmmd.predict(u, i);

                diffArr[numSeq] = Math.abs(AuiReal - AuiEst) - recRMSE;
            }

            double sampleMean = StatUtils.mean(diffArr);
            double sampleSD = Math.sqrt(StatUtils.variance(diffArr));
            NormalDistribution normal = new NormalDistribution(sampleMean, sampleSD);

            // make a partition
            for (int kIndx = 0; kIndx < k; kIndx++) {
                Arrays.fill(rAssigmnt[kIndx], true);
            }
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                // the threshold to chose this rating as a perfect item
                double threshold = 1 - normal.cumulativeProbability(diffArr[numSeq]);
                if (Math.random() < 20.0 * threshold) {
                    rAssigmnt[(int) (k * Math.random())][numSeq] = false;
                    numSet++;
                }
            }
            System.out.println(numSet);
        } else if (mode == 6) {
            // build RSVD model
            RegularizedSVD recmmd = new RegularizedSVD(userCount, itemCount, maxValue, minValue, 20,
                0.01, 0.001, 0, 30, false);
            recmmd.buildModel(rateMatrix, null);
            double recRMSE = recmmd.evaluate(rateMatrix);

            // compute a probability for every rating
            int[] uIndx = rateMatrix.getRowIndx();
            int[] iIndx = rateMatrix.getColIndx();
            double[] Auis = rateMatrix.getVals();

            double[] diffArr = new double[rateCount];
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];
                double AuiReal = Auis[numSeq];
                double AuiEst = recmmd.predict(u, i);

                diffArr[numSeq] = Math.abs(AuiReal - AuiEst) - recRMSE;
            }

            double sampleMean = StatUtils.mean(diffArr);
            double sampleSD = Math.sqrt(StatUtils.variance(diffArr));
            NormalDistribution normal = new NormalDistribution(sampleMean, sampleSD);

            // make a partition
            for (int kIndx = 0; kIndx < k; kIndx++) {
                Arrays.fill(rAssigmnt[kIndx], false);
            }
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                // the threshold to chose this rating as a perfect item
                double threshold = 1 - normal.cumulativeProbability(diffArr[numSeq]);
                if (Math.random() < threshold) {
                    rAssigmnt[(int) (k * Math.random())][numSeq] = true;
                    numSet++;
                }
            }
            System.out.println(numSet);
        } else if (mode == 7) {
            // make a partition
            for (int kIndx = 0; kIndx < k; kIndx++) {
                Arrays.fill(rAssigmnt[kIndx], true);
            }

            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                if (Math.random() < 0.40d) {
                    continue;
                }

                // the threshold to chose this rating as a perfect item
                rAssigmnt[(int) (k * Math.random())][numSeq] = false;
            }
        }

        return initialStatParam(rateMatrix, rAssigmnt, errors, seInSubset, numInSubset);
    }

    /**
     * initial the statistical parameters
     * 
     * @param rateMatrix        the matrix containing training data
     * @param rAssigmnt         rating assignment table
     * @param errors            the error for every ratings
     * @param seInSubset        the square sum of errors in different partitions
     * @param numInSubset       the number of ratings in different partitions
     * @return
     */
    protected double initialStatParam(MatlabFasionSparseMatrix rateMatrix, boolean[][] rAssigmnt,
                                      double[] errors, double[] seInSubset, int[] numInSubset) {
        // refresh statistical parameters
        Arrays.fill(errors, 0.0d);
        Arrays.fill(seInSubset, 0.0d);
        Arrays.fill(numInSubset, 0);

        int rateCount = rateMatrix.getNnz();
        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        double[] Auis = rateMatrix.getVals();

        double se = 0.0d;
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];

            double AuiReal = Auis[numSeq];
            double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures);
            double error = AuiReal - AuiEst;

            errors[numSeq] = Math.pow(error, 2.0d);
            se += errors[numSeq];

            for (int kIndx = 0; kIndx < k; kIndx++) {
                if (rAssigmnt[kIndx][numSeq] == false) {
                    continue;
                }
                seInSubset[kIndx] += errors[numSeq];
                numInSubset[kIndx]++;
            }
        }
        return se;
    }

}
