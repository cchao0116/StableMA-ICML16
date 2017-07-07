package code.sma.recmmd;

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
    public double calcReg(double factrVal, double... param) {
        switch (this) {
            case L1:
                return smoothed_L1(factrVal, reg_lambda, 1000.0 * 1000.0);
            case L2:
                return (1 - reg_lambda) * factrVal;
            case L12:
                assert param.length >= 1 : "L12 of each row is required.";
                return reg_lambda * factrVal * factrVal / param[0];
            default:
                return factrVal;
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

    public double calcCost(double sum_grad, double sum_hess) {
        switch (this) {
            case L1:
                return Math.pow(threshold_L1(sum_grad, reg_lambda), 2.0d) / sum_hess;
            case L2:
                return Math.pow(sum_grad, 2.0f) / (sum_hess + reg_lambda);
            case ELASTIC_NET: // elastic net
                return Math.pow(threshold_L1(sum_grad, 0.5 * reg_lambda), 2.0f)
                       / (sum_hess + 0.5 * reg_lambda);
            default:
                return Math.pow(sum_grad, 2.0d) / sum_hess;
        }
    }

    protected double smoothed_L1(double fValue, double lambda, double alpha) {
        return lambda * (1.0 / (1.0 + Math.exp(-1.0 * alpha * fValue))
                         - 1.0 / (1.0 + Math.exp(1.0 * alpha * fValue)));
    }

    protected double threshold_L1(double w, double lambda) {
        if (w > +lambda)
            return w - lambda;
        if (w < -lambda)
            return w + lambda;
        return 0.0;
    }

}
