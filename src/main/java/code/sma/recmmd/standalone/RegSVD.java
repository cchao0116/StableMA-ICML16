package code.sma.recmmd.standalone;

import code.sma.core.AbstractIterator;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseMatrix;
import code.sma.core.impl.Tuples;
import code.sma.recmmd.RecConfigEnv;

/**
 * This is a class implementing Regularized SVD (Singular Value Decomposition).
 * Technical detail of the algorithm can be found in
 * Arkadiusz Paterek, Improving Regularized Singular Value Decomposition Collaborative Filtering,
 * Proceedings of KDD Cup and Workshop, 2007.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public class RegSVD extends MFRecommender {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /*========================================
     * Constructors
     *========================================*/
    public RegSVD(RecConfigEnv rce) {
        super(rce);
    }

    public RegSVD(RecConfigEnv rce, DenseMatrix userDenseFeatures, DenseMatrix itemDenseFeatures) {
        super(rce, userDenseFeatures, itemDenseFeatures);
    }

    /*========================================
     * Model Builder
     *========================================*/
    /**
     * @see edu.tongji.ml.matrix.MFRecommender#buildModel(edu.tongji.data.Tuples, edu.tongji.data.Tuples)
     */
    @Override
    public void buildModel(Tuples train, Tuples test) {
        super.buildModel(train, test);

        // Gradient Descent:
        int round = 0;
        int rateCount = train.getNnz();
        double prevErr = 99999;
        double currErr = 9999;

        boolean isCollaps = false;
        AbstractIterator iDataElem = (AbstractIterator) train.iterator();
        while (Math.abs(prevErr - currErr) > 0.0001 && round < maxIter && !isCollaps) {
            double sum = 0.0;

            iDataElem.refresh();
            while (iDataElem.hasNext()) {
                DataElem e = iDataElem.next();
                short num_ifactor = e.getNum_ifacotr();

                for (int f = 0; f < num_ifactor; f++) {
                    int u = e.getIndex_user(f);
                    int i = e.getIndex_item(f);

                    double AuiReal = e.getValue_ifactor(f);
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
            }
            prevErr = currErr;
            currErr = Math.sqrt(sum / rateCount);

            round++;

            // Show progress:
            isCollaps = recordLoggerAndDynamicStop(round, test, currErr);
        }
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Param: FC: " + featureCount + " LR: " + learningRate + " R: " + regularizer
               + " ALG[RegSVD]";
    }

}
