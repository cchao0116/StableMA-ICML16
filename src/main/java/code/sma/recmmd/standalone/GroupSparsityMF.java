package code.sma.recmmd.standalone;

import java.util.Map;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.impl.UJMPDenseMatrix;
import code.sma.core.impl.UJMPDenseVector;
import code.sma.main.Configures;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Loss;
import code.sma.util.EvaluationMetrics;
import code.sma.util.LoggerUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * This is a class implementing GroupSparse Matrix Factorization (GSMF)
 * Technical detail of the algorithm can be found in
 * Ting Yuan, Recommendation by Mining Multiple User Behaviors with Group Sparsity
 * Proceedings of AAAI, 2014
 * 
 * @author Chao Chen
 * @version $Id: GSMF.java, v 0.1 Jan 28, 2016 1:05:24 PM Exp $
 */
public class GroupSparsityMF extends MFRecommender {
    /**  SerialVersionNum */
    private static final long        serialVersionUID = 1L;

    /** Number of item clusters*/
    private int                      L;
    /** User profile in low-rank matrix form. */
    private UJMPDenseMatrix          userUJMPFeatures;
    /** Item profile in low-rank matrix form. */
    private UJMPDenseMatrix          itemUJMPFeatures;
    /** The Indicator matrix indexed by user id*/
    private transient IntArrayList[] IijIndxU;
    /** The Indicator matrix indexed by item id*/
    private transient IntArrayList[] IijIndxI;

    /*========================================
     * Constructors
     *========================================*/
    public GroupSparsityMF(Configures conf, Map<String, Plugin> plugins) {
        super(conf, plugins);
        this.L = conf.getInteger("ITEM_CLUSTER_NUM_VALUE");

        runtimes.doubles.add(conf.getDouble("ALPA_VALUE"));
        runtimes.doubles.add(conf.getDouble("BETA_VALUE"));
        runtimes.doubles.add(conf.getDouble("LAMBDA_VALUE"));

        runtimes.ia_func = new short[runtimes.itemCount];

    }

    /**
     * @see code.sma.recmmd.standalone.MFRecommender#prepare_runtimes(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    protected void prepare_runtimes(AbstractMatrix train, AbstractMatrix test) {
        runtimes.itrain = (AbstractIterator) train.iterator();
        runtimes.itest = test == null ? null : (AbstractIterator) test.iterator();
        runtimes.nnz = runtimes.itrain.get_num_ifactor();

        int userCount = runtimes.userCount;
        int itemCount = runtimes.itemCount;
        int featureCount = runtimes.featureCount;

        {
            //user features
            userUJMPFeatures = new UJMPDenseMatrix(userCount, featureCount);
            for (int u = 0; u < userCount; u++) {
                for (int f = 0; f < featureCount; f++) {
                    double rdm = Math.random() / featureCount;
                    userUJMPFeatures.setValue(u, f, rdm);
                }
            }

            //item features
            itemUJMPFeatures = new UJMPDenseMatrix(featureCount, itemCount);
            for (int i = 0; i < itemCount; i++) {
                for (int f = 0; f < featureCount; f++) {
                    double rdm = Math.random() / featureCount;
                    itemUJMPFeatures.setValue(f, i, rdm);
                }
            }
        }

        // dividing rating matrix into $L$ pieces
        IijIndxU = new IntArrayList[userCount];
        IijIndxI = new IntArrayList[itemCount];
        for (int u = 0; u < userCount; u++) {
            IijIndxU[u] = new IntArrayList();
        }

        for (int i = 0; i < itemCount; i++) {
            IijIndxI[i] = new IntArrayList();
            runtimes.ia_func[i] = (short) (Math.random() * L);
        }

        AbstractIterator iDataElem = runtimes.itrain.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();

            int u = e.getIndex_user(0);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                IijIndxU[u].add(i);
                IijIndxI[i].add(u);
            }

        }
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#update_inner(code.sma.core.AbstractIterator)
     */
    @Override
    protected void update_inner(AbstractIterator iDataElem) {
        // a) update U features
        updateU(iDataElem);

        // b) update V features
        updateI(iDataElem);

        // c) training error
        update_runtimes();
    }

