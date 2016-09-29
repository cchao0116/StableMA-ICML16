package code.sma.recommender.standalone;

import java.util.Arrays;

import org.apache.commons.math3.stat.StatUtils;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recommender.RecConfigEnv;
import code.sma.util.LoggerUtil;

/**
 * This is a class implementing SMA (Stable Matrix Approximation).
 * Technical detail of the algorithm can be found in
 * Dongsheng Li, Stable Matrix Approximation,
 * Proceedings of ICML, 2016.
 * 
 * @author Chao Chen
 * @version $Id: StableSVD.java, v 0.1 Dec 22, 2015 11:43:15 AM Exp $
 */
public class StableMA extends MatrixFactorizationRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;
    /** Number of hard-predictable subsets */
    protected int             numOfHPSet;

    /*========================================
     * Constructors
     *========================================*/
    /**
     * Construct a matrix-factorization-based model with the given data.
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
     * @param verbose Indicating whether to show iteration steps and train error.
     * @param rce The recommender's specific parameters
     */
    public StableMA(int uc, int ic, double max, double min, int fc, double lr, double r, double m,
                    int iter, boolean verbose, RecConfigEnv rce) {
        super(uc, ic, max, min, fc, lr, r, m, iter, verbose, rce);
        numOfHPSet = ((Double) rce.get("NUMBER_HARD_PREDICTION_SET_VALUE")).intValue();
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
        boolean[][] rAssigmnt = new boolean[numOfHPSet][rateCount];
        double[] errors = new double[rateCount];
        double[] seInSubset = new double[numOfHPSet];
        int[] numInSubset = new int[numOfHPSet];
        double se = assignStable(rateMatrix, rAssigmnt, errors, seInSubset, numInSubset);
        LoggerUtil.info(runningLogger, "Param: FC: " + featureCount + "\tLR: " + learningRate
                                       + "\tR: " + regularizer + "\tT: " + rateCount);

        // update model
        boolean isCollaps = false;
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter && !isCollaps) {

            // compute essential errors
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];

                double AuiReal = Auis[numSeq];
                double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                double err = AuiReal - AuiEst;

                // compute all rmse-s
                double rmse = Math.sqrt((se - errors[numSeq] + err * err) / rateCount);
                double[] subRMSES = new double[numOfHPSet];
                for (int kIndx = 0; kIndx < numOfHPSet; kIndx++) {
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
                    double Gis = itemDenseFeatures.getValue(i, s);

                    double uGrad = err * Gis / rmse - regularizer * Fus;
                    double iGrad = err * Fus / rmse - regularizer * Gis;

                    for (int kIndx = 0; kIndx < numOfHPSet; kIndx++) {
                        if (rAssigmnt[kIndx][numSeq] == false) {
                            continue;
                        }

                        uGrad += err * Gis / (2 * numOfHPSet * subRMSES[kIndx]);
                        iGrad += err * Fus / (2 * numOfHPSet * subRMSES[kIndx]);
                    }

                    userDenseFeatures.setValue(u, s, Fus + learningRate * uGrad, true);
                    itemDenseFeatures.setValue(i, s, Gis + learningRate * iGrad, true);
                }
            }

            // Show progress:
            prevErr = currErr;
            currErr = Math.sqrt(se / rateCount);
            round++;

            isCollaps = recordLoggerAndDynamicStop(round, tMatrix, currErr);
        }
    }

    /**
     * Assign ratings to different groups <br/>
     * 
     * @param rateMatrix        the matrix containing training data
     * @param rAssigmnt         rating assignment table
     * @param errors            the error for every ratings
     * @param seInSubset        the square sum of errors in different partitions
     * @param numInSubset       the number of ratings in different partitions
     * @return
     */
    protected double assignStable(MatlabFasionSparseMatrix rateMatrix, boolean[][] rAssigmnt,
                                  double[] errors, double[] seInSubset, int[] numInSubset) {
        int rateCount = rateMatrix.getNnz();

        // build RSVD model
        RegularizedSVD recmmd = new RegularizedSVD(userCount, itemCount, maxValue, minValue, 20,
            0.01, 0.001, 0, 30, false, null);
        recmmd.buildModel(rateMatrix, null);
        double recRMSE = recmmd.evaluate(rateMatrix).getRMSE();

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

        // make a partition
        for (int kIndx = 0; kIndx < numOfHPSet; kIndx++) {
            Arrays.fill(rAssigmnt[kIndx], true);
        }
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            double rand = Math.random();
            boolean isHardPredictableItem = diffArr[numSeq] > sampleMean;

            if ((isHardPredictableItem & (rand < 0.45)) | (!isHardPredictableItem & rand < 0.60)) {
                rAssigmnt[(int) (numOfHPSet * Math.random())][numSeq] = false;
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
            double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
            double error = AuiReal - AuiEst;

            errors[numSeq] = Math.pow(error, 2.0d);
            se += errors[numSeq];

            for (int kIndx = 0; kIndx < numOfHPSet; kIndx++) {
                if (rAssigmnt[kIndx][numSeq] == false) {
                    continue;
                }
                seInSubset[kIndx] += errors[numSeq];
                numInSubset[kIndx]++;
            }
        }
        return se;
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Param: FC: " + featureCount + " LR: " + learningRate + " R: " + regularizer
               + " ALG[SMA][" + numOfHPSet + "]";
    }

}
