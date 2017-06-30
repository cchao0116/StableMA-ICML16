package code.sma.recmmd;

import code.sma.recmmd.ma.stats.Accumulator;

/**
 * regularizer
 * 
 * @author Chao.Chen
 * @version $Id: Regularizer.java, v 0.1 2017年3月9日 下午1:55:44 Chao.Chen Exp $
 */
public enum Regularizer {
                         L1, //L1-norm
                         L2, // L2-norm
                         L12, // Group-sparse norm
                         ELASTIC_NET; // Elastic net
    // regularization hyper-parameter
    private double reg_lambda;

    public Regularizer lambda(double reg_lambda) {
        this.reg_lambda = reg_lambda;
        return this;
    }

    /**
     * compute the regularization of the given parameter
     * 
     * @param accFactr      the accumulator of the latent factor
     * @param accId         the index within the accumulator
     * @param factrVal      the value of the latent factor
     * @return
     */
    public double reg(Accumulator accFactr, int accId, double factrVal) {
        switch (this) {
            case L1:
                return smoothed_L1(factrVal, 1000.0 * 1000.0);
            case L2:
                return factrVal;
            case L12:
                return factrVal / accFactr.rs(accId);
            default:
                return 0.0d;
        }
    }

    /**
     * compute the weight of a leaf w.r.t specific regularizer
     * 
     * @param sum_grad      sum of gradient
     * @param sum_hess      sum of hessian
     * @return              the leaf weight
     */
    public double calcWeight(double sum_grad, double sum_hess) {
        if (sum_hess < 2.0) {
            return 0.0;
        } else {
            assert sum_hess > 1e-5 : "second order derivative too low";
            switch (this) {
                case L1:
                    return -threshold_L1(sum_grad, reg_lambda) / sum_hess;
                case L2:
                    return -sum_grad / (sum_hess + reg_lambda);
                case ELASTIC_NET: // elastic net
                    return -threshold_L1(sum_grad, 0.5 * reg_lambda)
                           / (sum_hess + 0.5 * reg_lambda);
                default:
                    return -sum_grad / sum_hess;
            }
        }
    }

    protected double smoothed_L1(double fValue, double alpha) {
        return 1.0 / (1.0 + Math.exp(-1.0 * alpha * fValue))
               - 1.0 / (1.0 + Math.exp(1.0 * alpha * fValue));
    }

    protected double threshold_L1(double w, double lambda) {
        if (w > +lambda)
            return w - lambda;
        if (w < -lambda)
            return w + lambda;
        return 0.0;
    }

}