    protected void updateU(AbstractIterator iDataElem) {
        int userCount = runtimes.userCount;
        int featureCount = runtimes.featureCount;

        double alpha = runtimes.doubles.getDouble(0);
        double lambda = runtimes.doubles.getDouble(2);

        // the right vector term for every user
        UJMPDenseMatrix rightSideMtx = new UJMPDenseMatrix(userCount, featureCount);
        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();

            int u = e.getIndex_user(0);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                double Rui = e.getValue_ifactor(f);
                rightSideMtx.plusInRow(u, itemUJMPFeatures.getCol(i).scale(alpha * Rui));
            }
        }

        for (int i = 0; i < userCount; i++) {
            // left matrix term, which will be inversed
            UJMPDenseMatrix leftSideMtx = UJMPDenseMatrix.makeIdentity(featureCount).scale(lambda);
            for (int ratedItemIndx : IijIndxU[i]) {
                UJMPDenseVector Vj = itemUJMPFeatures.getCol(ratedItemIndx);
                leftSideMtx = leftSideMtx.plus(Vj.outerProduct(Vj).scale(alpha));
            }

            // follow Equation (7)
            UJMPDenseVector Ui = leftSideMtx.inverse().times(rightSideMtx.getRow(i));
            for (int fcIndx = 0; fcIndx < featureCount; fcIndx++) {
                userUJMPFeatures.setValue(i, fcIndx, Ui.getValue(fcIndx));
            }
        }
    }

    protected void updateI(AbstractIterator iDataElem) {
        int itemCount = runtimes.itemCount;
        int featureCount = runtimes.featureCount;

        double alpha = runtimes.doubles.getDouble(0);
        double beta = runtimes.doubles.getDouble(1);
        double lambda = runtimes.doubles.getDouble(2);

        short[] ia_func = runtimes.ia_func;

        // the right vector term for every item
        UJMPDenseMatrix rightSideMtx = new UJMPDenseMatrix(itemCount, featureCount);
        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();

            int u = e.getIndex_user(0);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                double Rui = e.getValue_ifactor(f);
                rightSideMtx.plusInRow(i, userUJMPFeatures.getRow(u).scale(Rui));
            }
        }

        // compute D^b
        UJMPDenseMatrix[] Ds = new UJMPDenseMatrix[L];
        for (int l = 0; l < L; l++) {
            Ds[l] = new UJMPDenseMatrix(featureCount, featureCount);
        }
        for (int t = 0; t < featureCount; t++) {
            double[] Dtt = new double[L];
            for (int j = 0; j < itemCount; j++) {
                Dtt[ia_func[j]] += Math.pow(itemUJMPFeatures.getValue(t, j), 2.0d);
            }

            for (int l = 0; l < L; l++) {
                Ds[l].setValue(t, t, beta / Math.sqrt(Dtt[l]));
            }
        }

        // update V
        for (int j = 0; j < itemCount; j++) {
            // left matrix term, which will be inversed
            UJMPDenseMatrix leftSideMtx = UJMPDenseMatrix.makeIdentity(featureCount)
                .scale(lambda / alpha).plus(Ds[ia_func[j]]);

            for (int usrRatedIndx : IijIndxI[j]) {
                UJMPDenseVector Vj = userUJMPFeatures.getRow(usrRatedIndx);
                leftSideMtx = leftSideMtx.plus(Vj.outerProduct(Vj));
            }

            // follow Equation (5)
            UJMPDenseVector Vj = leftSideMtx.inverse().times(rightSideMtx.getRow(j));
            for (int fcIndx = 0; fcIndx < featureCount; fcIndx++) {
                itemUJMPFeatures.setValue(fcIndx, j, Vj.getValue(fcIndx));
            }
        }
    }

    /** 
     * @see code.sma.recmmd.standalone.MFRecommender#update_runtimes()
     */
    @Override
    protected void update_runtimes() {
        Loss lossfunc = runtimes.lossFunction;

        AbstractIterator iDataElem = runtimes.itrain;
        iDataElem.refresh();
        while (iDataElem.hasNext()) {
            DataElem e = iDataElem.next();
            short num_ifactor = e.getNum_ifacotr();

            int u = e.getIndex_user(0);
            for (int f = 0; f < num_ifactor; f++) {
                int i = e.getIndex_item(f);

                double Rui = e.getValue_ifactor(f);
                runtimes.sumErr += lossfunc.diff(Rui, predict(u, i));
            }
        }

        runtimes.prevErr = runtimes.currErr;
        runtimes.currErr = Math.sqrt(runtimes.sumErr / runtimes.nnz);
        runtimes.round++;

        if (runtimes.showProgress && (runtimes.round % 5 == 0) && runtimes.itest != null) {
            EvaluationMetrics metric = new EvaluationMetrics(this);
            LoggerUtil.info(runningLogger, String.format("%d\t%.6f [%s]", runtimes.round,
                runtimes.currErr, metric.printOneLine()));
            runtimes.testErr.add(metric.getRMSE());
        } else {
            LoggerUtil.info(runningLogger,
                String.format("%d\t%.6f", runtimes.round, runtimes.currErr));
        }
    }

    /** 
     * @see MFRecommender.tongji.ml.matrix.MatrixFactorizationRecommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        double maxValue = runtimes.maxValue;
        double minValue = runtimes.minValue;

        double prediction = userUJMPFeatures.getRow(u).innerProduct(itemUJMPFeatures.getCol(i));
        return Math.max(minValue, Math.min(prediction, maxValue));
    }

    /** 
     * @see code.sma.recmmd.Recommender#toString()
     */
    @Override
    public String toString() {
        return String.format("GSMF[%d]_L[%d]_[%d_%d_%d]", runtimes.featureCount, L,
            Math.round(runtimes.doubles.getDouble(0) * 100),
            Math.round(runtimes.doubles.getDouble(1) * 100),
            Math.round(runtimes.doubles.getDouble(2) * 100));
    }

}
