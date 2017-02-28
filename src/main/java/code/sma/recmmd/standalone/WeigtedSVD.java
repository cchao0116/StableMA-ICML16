package code.sma.recmmd.standalone;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.dpncy.Discretizer;
import code.sma.util.LoggerUtil;

/**
 * This is a class implementing WSVD (Weighted Matrix Approximation).
 * Technical detail of the algorithm can be found in
 * Chao Chen, WEMAREC: Accurate and Scalable Recommendation through Weighted and Ensemble Matrix Approximation,
 * Proceedings of SIGIR, 2015.
 * 
 * @author Chao Chen
 * @version $Id: WeigtedRSVD.java, v 0.1 2014-10-19 上午11:20:27 chench Exp $
 */
public class WeigtedSVD extends MatrixFactorizationRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;
    /** parameter used in training*/
    protected double          beta0            = 0.4f;
    /** dicretizer */
    protected Discretizer     dctzr            = null;

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
     * @param trainInvlvIndces Indices involved in training
     * @param testInvlvIndces Indices involved in testing
     * @param b0 The parameter controlling the contribution of every training data.
     * @param dctzr The dicretizer to convert continuous data
     */
    public WeigtedSVD(int uc, int ic, double max, double min, int fc, double lr, double r, double m,
                      int iter, int[] trainInvlvIndces, int[] testInvlvIndces, double b0,
                      Discretizer dctzr) {
        super(uc, ic, max, min, fc, lr, r, m, iter, true, trainInvlvIndces, testInvlvIndces);
        this.beta0 = b0;
        this.dctzr = dctzr;
    }

    /*========================================
     * Model Builder
     *========================================*/
    /**
     * @see edu.tongji.ml.matrix.MatrixFactorizationRecommender#buildModel(edu.tongji.data.MatlabFasionSparseMatrix, edu.tongji.data.MatlabFasionSparseMatrix)
     */
    @Override
    public void buildloclModel(MatlabFasionSparseMatrix rateMatrix,
                               MatlabFasionSparseMatrix tMatrix) {
        super.buildloclModel(rateMatrix, null);

        // Compute dependencies
        double[] tnWs = dctzr.cmpTrainWs(rateMatrix, trainInvlvIndces);

        // Gradient Descent:
        int round = 0;
        int rateCount = rateMatrix.getNnz();
        double prevErr = 99999;
        double currErr = 9999;

        int[] uIndx = rateMatrix.getRowIndx();
        int[] iIndx = rateMatrix.getColIndx();
        double[] Auis = rateMatrix.getVals();
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter) {
            double sum = 0.0;

            for (int numSeq : trainInvlvIndces) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];
                double AuiReal = Auis[numSeq];
                double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                sum += lossFunction.diff(AuiReal, AuiEst);

                double deriWRTp = lossFunction.dervWRTPrdctn(AuiReal, AuiEst);
                for (int s = 0; s < featureCount; s++) {
                    double Fus = userDenseFeatures.getValue(u, s);
                    double Gis = itemDenseFeatures.getValue(i, s);
                    double wts = 1 + beta0 * tnWs[dctzr.convert(AuiReal)];

                    userDenseFeatures.setValue(u, s,
                        Fus + learningRate * (-deriWRTp * Gis * wts - regularizer * Fus), true);
                    itemDenseFeatures.setValue(i, s,
                        Gis + learningRate * (-deriWRTp * Fus * wts - regularizer * Gis), true);
                }
            }

            prevErr = currErr;
            currErr = Math.sqrt(sum / rateCount);
            round++;

            // Show progress:
            LoggerUtil.info(runningLogger, round + "\t" + currErr);
        }
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Param: FC: " + featureCount + " LR: " + learningRate + " R: " + regularizer
               + " ALG[WSVD][" + String.format("%.0f", beta0 * 100) + "%]";
    }

}
