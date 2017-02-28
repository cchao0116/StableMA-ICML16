package code.sma.recmmd.ensemble;

import code.sma.datastructure.SparseMatrix;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.standalone.MatrixFactorizationRecommender;

/**
 * Ensemble-based Matrix Approximation method
 * 
 * @author Chao.Chen
 * @version $Id: EnsembleMFRecommender.java, v 0.1 2016年9月26日 下午4:22:14 Chao.Chen Exp $
 */
public abstract class EnsembleMFRecommender extends MatrixFactorizationRecommender {
    /** SerialVersionNum */
    protected static final long serialVersionUID = 1L;
    /** cumulative prediction */
    protected SparseMatrix      cumPrediction    = null;
    /** cumulative weights */
    protected SparseMatrix      cumWeight        = null;

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
    public EnsembleMFRecommender(RecConfigEnv rce) {
        super(rce);
        cumPrediction = new SparseMatrix(userCount, itemCount);
        cumWeight = new SparseMatrix(userCount, itemCount);
    }

    /**
     * return the weight of which the prediction
     * 
     * @param u the given user index
     * @param i the given item index
     * @param prediction the predicted rating
     * @return
     */
    public double ensnblWeight(int u, int i, double prediction) {
        return 1.0d;
    }

}
