package code.sma.recommender.ma;

import java.io.Serializable;

import org.apache.log4j.Logger;

import code.sma.datastructure.DenseMatrix;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recommender.Recommender;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;

/**
 * This is an abstract class implementing four matrix-factorization-based methods
 * including Regularized SVD, NMF, PMF, and Bayesian PMF.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public abstract class MatrixFactorizationRecommender extends Recommender implements Serializable {
    /** SerialVersionNum */
    private static final long     serialVersionUID = 1L;

    /** The number of features. */
    public int                    featureCount;
    /** Learning rate parameter. */
    public double                 learningRate;
    /** Regularization factor parameter. */
    public double                 regularizer;
    /** Momentum parameter. */
    public double                 momentum;
    /** Maximum number of iteration. */
    public int                    maxIter;
    /** The best RMSE in test*/
    protected double              bestRMSE         = Double.MAX_VALUE;

    /** Indicator whether to show progress of iteration. */
    public boolean                showProgress;
    /** Offset to rating estimation. Usually this is the average of ratings. */
    protected double              offset;

    /** User profile in low-rank matrix form. */
    public DenseMatrix            userDenseFeatures;
    /** Item profile in low-rank matrix form. */
    public DenseMatrix            itemDenseFeatures;

    /** logger */
    protected final static Logger runningLogger    = Logger
        .getLogger(LoggerDefineConstant.SERVICE_CORE);
    protected final static Logger resultLogger     = Logger
        .getLogger(LoggerDefineConstant.SERVICE_NORMAL);

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
     */
    public MatrixFactorizationRecommender(int uc, int ic, double max, double min, int fc, double lr,
                                          double r, double m, int iter, boolean verbose) {
        userCount = uc;
        itemCount = ic;
        maxValue = max;
        minValue = min;

        featureCount = fc;
        learningRate = lr;
        regularizer = r;
        momentum = m;
        maxIter = iter;

        showProgress = verbose;
    }

    /*========================================
     * Model Builder
     *========================================*/
    /**
     * @see edu.tongji.ml.Recommender#buildModel(edu.tongji.data.MatlabFasionSparseMatrix, edu.tongji.data.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildModel(MatlabFasionSparseMatrix rateMatrix, MatlabFasionSparseMatrix tMatrix) {
        LoggerUtil.info(runningLogger,
            "Param: FC: " + featureCount + "\tLR: " + learningRate + "\tR: " + regularizer);

        //user features
        userDenseFeatures = new DenseMatrix(userCount, featureCount);
        for (int u = 0; u < userCount; u++) {
            for (int f = 0; f < featureCount; f++) {
                double rdm = Math.random() / featureCount;
                userDenseFeatures.setValue(u, f, rdm);
            }
        }

        //item features
        itemDenseFeatures = new DenseMatrix(featureCount, itemCount);
        for (int i = 0; i < itemCount; i++) {
            for (int f = 0; f < featureCount; f++) {
                double rdm = Math.random() / featureCount;
                itemDenseFeatures.setValue(f, i, rdm);
            }
        }
    }

    /*========================================
     * Record Logs & Dynamic Stopper
     *========================================*/
    /**
     * Record Logs & Dynamic Stopper
     * 
     * @param round         the current round
     * @param tMatrix       test matrix
     * @param currErr       the current training error
     * @return              true to stop, false to continue
     */
    protected boolean recordLoggerAndDynamicStop(int round, MatlabFasionSparseMatrix tMatrix,
                                                 double currErr) {
        if (showProgress && (round % 5 == 0) && tMatrix != null) {
            double prmse = this.evaluate(tMatrix);
            LoggerUtil.info(runningLogger, round + "\t" + String.format("%.6f", currErr) + "\t"
                                           + String.format("%.6f", prmse));
            if (bestRMSE >= prmse) {
                bestRMSE = prmse;
            } else {
                return true;
            }
        } else {
            LoggerUtil.info(runningLogger, round + "\t" + String.format("%.6f", currErr));
        }

        return false;
    }

    /**
     * Record final logs 
     * 
     * @param tMatrix
     */
    protected void finalizeLogger(MatlabFasionSparseMatrix tMatrix) {
        if (tMatrix == null) {
            return;
        }

        double prmse = this.evaluate(tMatrix);
        bestRMSE = bestRMSE < prmse ? bestRMSE : prmse;
        LoggerUtil.info(resultLogger,
            "Param: FC: " + featureCount + "\tLR: " + learningRate + "\tR: " + regularizer
                                      + "\tRMSE: " + String.format("%.6f", bestRMSE));

    }

    /*========================================
     * Prediction
     *========================================*/

    /**
     * Evaluate the designated algorithm with the given test data.
     * 
     * @param testMatrix The rating matrix with test data.
     * 
     * @return The result of evaluation, such as MAE, RMSE, and rank-score.
     */
    public double evaluate(MatlabFasionSparseMatrix testMatrix) {
        double RMSE = 0.0d;

        int rateCount = testMatrix.getNnz();
        int[] uIndx = testMatrix.getRowIndx();
        int[] iIndx = testMatrix.getColIndx();
        double[] Auis = testMatrix.getVals();
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            double AuiReal = Auis[numSeq];
            double AuiEst = predict(u, i);

            RMSE += Math.pow(AuiReal - AuiEst, 2.0d);
        }

        return Math.sqrt(RMSE / rateCount);
    }

    /**
     * @see edu.tongji.ml.Recommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        double prediction = this.offset;
        if (userDenseFeatures != null && itemDenseFeatures != null) {
            prediction += userDenseFeatures.innerProduct(u, i, itemDenseFeatures);
        } else {
            throw new RuntimeException("features were not initialized.");
        }

        if (prediction > maxValue) {
            return maxValue;
        } else if (prediction < minValue) {
            return minValue;
        } else {
            return prediction;
        }
    }

}
