package code.sma.recmmd.standalone;

import code.sma.datastructure.DenseVector;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recmmd.RecConfigEnv;

/**
 * Gradient boosted matrix approximation for collaborative filtering
 * 
 * @author Chao.Chen
 * @version $Id: GradientBoostedMA.java, v 0.1 2017年1月4日 上午10:18:35 Chao.Chen Exp $
 */
public class GradientBoostedMA extends MatrixFactorizationRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /*========================================
     * Constructors
     *========================================*/
    public GradientBoostedMA(RecConfigEnv rce) {
        super(rce);
        avgUser = (DenseVector) rce.get("AVG_USER");
        avgItem = (DenseVector) rce.get("AVG_ITEM");
    }

    /** 
     * @see code.sma.recmmd.standalone.MatrixFactorizationRecommender#buildModel(code.sma.datastructure.MatlabFasionSparseMatrix, code.sma.datastructure.MatlabFasionSparseMatrix)
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

        boolean isCollaps = false;
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter && !isCollaps) {
            double sum = 0.0;

            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int u = uIndx[numSeq];
                int i = iIndx[numSeq];

                //global model
                double AuiReal = Auis[numSeq];
                double AuiEst = userDenseFeatures.innerProduct(u, i, itemDenseFeatures, true);
                sum += lossFunction.diff(AuiReal, AuiEst);

                double deriWRTp = lossFunction.dervWRTPrdctn(AuiReal, AuiEst);
                for (int s = 0; s < featureCount; s++) {
                    double Fus = userDenseFeatures.getValue(u, s);
                    double Gis = itemDenseFeatures.getValue(i, s);

                    //global model updates
                    userDenseFeatures.setValue(u, s,
                        Fus + learningRate * (-deriWRTp * Gis - regularizer * Fus), true);
                    itemDenseFeatures.setValue(i, s,
                        Gis + learningRate * (-deriWRTp * Fus - regularizer * Gis), true);
                }
            }

            prevErr = currErr;
            currErr = Math.sqrt(sum / rateCount);

            round++;

            // Show progress:
            isCollaps = recordLoggerAndDynamicStop(round, tMatrix, currErr);
        }

    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Param: FC: " + featureCount + " LR: " + learningRate + " R: " + regularizer
               + " ALG[GBMA]";
    }
}
