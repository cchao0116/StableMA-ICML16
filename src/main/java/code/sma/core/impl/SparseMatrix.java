package code.sma.core.impl;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.util.StringUtil;

/**
 * This class implements sparse matrix, containing empty values for most space.
 * 
 * @author Joonseok Lee
 * @since 2012. 4. 20
 * @version 1.1
 */
public class SparseMatrix extends AbstractMatrix implements Serializable {
    /** SerialVersionNum */
    private static final long serialVersionUID = 8003;

    /** The number of rows. */
    private int               M;
    /** The number of columns. */
    private int               N;
    /** The array of row references. */
    private SparseVector[]    rows;
    /** The array of column references. */
    private SparseVector[]    cols;

    /*========================================
     * Constructors
     *========================================*/
    /**
     * Construct an empty sparse matrix, with a given size.
     * 
     * @param m The number of rows.
     * @param n The number of columns.
     */
    public SparseMatrix(int m, int n) {
        this.M = m;
        this.N = n;
        rows = new SparseVector[M];
        cols = new SparseVector[N];

        for (int i = 0; i < M; i++) {
            rows[i] = new SparseVector(N);
        }
        for (int j = 0; j < N; j++) {
            cols[j] = new SparseVector(M);
        }
    }

    /**
     * Construct an empty sparse matrix, with data copied from another sparse matrix.
     * 
     * @param sm The matrix having data being copied.
     */
    public SparseMatrix(SparseMatrix sm) {
        this.M = sm.M;
        this.N = sm.N;
        rows = new SparseVector[M];
        cols = new SparseVector[N];

        for (int i = 0; i < M; i++) {
            rows[i] = sm.getRow(i);
        }
        for (int j = 0; j < N; j++) {
            cols[j] = sm.getCol(j);
        }
    }

    /** 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<DataElem> iterator() {
        throw new RuntimeException("This method has not been implemented in SparseMatrix!");
    }

    /** 
     * @see code.sma.core.AbstractMatrix#loadNext(java.lang.String)
     */
    @Override
    public void loadNext(String line) {
        assert StringUtil.isNotBlank(line) : "Line must not be blank";

        Scanner scanner = new Scanner(line);
        scanner.skip("^(\\d+\\s+){1}");
        int num_global = scanner.nextInt();
        this.num_global += num_global;

        int num_ufactor = scanner.nextInt();
        this.num_ufactor += num_ufactor;

        this.num_ifactor += scanner.nextInt();

        scanner.useDelimiter(":+|\\s+");

        // skip global features
        for (int n = 0; n < num_global; n++) {
            scanner.nextInt();
            scanner.nextFloat();
        }

        // skip user factors
        int uId = 0;
        for (int n = 0; n < num_ufactor; n++) {
            uId = scanner.nextInt();
            scanner.nextFloat();
        }

        while (scanner.hasNextInt()) {
            setValue(uId, scanner.nextInt(), scanner.nextFloat());
            num_val++;
        }
        num_row++;
        IOUtils.closeQuietly(scanner);
    }

    /*========================================
     * Getter/Setter
     *========================================*/
    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The row index to retrieve.
     * @param j The column index to retrieve.
     * @return The value stored at the given index.
     */
    public double getValue(int i, int j) {
        return rows[i].floatValue(j);
    }

    /**
     * Set a new value at the given index.
     * 
     * @param i The row index to store new value.
     * @param j The column index to store new value.
     * @param value The value to store.
     */
    public void setValue(int i, int j, double value) {
        if (value == 0.0) {
            rows[i].remove(j);
            cols[j].remove(i);
        } else {
            rows[i].setValue(j, value);
            cols[j].setValue(i, value);
        }
    }

    /** 
     * @see code.sma.core.AbstractMatrix#rowRef(int)
     */
    @Override
    public DataElem rowRef(int i) {
        throw new RuntimeException("This method has not been implemented in SparseMatrix!");
    }

    /**
     * Return a reference of a given row.
     * Make sure to use this method only for read-only purpose.
     * 
     * @param index The row index to retrieve.
     * @return A reference to the designated row.
     */
    public SparseVector getRowRef(int index) {
        return rows[index];
    }

    /**
     * Return a copy of a given row.
     * Use this if you do not want to affect to original data.
     * 
     * @param index The row index to retrieve.
     * @return A reference to the designated row.
     */
    public SparseVector getRow(int index) {
        SparseVector newVector = this.rows[index].copy();

        return newVector;
    }

