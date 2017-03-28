package code.sma.recmmd.standalone;

import java.util.Arrays;

import org.apache.commons.math3.stat.StatUtils;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recmmd.RecConfigEnv;
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
    public StableMA(RecConfigEnv rce) {
        super(rce);
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
                double diff = lossFunction.diff(AuiReal, AuiEst);

                // compute all rmse-s
                double rmse = Math.sqrt((se - errors[numSeq] + diff) / rateCount);
                double[] subRMSES = new double[numOfHPSet];
                for (int kIndx = 0; kIndx < numOfHPSet; kIndx++) {
                    if (rAssigmnt[kIndx][numSeq] == false) {
                        continue;
                    }

                    subRMSES[kIndx] = Math
                        .sqrt((seInSubset[kIndx] - errors[numSeq] + diff) / numInSubset[kIndx]);
                    seInSubset[kIndx] = seInSubset[kIndx] - errors[numSeq] + diff;
                }
                se = se - errors[numSeq] + diff;
                errors[numSeq] = diff;

                // stochastic gradient descend
                double deriWRTp = lossFunction.dervWRTPrdctn(AuiReal, AuiEst);
                for (int s = 0; s < featureCount; s++) {
                    double Fus = userDenseFeatures.getValue(u, s);
                    double Gis = itemDenseFeatures.getValue(i, s);

                    double uGrad = -deriWRTp * Gis / rmse - regularizer * regType.reg(null, 0, Fus);
                    double iGrad = -deriWRTp * Fus / rmse - regularizer * regType.reg(null, 0, Gis);

                    for (int kIndx = 0; kIndx < numOfHPSet; kIndx++) {
                        if (rAssigmnt[kIndx][numSeq] == false) {
                            continue;
                        }

                        uGrad += -deriWRTp * Gis / (2 * numOfHPSet * subRMSES[kIndx]);
                        iGrad += -deriWRTp * Fus / (2 * numOfHPSet * subRMSES[kIndx]);
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
        RecConfigEnv rce = new RecConfigEnv();
        rce.put("USER_COUNT_VALUE", userCount * 1.0);
        rce.put("ITEM_COUNT_VALUE", itemCount * 1.0);
        rce.put("MAX_RATING_VALUE", maxValue * 1.0);
        rce.put("MIN_RATING_VALUE", minValue * 1.0);

        rce.put("FEATURE_COUNT_VALUE", 30 * 1.0);
        rce.put("LEARNING_RATE_VALUE", 0.01);
        rce.put("REGULAIZED_VALUE", 0.001);
        rce.put("MAX_ITERATION_VALUE", 30 * 1.0);
        rce.put("VERBOSE_BOOLEAN", false);

        RegularizedSVD recmmd = new RegularizedSVD(rce);
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
