package code.sma.recommender.standalone;

import java.util.HashMap;
import java.util.Map;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.depndncy.Discretizer;
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
        Map<Integer, Double> trnWs = new HashMap<Integer, Double>();
        cmpTrainParam(rateMatrix, trnWs);

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

                double err = AuiReal - AuiEst;
                sum += Math.abs(err);
                for (int s = 0; s < featureCount; s++) {
                    double Fus = userDenseFeatures.getValue(u, s);
                    double Gis = itemDenseFeatures.getValue(i, s);
                    double wts = trnWs.get(dctzr.convert(AuiReal));

                    userDenseFeatures.setValue(u, s,
                        Fus + learningRate * (err * Gis * wts - regularizer * Fus), true);
                    itemDenseFeatures.setValue(i, s,
                        Gis + learningRate * (err * Fus * wts - regularizer * Gis), true);
                }
            }

            prevErr = currErr;
            currErr = sum / rateCount;

            round++;

            // Show progress:
            LoggerUtil.info(runningLogger, round + "\t" + currErr);
        }
    }

    /**
     * Compute the parameters which would be used in the training
     * 
     * @param rateMatrix
     * @return
     */
    protected void cmpTrainParam(MatlabFasionSparseMatrix rateMatrix, Map<Integer, Double> trnWs) {
        // compute rating distributions for each rating value
        double[] Auis = rateMatrix.getVals();
        for (int numSeq : trainInvlvIndces) {
            int key = dctzr.convert(Auis[numSeq]);
            Double val = trnWs.get(key);
            trnWs.put(key, val == null ? 0.0d : val + 1);
        }
        for (Integer key : trnWs.keySet()) {
            Double val = trnWs.get(key);
            trnWs.put(key, 1 + beta0 * val / trainInvlvIndces.length);
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
