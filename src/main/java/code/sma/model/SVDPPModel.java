package code.sma.model;

import code.sma.core.AbstractVector;
import code.sma.core.DataElem;
import code.sma.core.impl.DenseMatrix;
import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;
import code.sma.recmmd.cf.ma.stats.StatsOperator;
import code.sma.util.LoggerUtil;

/**
 * SVD++ model
 * 
 * @author Chao Chen
 * @version $Id: SVDPPModel.java, v 1.0 Dec 7, 2017 1:18:21 PM$
 */
public class SVDPPModel extends FactorModel {
    private static final long serialVersionUID = 1L;

    /** User implicit feedback in low-rank matrix form. */
    public DenseMatrix        yfactors;

    public SVDPPModel(Configures conf) {
        super(conf);

        int itemCount = conf.getInteger("ITEM_COUNT_VALUE");
        int featureCount = conf.getInteger("FEATURE_COUNT_VALUE");

        this.yfactors = new DenseMatrix(itemCount, featureCount);
    }

    /**
     * calculate the implicit factors based on user's rated or viewed items
     * 
     * @param ref_yfactors  reference for user's implicit factors
     * @return  implicit factor
     */
    public DenseVector calcImplicFeature(DenseVector[] ref_yfactors) {
        assert ref_yfactors != null
               && ref_yfactors.length != 0 : "yfactors cannot be null or empty.";
        int L = ref_yfactors.length;
        int N = ref_yfactors[0].length();

        // aggregate
        float[] sums = new float[N];
        for (int l = 0; l < L; l++) {
            for (int f = 0; f < N; f++) {
                sums[f] += ref_yfactors[l].floatValue(f);
            }
        }

        // normalized somehow
        float scale = (float) (1.0f / Math.sqrt(L));
        for (int f = 0; f < N; f++) {
            sums[f] *= scale;
        }
        return new DenseVector(N, sums);
    }

    /** 
     * @see code.sma.model.AbstractModel#predict(code.sma.core.DataElem)
     */
    @Override
    public double[] predict(DataElem e) {
        assert (ufactors != null && ifactors != null
                && yfactors != null) : "Feature matrix cannot be null";

        short num_ifactor = e.getNum_ifacotr();
        double[] preds = new double[num_ifactor];

        DenseVector[] ref_yfactors = new DenseVector[num_ifactor];
        for (int f = 0; f < num_ifactor; f++) {
            int i = e.getIndex_item(f);
            ref_yfactors[f] = StatsOperator.getVectorRef(yfactors, i);
        }

        int u = e.getIndex_user(0);
        AbstractVector ufs = ufactors.getRowRef(u);
        AbstractVector yfs = calcImplicFeature(ref_yfactors);

        for (int p = 0; p < num_ifactor; p++) {
            int i = e.getIndex_item(p);
            AbstractVector ifs = ifactors.getRowRef(i);

            if (ufs == null || ifs == null || yfs == null) {
                LoggerUtil.debug(runningLogger,
                    String.format("null latent factors for (%d,%d)-entry", u, i));
                preds[p] = defaultValue;
            } else {
                double prediction = base + ubias.floatValue(u) + ibias.floatValue(i)
                                    + ifs.innerProduct(ufs, yfs);
                preds[p] = Math.max(minValue, Math.min(prediction, maxValue));
            }
        }
        return preds;
    }
}
