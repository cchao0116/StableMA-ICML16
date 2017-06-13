package code.sma.recmmd;

import code.sma.recmmd.stats.Accumulator;

/**
 * regularizer
 * 
 * @author Chao.Chen
 * @version $Id: Regularizer.java, v 0.1 2017年3月9日 下午1:55:44 Chao.Chen Exp $
 */
public enum Regularizer {
                         L1, //L1-norm
                         L2, // L2-norm
                         L12; // Group-sparse norm

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
                double alpha = 1000.0 * 1000.0;
                return 1.0 / (1.0 + Math.exp(-1.0 * alpha * factrVal))
                       - 1.0 / (1.0 + Math.exp(1.0 * alpha * factrVal));
            case L2:
                return factrVal;
            case L12:
                return factrVal / accFactr.rs(accId);
            default:
                return 0.0d;
        }

    }

}
