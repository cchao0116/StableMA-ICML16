package code.sma.core;

import java.io.Serializable;

/**
 * the abstract vector defines the basic operations.
 * 
 * @author Chao.Chen
 * @version $Id: AbstractVector.java, v 0.1 2017年5月26日 下午2:42:29 Chao.Chen Exp $
 */
public abstract class AbstractVector implements Serializable {
    /** SerialVersionNum */
    private static final long serialVersionUID = 1L;

    /**
     * Set a new value at the given index.
     * 
     * @param i The index to store new value.
     * @param value The value to store.
     */
    public abstract void setValue(int i, double value);

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The index to retrieve.
     * @return The value stored at the given index.
     */
    public abstract float floatValue(int i);

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The index to retrieve.
     * @return The value stored at the given index.
     */
    public int intValue(int i) {
        throw new RuntimeException("This method has not been implemented in AbstractVector!");
    }

    /**
     * Capacity of this vector.
     * 
     * @return The length of sparse vector
     */
    public abstract int length();

    /**
     * The sum of the inner products.
     * 
     * @param b The vectors to be inner-producted with this vector.
     * @return The inner-product value.
     */
    public double innerProduct(AbstractVector... b) {
        assert (this != null && b != null) : "1D Tensor should not be null";
        assert b.length != 0 : "b's length shoud be non-zero";
        //        assert this.length() == b.length() : "The dimentions of two vector are inequate";

        int L = b.length;
        int N = this.length();
        double sum = 0.0d;
        for (int i = 0; i < N; i++) {
            double bs = 0.0d;
            for (int l = 0; l < L; l++) {
                bs += b[l].floatValue(i);
            }

            sum += this.floatValue(i) * bs;
        }

        return sum;
    }
}