    /**
     * Return a reference of a given column.
     * Make sure to use this method only for read-only purpose.
     * 
     * @param index The column index to retrieve.
     * @return A reference to the designated column.
     */
    public SparseVector getColRef(int index) {
        return cols[index];
    }

    /**
     * Return a copy of a given column.
     * Use this if you do not want to affect to original data.
     * 
     * @param index The column index to retrieve.
     * @return A reference to the designated column.
     */
    public SparseVector getCol(int index) {
        SparseVector newVector = this.cols[index].copy();

        return newVector;
    }

    /*========================================
     * Properties
     *========================================*/
    /**
     * Capacity of this matrix.
     * 
     * @return An array containing the length of this matrix.
     * Index 0 contains row count, while index 1 column count.
     */
    public int[] shape() {
        int[] lengthArray = new int[2];

        lengthArray[0] = this.M;
        lengthArray[1] = this.N;

        return lengthArray;
    }

    /**
     * Set a new size of the matrix.
     * 
     * @param m The new row count.
     * @param n The new column count.
     */
    public void setSize(int m, int n) {
        this.M = m;
        this.N = n;
    }

    /**
     * Return items in the diagonal in vector form.
     * 
     * @return Diagonal vector from the matrix.
     */
    public SparseVector diagonal() {
        SparseVector v = new SparseVector(Math.min(this.M, this.N));

        for (int i = 0; i < Math.min(this.M, this.N); i++) {
            double value = this.getValue(i, i);
            if (value > 0.0) {
                v.setValue(i, value);
            }
        }

        return v;
    }

    /**
     * The value of maximum element in the matrix.
     * 
     * @return The maximum value.
     */
    public double max() {
        double curr = Double.MIN_VALUE;

        for (int i = 0; i < this.M; i++) {
            SparseVector v = this.getRowRef(i);
            if (v.itemCount() > 0) {
                double rowMax = v.max();
                if (v.max() > curr) {
                    curr = rowMax;
                }
            }
        }

        return curr;
    }

    /**
     * The value of minimum element in the matrix.
     * 
     * @return The minimum value.
     */
    public double min() {
        double curr = Double.MAX_VALUE;

        for (int i = 0; i < this.M; i++) {
            SparseVector v = this.getRowRef(i);
            if (v.itemCount() > 0) {
                double rowMin = v.min();
                if (v.min() < curr) {
                    curr = rowMin;
                }
            }
        }

        return curr;
    }

    /**
     * Sum of every element. It ignores non-existing values.
     * 
     * @return The sum of all elements.
     */
    public double sum() {
        double sum = 0.0;

        for (int i = 0; i < this.M; i++) {
            SparseVector v = this.getRowRef(i);
            sum += v.sum();
        }

        return sum;
    }

    /**
     * Average of every element. It ignores non-existing values.
     * 
     * @return The average value.
     */
    public double average() {
        return this.sum() / num_ifactor;
    }

