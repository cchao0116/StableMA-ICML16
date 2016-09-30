package code.sma.recommender.rank;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recommender.RecConfigEnv;
import code.sma.recommender.standalone.MatrixFactorizationRecommender;
import code.sma.util.EvaluationMetrics;

/**
 * Matrix Factorization Methods in application of Top-N Problem
 * 
 * @author Chao.Chen
 * @version $Id: RankBasedMFRecommender.java, v 0.1 2016年9月30日 上午10:49:35 Chao.Chen Exp $
 */
public class RankBasedMFRecommender extends MatrixFactorizationRecommender {
    /** Top-N recommendations*/
    protected int             topN;
    /**  serialVersionUID */
    private static final long serialVersionUID = 1L;

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
    public RankBasedMFRecommender(int uc, int ic, double max, double min, int fc, double lr,
                                  double r, double m, int iter, boolean verbose, RecConfigEnv rce) {
        super(uc, ic, max, min, fc, lr, r, m, iter, verbose, rce);
        topN = ((Double) rce.get("TOP_N_VALUE")).intValue();
    }

    /** 
     * @see code.sma.recommender.standalone.MatrixFactorizationRecommender#evaluate(code.sma.datastructure.MatlabFasionSparseMatrix)
     */
    @Override
    public EvaluationMetrics evaluate(MatlabFasionSparseMatrix testMatrix) {
        return new EvaluationMetrics(this, testMatrix, topN);
    }

    /** 
     * @see code.sma.recommender.standalone.MatrixFactorizationRecommender#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        return offset + userDenseFeatures.innerProduct(u, i, itemDenseFeatures, false);
    }

}
