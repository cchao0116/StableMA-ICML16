package code.sma.datastructure;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.primitives.Doubles;

/**
 * Dense Vector used to represent the features
 * 
 * @author Chao.Chen
 * @version $Id: DenseVector.java, v 0.1 2016年9月26日 下午1:44:48 Chao.Chen Exp $
 */
public class DenseVector implements Serializable, Iterable<Double> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    /** The number of columns. */
    private int               N;
    /** The vector of values*/
    private double[]          vals;

    /**
     * Construction
     * 
     * @param N             the length of the vector
     */
    public DenseVector(int N) {
        super();
        this.N = N;
        this.vals = new double[N];
    }

    /**
     * Construction
     * 
     * @param N             the length of the vector
     * @param needRanInit   need to randomly initialize the vector
     */
    public DenseVector(int N, boolean needRanInit) {
        super();
        this.N = N;
        this.vals = new double[N];

        if (needRanInit) {
            for (int n = 0; n < N; n++) {
                vals[n] = Math.random() / N;
            }
        }
    }

    // ======================================== 
    //          Getter/Setter
    // ========================================

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The row index to retrieve.
     * @return The value stored at the given index.
     */
    public double getValue(int i) {
        if (i >= N) {
            return 0.0d;
        } else {
            return vals[i];
        }
    }

    /**
     * Set a new value at the given index.
     * 
     * @param i            The row index to store new value.
     * @param value        The value to store.
     */
    public void setValue(int i, double value) {
        if (i < N) {
            vals[i] = value;
        }
    }

    /**
     * the length of the vector
     * 
     * @return
     */
    public int len() {
        return N;
    }

    /**
     * Inner product of two vectors.
     * 
     * @param u                 the index of rows in this object 
     * @param i                 the index of rows in tDenseFeature
     * @param tDenseMatrix     the transposed DenseMatrix
     * @return
     */
    public double innerProduct(DenseVector tDenseVector) {
        if (N != tDenseVector.N) {
            throw new RuntimeException("The dimentions of two vector are inequate");
        }

        double sum = 0.0d;
        for (int n = 0; n < N; n++) {
            sum += vals[n] * tDenseVector.getValue(n);
        }
        return sum;
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
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Double> iterator() {
        return Doubles.asList(vals).iterator();
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ArrayUtils.toString(vals);
    }

}
