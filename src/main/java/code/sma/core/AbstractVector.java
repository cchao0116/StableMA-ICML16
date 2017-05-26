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
     * Set a new value at the given index.
     * 
     * @param i The index to store new value.
     * @param value The value to store.
     */
    public abstract void setValue(int i, float value);

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The index to retrieve.
     * @return The value stored at the given index.
     */
    public abstract float getValue(int i);

    /**
     * Capacity of this vector.
     * 
     * @return The length of sparse vector
     */
    public abstract int length();

    /**
     * Inner product of two vectors.
     * 
     * @param b The vector to be inner-producted with this vector.
     * @return The inner-product value.
     */
    public double innerProduct(AbstractVector b) {
        if (this.length() != b.length()) {
            throw new RuntimeException("The dimentions of two vector are inequate");
        }

        int len = this.length();
        double sum = 0.0d;
        for (int i = 0; i < len; i++) {
            sum += this.getValue(i) * b.getValue(i);
        }
        return sum;
    }
}
