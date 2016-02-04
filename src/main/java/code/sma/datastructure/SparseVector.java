package code.sma.datastructure;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements sparse vector, containing empty values for most space.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public class SparseVector implements Serializable {
    /** SerialVersionNum */
    private static final long    serialVersionUID = 8002;

    /** The length (maximum number of items to be stored) of sparse vector. */
    private int                  N;
    /** Data map for <index, value> pairs. */
    private Map<Integer, Double> map;

    /*========================================
     * Constructors
     *========================================*/
    /**
     * Construct an empty sparse vector, with capacity 0.
     * Capacity can be reset with setLength method later.
     */
    public SparseVector() {
        this.N = 0;
        this.map = new HashMap<Integer, Double>(0);
    }

    /**
     * Construct a new sparse vector with size n.
     * 
     * @param n The capacity of new sparse vector.
     */
    public SparseVector(int n) {
        this.N = n;
        this.map = new HashMap<Integer, Double>(0);
    }

    /*========================================
     * Getter/Setter
     *========================================*/
    /**
     * Set a new value at the given index.
     * 
     * @param i The index to store new value.
     * @param value The value to store.
     */
    public void setValue(int i, double value) {
        if (value == 0.0)
            map.remove(i);
        else
            map.put(i, value);
    }

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The index to retrieve.
     * @return The value stored at the given index.
     */
    public double getValue(int i) {
        if (map.containsKey(i))
            return map.get(i);
        else
            return 0.0;
    }

    /**
     * Delete a value stored at the given index.
     * 
     * @param i The index to delete the value in it.
     */
    public void remove(int i) {
        if (map.containsKey(i))
            map.remove(i);
    }

    /**
     * Copy the whole sparse vector and make a clone.
     * 
     * @return A clone of the current sparse vector, containing same values.
     */
    public SparseVector copy() {
        SparseVector newVector = new SparseVector(this.N);

        for (int i : this.map.keySet()) {
            newVector.setValue(i, this.getValue(i));
        }

        return newVector;
    }

    /**
     * Get a list of existing indices.
     * This can be useful to traverse the whole vector efficiently only with existing values.
     * 
     * @return An array of integer, containing indices with valid items.
     */
    public int[] indexList() {
        if (this.itemCount() == 0)
            return null;

        int[] result = new int[this.itemCount()];
        int idx = 0;
        for (int i : this.map.keySet()) {
            result[idx] = i;
            idx++;
        }

        return result;
    }

    /**
     * Get a list of existing values in array form.
     * The order of data is compatible with the index list returned by indexList method.
     * 
     * @return An array of data values contained in the vector.
     */
    public double[] valueList() {
        if (this.itemCount() == 0)
            return null;

        double[] result = new double[this.itemCount()];
        int idx = 0;
        for (int i : this.map.keySet()) {
            result[idx] = this.getValue(i);
            idx++;
        }

        return result;
    }

    /**
     * Set a same value to every element.
     * 
     * @param value The value to assign to every element.
     */
    public void initialize(double value) {
        for (int i = 0; i < this.N; i++) {
            this.setValue(i, value);
        }
    }

    /**
     * Set same value to given indices.
     * 
     * @param index The list of indices, which will be assigned the new value.
     * @param value The new value to be assigned.
     */
    public void initialize(int[] index, double value) {
        for (int i = 0; i < index.length; i++) {
            this.setValue(index[i], value);
        }
    }

    /**
     * remove all elements 
     */
    public void clear() {
        this.map.clear();
    }

    /*========================================
     * Properties
     *========================================*/
    /**
     * Capacity of this vector.
     * 
     * @return The length of sparse vector
     */
    public int length() {
        return N;
    }

    /**
     * Actual number of items in the vector.
     * 
     * @return The number of items in the vector.
     */
    public int itemCount() {
        return map.size();
    }

    /**
     * Set a new capacity of the vector.
     * 
     * @param n The new capacity value.
     */
    public void setLength(int n) {
        this.N = n;
    }

    /*========================================
     * Unary Vector operations
     *========================================*/
    /**
     * Scalar addition operator.
     * 
     * @param alpha The scalar value to be added to the original vector.
     * @return The resulting vector, added by alpha.
     */
    public SparseVector add(double alpha) {
        SparseVector a = this;
        SparseVector c = new SparseVector(N);

        for (int i : a.map.keySet()) {
            c.setValue(i, alpha + a.getValue(i));
        }

        return c;
    }

    /**
     * Scalar subtraction operator.
     * 
     * @param alpha The scalar value to be subtracted from the original vector.
     * @return The resulting vector, subtracted by alpha.
     */
    public SparseVector sub(double alpha) {
        SparseVector a = this;
        SparseVector c = new SparseVector(N);

        for (int i : a.map.keySet()) {
            c.setValue(i, a.getValue(i) - alpha);
        }

        return c;
    }

    /**
     * Scalar multiplication operator.
     * 
     * @param alpha The scalar value to be multiplied to the original vector.
     * @return The resulting vector, multiplied by alpha.
     */
    public SparseVector scale(double alpha) {
        SparseVector a = this;
        SparseVector c = new SparseVector(N);

        for (int i : a.map.keySet()) {
            c.setValue(i, alpha * a.getValue(i));
        }

        return c;
    }

    /**
     * Scalar power operator.
     * 
     * @param alpha The scalar value to be powered to the original vector.
     * @return The resulting vector, powered by alpha.
     */
    public SparseVector power(double alpha) {
        SparseVector a = this;
        SparseVector c = new SparseVector(N);

        for (int i : a.map.keySet()) {
            c.setValue(i, Math.pow(a.getValue(i), alpha));
        }

        return c;
    }

    /**
     * Exponential of a given constant.
     * 
     * @param alpha The exponent.
     * @return The resulting exponential vector.
     */
    public SparseVector exp(double alpha) {
        SparseVector a = this;
        SparseVector c = new SparseVector(N);

        for (int i : a.map.keySet()) {
            c.setValue(i, Math.pow(alpha, a.getValue(i)));
        }

        return c;
    }

    /**
     * 2-norm of the vector.
     * 
     * @return 2-norm value of the vector.
     */
    public double norm() {
        SparseVector a = this;
        return Math.sqrt(a.innerProduct(a));
    }

    /**
     * Sum of every element in the vector.
     * 
     * @return Sum value of every element.
     */
    public double sum() {
        SparseVector a = this;

        double sum = 0.0;
        for (int i : a.map.keySet()) {
            sum += a.getValue(i);
        }

        return sum;
    }

    /**
     * The value of maximum element in the vector.
     * 
     * @return Maximum value in the vector.
     */
    public double max() {
        SparseVector a = this;

        double curr = Double.MIN_VALUE;
        for (int i : a.map.keySet()) {
            if (a.getValue(i) > curr) {
                curr = a.getValue(i);
            }
        }

        return curr;
    }

    /**
     * The value of minimum element in the vector.
     * 
     * @return Minimum value in the vector.
     */
    public double min() {
        SparseVector a = this;

        double curr = Double.MAX_VALUE;
        for (int i : a.map.keySet()) {
            if (a.getValue(i) < curr) {
                curr = a.getValue(i);
            }
        }

        return curr;
    }

    /**
     * Sum of absolute value of every element in the vector.
     * 
     * @return Sum of absolute value of every element.
     */
    public double absoluteSum() {
        SparseVector a = this;

        double sum = 0.0;
        for (int i : a.map.keySet()) {
            sum += Math.abs(a.getValue(i));
        }

        return sum;
    }

    /**
     * Average of every element. It ignores non-existing values.
     * 
     * @return The average value.
     */
    public double average() {
        SparseVector a = this;

        return a.sum() / (double) a.itemCount();
    }

    /**
     * Variance of every element. It ignores non-existing values.
     * 
     * @return The variance value.
     */
    public double variance() {
        double avg = this.average();
        double sum = 0.0;

        for (int i : this.map.keySet()) {
            sum += Math.pow(this.getValue(i) - avg, 2);
        }

        return sum / this.itemCount();
    }

    /**
     * Standard Deviation of every element. It ignores non-existing values.
     * 
     * @return The standard deviation value.
     */
    public double stdev() {
        return Math.sqrt(this.variance());
    }

    /*========================================
     * Binary Vector operations
     *========================================*/
    /**
     * Vector sum (a + b)
     * 
     * @param b The vector to be added to this vector.
     * @return The resulting vector after summation.
     */
    public SparseVector plus(SparseVector b) {
        SparseVector a = this;
        if (a.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        SparseVector c = new SparseVector(N);
        for (int i : a.map.keySet())
            c.setValue(i, a.getValue(i)); // c = a
        for (int i : b.map.keySet())
            c.setValue(i, b.getValue(i) + c.getValue(i)); // c = c + b

        return c;
    }

    /**
     * Vector subtraction (a - b)
     * 
     * @param b The vector to be subtracted from this vector.
     * @return The resulting vector after subtraction.
     */
    public SparseVector minus(SparseVector b) {
        SparseVector a = this;
        if (a.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        SparseVector c = new SparseVector(N);
        for (int i : a.map.keySet())
            c.setValue(i, a.getValue(i)); // c = a
        for (int i : b.map.keySet())
            c.setValue(i, c.getValue(i) - b.getValue(i)); // c = c - b

        return c;
    }

    /**
     * Vector subtraction (a - b), for only existing values.
     * The resulting vector can have a non-zero value only if both vectors have a value at the index.
     * 
     * @param b The vector to be subtracted from this vector.
     * @return The resulting vector after subtraction.
     */
    public SparseVector commonMinus(SparseVector b) {
        SparseVector a = this;
        //		if (a.N != b.N)
        //			throw new RuntimeException("Vector lengths disagree");

        SparseVector c = new SparseVector(N);
        if (a.itemCount() <= b.itemCount()) {
            for (int i : a.map.keySet()) {
                if (b.map.containsKey(i))
                    c.setValue(i, a.getValue(i) - b.getValue(i));
            }
        } else {
            for (int i : b.map.keySet()) {
                if (a.map.containsKey(i))
                    c.setValue(i, a.getValue(i) - b.getValue(i));
            }
        }

        return c;
    }

    /**
     * Inner product of two vectors.
     * 
     * @param b The vector to be inner-producted with this vector.
     * @return The inner-product value.
     */
    public double innerProduct(SparseVector b) {
        SparseVector a = this;
        double sum = 0.0;

        if (a.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        // iterate over the vector with the fewer items
        if (a.itemCount() <= b.itemCount()) {
            for (int i : a.map.keySet()) {
                if (b.map.containsKey(i))
                    sum += a.getValue(i) * b.getValue(i);
            }
        } else {
            for (int i : b.map.keySet()) {
                if (a.map.containsKey(i))
                    sum += a.getValue(i) * b.getValue(i);
            }
        }

        return sum;
    }

    /**
     * Outer product of two vectors.
     * 
     * @param b The vector to be outer-producted with this vector.
     * @return The resulting outer-product matrix. 
     */
    public SparseMatrix outerProduct(SparseVector b) {
        SparseMatrix A = new SparseMatrix(this.N, b.N);

        for (int i = 0; i < this.N; i++) {
            for (int j = 0; j < b.N; j++) {
                A.setValue(i, j, this.getValue(i) * b.getValue(j));
            }
        }

        return A;
    }

    /*========================================
     * Binary Vector operations (partial)
     *========================================*/
    /**
     * Vector sum (a + b) for indices only in the given indices.
     * 
     * @param b The vector to be added to this vector.
     * @param indexList The list of indices to be applied summation.
     * @return The resulting vector after summation.
     */
    public SparseVector partPlus(SparseVector b, int[] indexList) {
        if (indexList == null)
            return this;

        if (this.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        for (int i : indexList)
            this.setValue(i, this.getValue(i) + b.getValue(i)); // c = c + b

        return this;
    }

    /**
     * Vector subtraction (a - b) for indices only in the given indices.
     * 
     * @param b The vector to be subtracted from this vector.
     * @param indexList The list of indices to be applied subtraction.
     * @return The resulting vector after subtraction.
     */
    public SparseVector partMinus(SparseVector b, int[] indexList) {
        if (indexList == null)
            return this;

        if (this.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        for (int i : indexList)
            this.setValue(i, this.getValue(i) - b.getValue(i)); // c = c - b

        return this;
    }

    /**
     * Inner-product for indices only in the given indices.
     * 
     * @param b The vector to be inner-producted with this vector.
     * @param indexList The list of indices to be applied inner-product.
     * @return The inner-product value.
     */
    public double partInnerProduct(SparseVector b, int[] indexList) {
        double sum = 0.0;

        if (indexList != null) {
            for (int i : indexList) {
                sum += this.getValue(i) * b.getValue(i);
            }
        }

        return sum;
    }

    /**
     * Outer-product for indices only in the given indices.
     * 
     * @param b The vector to be outer-producted with this vector.
     * @param indexList The list of indices to be applied outer-product.
     * @return The outer-product value.
     */
    public SparseMatrix partOuterProduct(SparseVector b, int[] indexList) {
        if (indexList == null)
            return null;

        SparseMatrix A = new SparseMatrix(b.length(), b.length());

        for (int i : indexList) {
            for (int j : indexList) {
                A.setValue(i, j, this.getValue(i) * b.getValue(j));
            }
        }

        return A;
    }

    /**
     * Convert the vector to a printable string.
     * 
     * @return The resulted string in the form of "(1: 5.0) (2: 4.5)"
     */
    @Override
    public String toString() {
        String s = "";
        for (int i : this.map.keySet()) {
            s += "(" + i + ": " + map.get(i) + ") ";
        }
        return s;
    }
}
