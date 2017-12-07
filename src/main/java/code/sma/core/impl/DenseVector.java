package code.sma.core.impl;

import org.apache.commons.lang3.ArrayUtils;

import code.sma.core.AbstractVector;

/**
 * Dense Vector used to represent the features
 * 
 * @author Chao.Chen
 * @version $Id: DenseVector.java, v 0.1 2016年9月26日 下午1:44:48 Chao.Chen Exp $
 */
public class DenseVector extends AbstractVector {
    private static final long serialVersionUID = 1L;

    /** The number of columns. */
    private int               N;
    /** The vector of values*/
    private float[]           vals;

    public DenseVector(int N) {
        super();
        this.N = N;
        this.vals = new float[N];
    }

    public DenseVector(int N, float[] vals) {
        super();
        this.N = N;
        this.vals = vals;
    }

    // ======================================== 
    //          Getter/Setter
    // ========================================
    /**
     * @see code.sma.core.AbstractVector#floatValue(int)
     */
    @Override
    public float floatValue(int i) {
        if (i >= N) {
            return 0.0f;
        } else {
            return vals[i];
        }
    }

    /**
     * @see code.sma.core.AbstractVector#setValue(int, double)
     */
    @Override
    public void setValue(int i, double value) {
        if (i < N) {
            vals[i] = (float) value;
        }
    }

    /** 
     * @see code.sma.core.AbstractVector#length()
     */
    @Override
    public int length() {
        return N;
    }

    /**
     * Euclidean norm of the vector
     * 
     * @return L2-norm
     */
    public double norm() {
        return Math.sqrt(innerProduct(this));
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ArrayUtils.toString(vals);
    }

}
