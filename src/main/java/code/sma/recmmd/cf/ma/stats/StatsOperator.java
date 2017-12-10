package code.sma.recmmd.cf.ma.stats;

import code.sma.core.AbstractVector;
import code.sma.core.impl.DenseMatrix;
import code.sma.core.impl.DenseVector;
import code.sma.recmmd.Loss;

/**
 * 1D-Tensor operator with mean, variance estimator
 * 
 * @author Chao.Chen
 * @version $Id: StatsOperator.java, v 0.1 2017年6月7日 下午4:50:52 Chao.Chen Exp $
 */
public final class StatsOperator {

    private StatsOperator() {
    }

    /**
     * get reference of row vector of DenseMatrix
     * 
     * @param factor        the dense matrix
     * @param row           row Id
     * @param acumltor      option: accumulator to record the value of factors
     * @return              the vector reference
     */
    public static DenseVector getVectorRef(DenseMatrix factor, int row, Accumulator... acumltor) {
        return getVectorRef(factor, row, RandomVector.UNIFORM, acumltor);
    }

    /**
     * get reference of row vector of DenseMatrix
     * 
     * @param factor        the dense matrix
     * @param row           row Id
     * @param rand          type of how to initialize a vector
     * @param acumltor      option: accumulator to record the value of factors
     * @return              the vector reference
     */
    public static DenseVector getVectorRef(DenseMatrix factor, int row, RandomVector rand,
                                           Accumulator... acumltor) {
        assert factor != null : "Matrix should not be null";

        int[] shape = factor.shape();
        assert row < shape[0] : String.format("%d should be less than %d", row, shape[0]);
        DenseVector vec = factor.getRowRef(row);

        if (vec == null) {
//            vec = rand.create(shape[1], 1.0 / shape[1]);
            vec = rand.create(shape[1], 0.01); // seems 0.01 generally works well
            factor.setRowRef(row, vec);
        }

        // initialize vector statistics
        if (acumltor != null) {
            for (Accumulator acr : acumltor) {
                if (acr != null) {
                    for (int n = 0; n < shape[1]; n++) {
                        acr.update(n, Math.pow(vec.floatValue(n), 2.0d));
                    }
                }
            }
        }

        return vec;
    }

    /**
     * inner-product with recording the difference between real and factored value
     * 
     * @param ufactor           user latent factors
     * @param ifactor           item latent factors
     * @param lossFunction      loss function
     * @param real              real value
     * @param acumltor          accumulator to record the difference
     * @param accId             calculator's Id in accumulator
     * @param vId               value's Id in accumulator
     * @return                  inner-product
     */
    public static double innerProduct(AbstractVector ufactor, AbstractVector ifactor,
                                      Loss lossFunction, double real, Accumulator acumltor,
                                      int accId, int vId) {
        double ip = ufactor.innerProduct(ifactor);

        if (acumltor != null) {
            acumltor.update(accId, vId, lossFunction.calcLoss(real, ip));
        }
        return ip;
    }

    /**
     * inner-product with recording the difference between real and factored value
     * 
     * @param ufactor           user latent factors
     * @param ifactor           item latent factors
     * @param lossFunction      loss function
     * @param real              real value
     * @param acumltor          Option: accumulator to record the difference
     * @return                  inner-product
     */
    public static double innerProduct(AbstractVector ufactor, AbstractVector ifactor,
                                      Loss lossFunction, double real, Accumulator... acumltor) {
        double ip = ufactor.innerProduct(ifactor);

        if (acumltor != null) {
            for (Accumulator acr : acumltor) {
                if (acr != null) {
                    acr.traverse(lossFunction.calcLoss(real, ip));
                }
            }
        }
        return ip;
    }

    public static void updateVector(AbstractVector factor, int f, double fVal,
                                    Accumulator... acumltor) {
        factor.setValue(f, fVal);

        if (acumltor != null) {
            for (Accumulator acr : acumltor) {
                if (acr != null) {
                    acr.update(f, fVal * fVal);
                }
            }
        }
    }

}
