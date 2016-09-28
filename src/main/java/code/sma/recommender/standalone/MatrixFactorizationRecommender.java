package code.sma.recommender.standalone;

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
    protected static final long   serialVersionUID = 1L;

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

    /** indices involved in training */
    public int[]                  trainInvlvIndces;
    /** indices involved in testing */
    public int[]                  testInvlvIndces;

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
     * @param trainInvlvIndces Indices involved in training
     * @param testInvlvIndces Indices involved in testing
     */
    public MatrixFactorizationRecommender(int uc, int ic, double max, double min, int fc, double lr,
                                          double r, double m, int iter, boolean verbose,
                                          int[] trainInvlvIndces, int[] testInvlvIndces) {
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
        this.trainInvlvIndces = trainInvlvIndces;
        this.testInvlvIndces = testInvlvIndces;
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
        userDenseFeatures = new DenseMatrix(userCount, featureCount);
        itemDenseFeatures = new DenseMatrix(itemCount, featureCount);
    }

    /**
     * @see code.sma.recommender.Recommender#buildloclModel(code.sma.datastructure.MatlabFasionSparseMatrix, code.sma.datastructure.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildloclModel(MatlabFasionSparseMatrix rateMatrix,
                               MatlabFasionSparseMatrix tMatrix) {
        LoggerUtil.info(runningLogger,
            "Param: FC: " + featureCount + "\tLR: " + learningRate + "\tR: " + regularizer);
        userDenseFeatures = new DenseMatrix(userCount, featureCount);
        itemDenseFeatures = new DenseMatrix(itemCount, featureCount);
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

    /*========================================
     * Prediction
     *========================================*/

    /**
     * @see code.sma.recommender.Recommender#evaluate(code.sma.datastructure.MatlabFasionSparseMatrix)
     */
    @Override
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
        // compute the prediction by using inner product 
        double prediction = this.offset;
        if (userDenseFeatures != null && itemDenseFeatures != null) {
            if (userDenseFeatures.getRowRef(u) == null | itemDenseFeatures.getRowRef(i) == null) {
                return (maxValue + minValue) / 2.0;
            } else {
                prediction += userDenseFeatures.innerProduct(u, i, itemDenseFeatures, false);
            }
        } else {
            throw new RuntimeException("features were not initialized.");
        }

        // normalize the prediction
        if (prediction > maxValue) {
            return maxValue;
        } else if (prediction < minValue) {
            return minValue;
        } else {
            return prediction;
        }
    }

}