    /**
     * Variance of every element. It ignores non-existing values.
     * 
     * @return The variance value.
     */
    public double variance() {
        double avg = this.average();
        double sum = 0.0;

        for (int i = 0; i < this.M; i++) {
            int[] itemList = this.getRowRef(i).indexList();
            if (itemList != null) {
                for (int j : itemList) {
                    sum += Math.pow(this.getValue(i, j) - avg, 2);
                }
            }
        }

        return sum / num_ifactor;
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
     * Matrix operations
     *========================================*/
    /**
     * Scalar subtraction (aX).
     * 
     * @param alpha The scalar value to be multiplied to this matrix.
     * @return The resulting matrix after scaling.
     */
    public SparseMatrix scale(double alpha) {
        SparseMatrix A = new SparseMatrix(this.M, this.N);

        float _alpha = (float) alpha;
        for (int i = 0; i < A.M; i++) {
            A.rows[i] = this.getRowRef(i).scale(_alpha);
        }
        for (int j = 0; j < A.N; j++) {
            A.cols[j] = this.getColRef(j).scale(_alpha);
        }

        return A;
    }

    /**
     * Scalar subtraction (aX) on the matrix itself.
     * This is used for minimizing memory usage.
     * 
     * @param alpha The scalar value to be multiplied to this matrix.
     */
    public void selfScale(double alpha) {
        for (int i = 0; i < this.M; i++) {
            int[] itemList = this.getRowRef(i).indexList();
            if (itemList != null) {
                for (int j : itemList) {
                    this.setValue(i, j, this.getValue(i, j) * alpha);
                }
            }
        }
    }

    /**
     * Scalar addition.
     * @param alpha The scalar value to be added to this matrix.
     * @return The resulting matrix after addition.
     */
    public SparseMatrix add(double alpha) {
        SparseMatrix A = new SparseMatrix(this.M, this.N);

        float _alpha = (float) alpha;
        for (int i = 0; i < A.M; i++) {
            A.rows[i] = this.getRowRef(i).add(_alpha);
        }
        for (int j = 0; j < A.N; j++) {
            A.cols[j] = this.getColRef(j).add(_alpha);
        }

        return A;
    }

    /**
     * Scalar addition on the matrix itself.
     * @param alpha The scalar value to be added to this matrix.
     */
    public void selfAdd(double alpha) {
        for (int i = 0; i < this.M; i++) {
            int[] itemList = this.getRowRef(i).indexList();
            if (itemList != null) {
                for (int j : itemList) {
                    this.setValue(i, j, this.getValue(i, j) + alpha);
                }
            }
        }
    }

    /**
     * Exponential of a given constant.
     * 
     * @param alpha The exponent.
     * @return The resulting exponential matrix.
     */
    public SparseMatrix exp(double alpha) {
        for (int i = 0; i < this.M; i++) {
            SparseVector b = this.getRowRef(i);
            int[] indexList = b.indexList();

            if (indexList != null) {
                for (int j : indexList) {
                    this.setValue(i, j, Math.pow(alpha, this.getValue(i, j)));
                }
            }
        }

        return this;
    }

    /**
     * The transpose of the matrix.
     * This is simply implemented by interchanging row and column each other. 
     * 
     * @return The transpose of the matrix.
     */
    public SparseMatrix transpose() {
        SparseMatrix A = new SparseMatrix(this.N, this.M);

        A.cols = this.rows;
        A.rows = this.cols;

        return A;
    }

    /**
     * transpose on the matrix itself.
     * This is used for minimizing memory usage.
     */
    public void selfTranspose() {
        int N = this.N;
        this.N = this.M;
        this.M = N;

        SparseVector[] rows = this.rows;
        this.rows = this.cols;
        this.cols = rows;
    }

    /**
     * Matrix-vector product (b = Ax)
     * 
     * @param x The vector to be multiplied to this matrix.
     * @throws RuntimeException when dimensions disagree
     * @return The resulting vector after multiplication.
     */
    public SparseVector times(SparseVector x) {
        if (N != x.length())
            throw new RuntimeException("Dimensions disagree");

        SparseMatrix A = this;
        SparseVector b = new SparseVector(M);

        for (int i = 0; i < M; i++) {
            b.setValue(i, A.rows[i].innerProduct(x));
        }

        return b;
    }

    /**
     * Matrix-matrix product (C = AB)
     * 
     * @param B The matrix to be multiplied to this matrix.
     * @throws RuntimeException when dimensions disagree
     * @return The resulting matrix after multiplication.
     */
    public SparseMatrix times(SparseMatrix B) {
        // original implementation
        if (N != (B.shape())[0])
            throw new RuntimeException("Dimensions disagree");

        SparseMatrix A = this;
        SparseMatrix C = new SparseMatrix(M, (B.shape())[1]);
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < (B.shape())[1]; j++) {
                SparseVector x = A.getRowRef(i);
                SparseVector y = B.getColRef(j);

                if (x != null && y != null)
                    C.setValue(i, j, x.innerProduct(y));
                else
                    C.setValue(i, j, 0.0);
            }
        }

