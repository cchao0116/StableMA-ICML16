package code.sma.recommender.standalone;

import java.io.Serializable;

import org.apache.log4j.Logger;

import code.sma.datastructure.DenseMatrix;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recommender.Loss;
import code.sma.recommender.RecConfigEnv;
import code.sma.recommender.Recommender;
import code.sma.util.EvaluationMetrics;
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
    /** the loss funciton to measure the distance between real value and approximated value*/
    protected Loss                lossFunction;

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
     * @param rce The recommender's specific parameters
     */
    public MatrixFactorizationRecommender(int uc, int ic, double max, double min, int fc, double lr,
                                          double r, double m, int iter, boolean verbose,
                                          RecConfigEnv rce) {
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
        lossFunction = Loss.valueOf((String) rce.get("LOSS_FUNCTION"));
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
            EvaluationMetrics metric = evaluate(tMatrix);
            LoggerUtil.info(runningLogger,
                String.format("%d\t%.6f [%s]", round, currErr, metric.printOneLine()));
            if (bestRMSE >= metric.getRMSE()) {
                bestRMSE = metric.getRMSE();
            } else {
                return true;
            }
        } else {
            LoggerUtil.info(runningLogger, String.format("%d\t%.6f", round, currErr));
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
    public EvaluationMetrics evaluate(MatlabFasionSparseMatrix testMatrix) {
        return new EvaluationMetrics(this, testMatrix);
    }

    /**
     * @see edu.tongji.ml.Recommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        // compute the prediction by using inner product 
        double prediction = this.offset;
        if (userDenseFeatures != null && itemDenseFeatures != null) {
            prediction += userDenseFeatures.innerProduct(u, i, itemDenseFeatures, false);
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
