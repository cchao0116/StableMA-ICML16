package code.sma.core.impl;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;

/**
 * This class implements dense vector with array-based implementation.
 * Note that we use UJMP package (http://www.ujmp.org) to implement this class.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public class UJMPDenseVector {
    /** The UJMP matrix to store data. */
    private Matrix map;
    /** The length (maximum number of items to be stored) of sparse vector. */
    private int    N;

    /*========================================
     * Constructors
     *========================================*/
    /**
     * Construct an empty dense vector, with capacity 0.
     * Capacity can be reset with setLength method later.
     */
    public UJMPDenseVector() {
        this.N = 0;
        this.map = org.ujmp.core.DenseMatrix.Factory.emptyMatrix();
        ;
    }

    /**
     * Construct a new dense vector with size n.
     * 
     * @param n The capacity of new dense vector.
     */
    public UJMPDenseVector(int n) {
        this.N = n;
        this.map = org.ujmp.core.DenseMatrix.Factory.zeros(n, 1);
        ;
    }

    /**
     * Construct a new dense vector, having same data with the given UJMP matrix.
     * 
     * @param m An UJMP matrix.
     */
    public UJMPDenseVector(Matrix m) {
        this.N = (int) (m.getSize())[0];
        this.map = m;
    }

    /*========================================
     * Getter/Setter
     *========================================*/
    /**
     * Get an UJMP matrix.
     * Note that the Matrix class is implemented like a vector, by UJMP.
     * 
     * @return UJMP matrix.
     */
    public Matrix getVector() {
        return map;
    }

    /**
     * Set a new value at the given index.
     * 
     * @param i The index to store new value.
     * @param value The value to store.
     * @throws ArrayIndexOutOfBoundsException when the index is out of range.
     */
    public void setValue(int i, double value) {
        if (i < 0 || i >= this.N) {
            throw new ArrayIndexOutOfBoundsException("Out of index range: " + i);
        }

        map.setAsDouble(value, i, 0);
    }

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The index to retrieve.
     * @return The value stored at the given index.
     * @throws ArrayIndexOutOfBoundsException when the index is out of range.
     */
    public double getValue(int i) {
        if (i < 0 || i >= this.N) {
            throw new ArrayIndexOutOfBoundsException("Out of index range: " + i);
        }

        return map.getAsDouble(i, 0);
    }

    /**
     * Delete a value stored at the given index.
     * 
     * @param i The index to delete the value in it.
     */
    public void remove(int i) {
        map.setAsDouble(0.0, i);
    }

    /**
     * Copy the whole dense vector and make a clone.
     * 
     * @return A clone of the current dense vector, containing same values.
     */
    public UJMPDenseVector copy() {
        UJMPDenseVector newVector = new UJMPDenseVector(N);
        for (int i : this.indexList()) {
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

        int idx = 0;
        int[] result = new int[this.itemCount()];
        for (long[] c : map.availableCoordinates()) {
            result[idx] = (int) c[0];
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
        for (int i = 0; i < N; i++) {
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
     * Convert the vector into the sparse vector.
     * 
     * @return The sparse vector with the same data.
     */
    public SparseVector toSparseVector() {
        SparseVector v = new SparseVector(this.N);

        for (int i = 0; i < this.N; i++) {
            double value = this.getValue(i);

            if (value != 0.0) {
                v.setValue(i, value);
            }
        }

        return v;
    }

    /**
     * Convert the vector into a sparse vector, but only with the selected indices.
     * 
     * @param indexList The list of indices converting to sparse vector.
     * @return The sparse vector with the same data, with given indices. 
     */
    public UJMPDenseVector toDenseSubset(int[] indexList) {
        if (indexList == null || indexList.length == 0)
            return null;

        UJMPDenseVector m = new UJMPDenseVector(indexList.length);

        int x = 0;
        for (int i : indexList) {
            m.setValue(x, this.getValue(i));
            x++;
        }

        return m;
    }

    /*========================================
     * Properties
     *========================================*/
    /**
     * Capacity of this vector
     * 
     * @return The length of dense vector
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
        return (int) map.getValueCount();
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
    public UJMPDenseVector add(double alpha) {
        map = map.plus(alpha);
        return this;
    }

    /**
     * Scalar subtraction operator.
     * 
     * @param alpha The scalar value to be subtracted from the original vector.
     * @return The resulting vector, subtracted by alpha.
     */
    public UJMPDenseVector sub(double alpha) {
        map = map.minus(alpha);
        return this;
    }

    /**
     * Scalar multiplication operator.
     * 
     * @param alpha The scalar value to be multiplied to the original vector.
     * @return The resulting vector, multiplied by alpha.
     */
    public UJMPDenseVector scale(double alpha) {
        map = map.times(alpha);
        return this;
    }

    /**
     * Scalar power operator.
     * 
     * @param alpha The scalar value to be powered to the original vector.
     * @return The resulting vector, powered by alpha.
     */
    public UJMPDenseVector power(double alpha) {
        for (int i : this.indexList()) {
            this.setValue(i, Math.pow(this.getValue(i), alpha));
        }

        return this;
    }

    /**
     * Exponential of a given constant.
     * 
     * @param alpha The exponent.
     * @return The resulting exponential vector.
     */
    public UJMPDenseVector exp(double alpha) {
        for (int i : this.indexList()) {
            this.setValue(i, Math.pow(alpha, this.getValue(i)));
        }

        return this;
    }

    /**
     * 2-norm of the vector.
     * 
     * @return 2-norm value of the vector.
     */
    public double norm() {
        return Math.sqrt(this.innerProduct(this));
    }

    /**
     * Sum of every element in the vector.
     * 
     * @return Sum value of every element.
     */
    public double sum() {
        Matrix colSum = map.sum(Ret.LINK, 0, true);
        return colSum.getAsDouble(0, 0);
    }

    /**
     * Sum of absolute value of every element in the vector.
     * 
     * @return Sum of absolute value of every element.
     */
    public double absoluteSum() {
        double sum = 0.0;
        for (int i : this.indexList()) {
            sum += Math.abs(this.getValue(i));
        }

        return sum;
    }

    /**
     * Average of every element. It ignores non-existing values.
     * 
     * @return The average value.
     */
    public double average() {
        return this.sum() / (double) this.itemCount();
    }

    /**
     * Variance of every element. It ignores non-existing values.
     * 
     * @return The variance value.
     */
    public double variance() {
        double avg = this.average();
        double sum = 0.0;

        for (int i : this.indexList()) {
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
     * @throws RuntimeException when vector lengths disagree.
     */
    public UJMPDenseVector plus(UJMPDenseVector b) {
        UJMPDenseVector a = this;
        if (a.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        Matrix v1 = map;
        Matrix v2 = b.getVector();

        return new UJMPDenseVector(v1.plus(v2));
    }

    /**
     * Vector subtraction (a - b)
     * 
     * @param b The vector to be subtracted from this vector.
     * @return The resulting vector after subtraction.
     * @throws RuntimeException when vector lengths disagree.
     */
    public UJMPDenseVector minus(UJMPDenseVector b) {
        UJMPDenseVector a = this;
        if (a.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        Matrix v1 = map;
        Matrix v2 = b.getVector();

        return new UJMPDenseVector(v1.minus(v2));
    }

    /**
     * Vector subtraction (a - b), for only existing values.
     * The resulting vector can have a non-zero value only if both vectors have a value at the index.
     * 
     * @param b The vector to be subtracted from this vector.
     * @return The resulting vector after subtraction.
     */
    public UJMPDenseVector commonMinus(UJMPDenseVector b) {
        UJMPDenseVector a = this;
        //		if (a.N != b.N)
        //			throw new RuntimeException("Vector lengths disagree");

        UJMPDenseVector c = new UJMPDenseVector(N);
        if (a.itemCount() <= b.itemCount()) {
            for (int i : a.indexList()) {
                if (b.map.containsDouble(i))
                    c.setValue(i, a.getValue(i) - b.getValue(i));
            }
        } else {
            for (int i : b.indexList()) {
                if (a.map.containsDouble(i))
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
    public double innerProduct(UJMPDenseVector b) {
        if (this.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        Matrix v1 = map;
        Matrix v2 = b.getVector();

        Matrix times = v1.times(v2);
        double sum = 0.0;
        for (long[] i : times.availableCoordinates()) {
            sum += times.getAsDouble(i);
        }

        return sum;
    }

    /**
     * Outer product of two vectors.
     * 
     * @param b The vector to be outer-producted with this vector.
     * @return The resulting outer-product matrix. 
     */
    public UJMPDenseMatrix outerProduct(UJMPDenseVector b) {
        UJMPDenseMatrix A = new UJMPDenseMatrix(this.N, b.N);

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
     * @throws RuntimeException when vector lengths disagree.
     * @return The resulting vector after summation.
     */
    public UJMPDenseVector partPlus(UJMPDenseVector b, int[] indexList) {
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
     * @throws RuntimeException when vector lengths disagree.
     * @return The resulting vector after subtraction.
     */
    public UJMPDenseVector partMinus(UJMPDenseVector b, int[] indexList) {
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
    public double partInnerProduct(UJMPDenseVector b, int[] indexList) {
        double sum = 0.0;

        if (indexList != null) {
            for (int i : indexList) {
                sum += this.getValue(i) * b.getValue(i);
            }
        }

        return sum;
    }

    /*========================================
     * Binary Vector operations without creating new Variable
     *========================================*/
    /**
     * Vector sum (a + b)
     * 
     * @param b The vector to be added to this vector.
     * @return The resulting vector after summation.
     * @throws RuntimeException when vector lengths disagree.
     */
    public void plusW(UJMPDenseVector b) {
        UJMPDenseVector a = this;
        if (a.N != b.N)
            throw new RuntimeException("Vector lengths disagree");

        for (int n = 0; n < N; n++) {
            a.setValue(n, a.getValue(n) + b.getValue(n));
        }
    }

    /**
     * Convert the vector to a printable string.
     * 
     * @return The resulted string in the form of "(1: 5.0) (2: 4.5)"
     */
    @Override
    public String toString() {
        String s = "";
        for (int i : this.indexList()) {
            s += "(" + i + ": " + map.getAsDouble(i, 0) + ") ";
        }
        return s;
    }
}