        return C;
    }

    /**
     * Matrix-matrix product (A = AB), without using extra memory.
     * 
     * @param B The matrix to be multiplied to this matrix.
     * @throws RuntimeException when dimensions disagree
     */
    public void selfTimes(SparseMatrix B) {
        // original implementation
        if (N != (B.shape())[0])
            throw new RuntimeException("Dimensions disagree");

        for (int i = 0; i < M; i++) {
            SparseVector tmp = new SparseVector(N);
            for (int j = 0; j < (B.shape())[1]; j++) {
                SparseVector x = this.getRowRef(i);
                SparseVector y = B.getColRef(j);

                if (x != null && y != null)
                    tmp.setValue(j, x.innerProduct(y));
                else
                    tmp.setValue(j, 0.0);
            }

            for (int j = 0; j < (B.shape())[1]; j++) {
                this.setValue(i, j, tmp.floatValue(j));
            }
        }
    }

    /**
     * Matrix-matrix sum (C = A + B)
     * 
     * @param B The matrix to be added to this matrix.
     * @throws RuntimeException when dimensions disagree
     * @return The resulting matrix after summation.
     */
    public SparseMatrix plus(SparseMatrix B) {
        SparseMatrix A = this;
        if (A.M != B.M || A.N != B.N)
            throw new RuntimeException("Dimensions disagree");

        SparseMatrix C = new SparseMatrix(M, N);
        for (int i = 0; i < M; i++) {
            C.rows[i] = A.rows[i].plus(B.rows[i]);
        }
        for (int j = 0; j < N; j++) {
            C.cols[j] = A.cols[j].plus(B.cols[j]);
        }

        return C;
    }

    /**
     * Generate an identity matrix with the given size.
     * 
     * @param n The size of requested identity matrix.
     * @return An identity matrix with the size of n by n. 
     */
    public static SparseMatrix makeIdentity(int n) {
        SparseMatrix m = new SparseMatrix(n, n);
        for (int i = 0; i < n; i++) {
            m.setValue(i, i, 1.0);
        }

        return m;
    }

    /**
     * Calculate inverse matrix.
     * 
     * @throws RuntimeException when dimensions disagree.
     * @return The inverse of current matrix.
     */
    public SparseMatrix inverse() {
        if (this.M != this.N)
            throw new RuntimeException("Dimensions disagree");

        SparseMatrix original = this;
        SparseMatrix newMatrix = makeIdentity(this.M);

        int n = this.M;

        if (n == 1) {
            newMatrix.setValue(0, 0, 1 / original.getValue(0, 0));
            return newMatrix;
        }

        SparseMatrix b = new SparseMatrix(original);

        for (int i = 0; i < n; i++) {
            // find pivot:
            double mag = 0;
            int pivot = -1;

            for (int j = i; j < n; j++) {
                double mag2 = Math.abs(b.getValue(j, i));
                if (mag2 > mag) {
                    mag = mag2;
                    pivot = j;
                }
            }

            // no pivot (error):
            if (pivot == -1 || mag == 0) {
                return newMatrix;
            }

            // move pivot row into position:
            if (pivot != i) {
                double temp;
                for (int j = i; j < n; j++) {
                    temp = b.getValue(i, j);
                    b.setValue(i, j, b.getValue(pivot, j));
                    b.setValue(pivot, j, temp);
                }

                for (int j = 0; j < n; j++) {
                    temp = newMatrix.getValue(i, j);
                    newMatrix.setValue(i, j, newMatrix.getValue(pivot, j));
                    newMatrix.setValue(pivot, j, temp);
                }
            }

            // normalize pivot row:
            mag = b.getValue(i, i);
            for (int j = i; j < n; j++) {
                b.setValue(i, j, b.getValue(i, j) / mag);
            }
            for (int j = 0; j < n; j++) {
                newMatrix.setValue(i, j, newMatrix.getValue(i, j) / mag);
            }

            // eliminate pivot row component from other rows:
            for (int k = 0; k < n; k++) {
                if (k == i)
                    continue;

                double mag2 = b.getValue(k, i);

                for (int j = i; j < n; j++) {
                    b.setValue(k, j, b.getValue(k, j) - mag2 * b.getValue(i, j));
                }
                for (int j = 0; j < n; j++) {
                    newMatrix.setValue(k, j,
                        newMatrix.getValue(k, j) - mag2 * newMatrix.getValue(i, j));
                }
            }
        }

        return newMatrix;
    }

    /**
     * Calculate Cholesky decomposition of the matrix.
     * 
     * @throws RuntimeException when matrix is not square.
     * @return The Cholesky decomposition result.
     */
    public SparseMatrix cholesky() {
        if (this.M != this.N)
            throw new RuntimeException("Matrix is not square");

        SparseMatrix A = this;

        int n = A.M;
        SparseMatrix L = new SparseMatrix(n, n);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0.0;
                for (int k = 0; k < j; k++) {
                    sum += L.getValue(i, k) * L.getValue(j, k);
                }
                if (i == j) {
                    L.setValue(i, i, Math.sqrt(A.getValue(i, i) - sum));
                } else {
                    L.setValue(i, j, 1.0 / L.getValue(j, j) * (A.getValue(i, j) - sum));
                }
            }
            if (Double.isNaN(L.getValue(i, i))) {
                //throw new RuntimeException("Matrix not positive definite: (" + i + ", " + i + ")");
                return null;
            }
        }

        return L.transpose();
    }

    /**
     * Generate a covariance matrix of the current matrix.
     * 
     * @return The covariance matrix of the current matrix.
     */
    public SparseMatrix covariance() {
        int columnSize = this.N;
        SparseMatrix cov = new SparseMatrix(columnSize, columnSize);

        for (int i = 0; i < columnSize; i++) {
            for (int j = i; j < columnSize; j++) {
                SparseVector data1 = this.getCol(i);
                SparseVector data2 = this.getCol(j);
                double avg1 = data1.average();
                double avg2 = data2.average();

                double value = data1.sub(avg1).innerProduct(data2.sub(avg2)) / (data1.length() - 1);
                cov.setValue(i, j, value);
                cov.setValue(j, i, value);
            }
        }

        return cov;
    }

    /*========================================
     * Matrix operations (partial)
     *========================================*/
    /**
     * Scalar Multiplication only with indices in indexList.
     * 
     * @param alpha The scalar to be multiplied to this matrix.
     * @param indexList The list of indices to be applied summation.
     * @return The resulting matrix after scaling.
     */
    public SparseMatrix partScale(double alpha, int[] indexList) {
        if (indexList != null) {
            for (int i : indexList) {
                for (int j : indexList) {
                    this.setValue(i, j, this.getValue(i, j) * alpha);
                }
            }
        }

        return this;
    }

    /**
     * Matrix summation (A = A + B) only with indices in indexList.
     * 
     * @param B The matrix to be added to this matrix.
     * @param indexList The list of indices to be applied summation.
     * @throws RuntimeException when dimensions disagree.
     * @return The resulting matrix after summation.
     */
    public SparseMatrix partPlus(SparseMatrix B, int[] indexList) {
        if (indexList != null) {
            if (this.M != B.M || this.N != B.N)
                throw new RuntimeException("Dimensions disagree");

            for (int i : indexList) {
                this.rows[i].partPlus(B.rows[i], indexList);
            }
            for (int j : indexList) {
                this.cols[j].partPlus(B.cols[j], indexList);
            }
        }

        return this;
    }

    /**
     * Matrix subtraction (A = A - B) only with indices in indexList.
     * 
     * @param B The matrix to be subtracted from this matrix.
     * @param indexList The list of indices to be applied subtraction.
     * @throws RuntimeException when dimensions disagree.
     * @return The resulting matrix after subtraction.
     */
    public SparseMatrix partMinus(SparseMatrix B, int[] indexList) {
        if (indexList != null) {
            if (this.M != B.M || this.N != B.N)
                throw new RuntimeException("Dimensions disagree");

            for (int i : indexList) {
                this.rows[i].partMinus(B.rows[i], indexList);
            }
            for (int j : indexList) {
                this.cols[j].partMinus(B.cols[j], indexList);
            }
        }

        return this;
    }

    /**
     * Matrix-vector product (b = Ax) only with indices in indexList.
     * 
     * @param x The vector to be multiplied to this matrix.
     * @param indexList The list of indices to be applied multiplication.
     * @return The resulting vector after multiplication.
     */
    public SparseVector partTimes(SparseVector x, int[] indexList) {
        if (indexList == null)
            return x;

        SparseVector b = new SparseVector(M);

        for (int i : indexList) {
            b.setValue(i, this.rows[i].partInnerProduct(x, indexList));
        }

        return b;
    }

    /**
     * Convert the matrix to a printable string.
     * 
     * @return The resulted string in the form of "(1, 2: 5.0) (2, 4: 4.5)"
     */
    @Override
    public String toString() {
        String s = "";

        for (int i = 0; i < this.M; i++) {
            SparseVector row = this.getRowRef(i);
            for (int j : row.indexList()) {
                s += "(" + i + ", " + j + ": " + this.getValue(i, j) + ") ";
            }
            s += "\r\n";
        }

        return s;
    }

}
