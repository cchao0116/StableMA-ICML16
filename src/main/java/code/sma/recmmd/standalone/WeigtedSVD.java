package code.sma.recmmd.standalone;

import code.sma.core.Tuples;
import code.sma.dpncy.Discretizer;
import code.sma.recmmd.RecConfigEnv;
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
public class WeigtedSVD extends MFRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;
    /** parameter used in training*/
    protected double          beta0            = 0.4f;
    /** dicretizer */
    protected Discretizer     dctzr            = null;

    /*========================================
     * Constructors
     *========================================*/
    public WeigtedSVD(RecConfigEnv rce, int[] trainInvlvIndces, int[] testInvlvIndces, double b0,
                      Discretizer dctzr) {
        super(rce, trainInvlvIndces, testInvlvIndces);
        this.beta0 = b0;
        this.dctzr = dctzr;
    }

    /*========================================
     * Model Builder
     *========================================*/
    /**
     * @see edu.tongji.ml.matrix.MFRecommender#buildModel(edu.tongji.data.Tuples, edu.tongji.data.Tuples)
     */
    @Override
    public void buildloclModel(Tuples rateMatrix,
                               Tuples tMatrix) {
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
            if (runningLogger.isDebugEnabled() && round % 5 == 0) {
                double RMSE = 0.0d;
                for (int numSeq : testInvlvIndces) {
                    int u = tMatrix.getRowIndx()[numSeq];
                    int i = tMatrix.getColIndx()[numSeq];
                    double AuiReal = tMatrix.getVals()[numSeq];

                    if (userDenseFeatures.getRowRef(u) == null
                        || itemDenseFeatures.getRowRef(i) == null) {
                        continue;
                    }
                    double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, false);
                    RMSE += Math.pow(AuiReal - AuiEst, 2.0);
                }

                bestRMSE = Math.sqrt(RMSE / testInvlvIndces.length);
                LoggerUtil.info(runningLogger,
                    String.format("%d\t%.6f,[%.6f]", round, currErr, bestRMSE));
            } else if (showProgress) {
                LoggerUtil.info(runningLogger, String.format("%d\t%.6f", round, currErr));
            }
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
