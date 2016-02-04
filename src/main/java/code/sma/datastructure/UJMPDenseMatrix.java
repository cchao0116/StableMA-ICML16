package code.sma.datastructure;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;

/**
 * 
 * @author Hanke
 * @version $Id: UJMPDenseMatrix.java, v 0.1 Jan 28, 2016 1:30:25 PM Exp $
 */
public class UJMPDenseMatrix {
    /** The number of rows. */
    private int    M;
    /** The number of columns. */
    private int    N;
    /** The UJMP matrix to store data. */
    private Matrix map;

    /*========================================
     * Constructors
     *========================================*/
    /**
     * Construct an empty dense matrix, with a given size.
     * 
     * @param m The number of rows.
     * @param n The number of columns.
     */
    public UJMPDenseMatrix(int m, int n) {
        this.M = m;
        this.N = n;
        this.map = org.ujmp.core.DenseMatrix.Factory.zeros(m, n);
    }

    /**
     * Construct an empty dense matrix, with data copied from UJMP matrix.
     * 
     * @param m An UJMP matrix.
     */
    public UJMPDenseMatrix(Matrix m) {
        long[] size = m.getSize();
        this.M = (int) (size[0]);
        this.N = (int) (size[1]);
        this.map = m;
    }

    /*========================================
     * Getter/Setter
     *========================================*/
    /**
     * Get an UJMP matrix.
     * 
     * @return UJMP matrix.
     */
    public Matrix getMatrix() {
        return map;
    }

    /**
     * Set a new value at the given index.
     * 
     * @param i The row index to store new value.
     * @param j The column index to store new value.
     * @param value The value to store.
     */
    public void setValue(int i, int j, double value) {
        if (i < 0 || i >= this.M || j < 0 || j >= this.N) {
            throw new ArrayIndexOutOfBoundsException("Out of index range: " + i);
        }

        map.setAsDouble(value, i, j);
    }

    /**
     * Retrieve a stored value from the given index.
     * 
     * @param i The row index to retrieve.
     * @param j The column index to retrieve.
     * @return The value stored at the given index.
     */
    public double getValue(int i, int j) {
        if (i < 0 || i >= this.M || j < 0 || j >= this.N) {
            throw new ArrayIndexOutOfBoundsException("Out of index range: " + i);
        }

        return map.getAsDouble(i, j);
    }

    /**
     * Return a reference of a given row.
     * Make sure to use this method only for read-only purpose.
     * 
     * @param index The row index to retrieve.
     * @return A reference to the designated row.
     */
    public UJMPDenseVector getRowRef(int index) {
        Matrix m = map.selectRows(Ret.ORIG, index);
        m = m.transpose();
        return new UJMPDenseVector(m);
    }

    /**
     * Return a copy of a given row.
     * Use this if you do not want to affect to original data.
     * 
     * @param index The row index to retrieve.
     * @return A reference to the designated row.
     */
    public UJMPDenseVector getRow(int index) {
        Matrix m = map.selectRows(Ret.NEW, index);
        m = m.transpose();
        return new UJMPDenseVector(m);
    }

    /**
     * Return a reference of a given column.
     * Make sure to use this method only for read-only purpose.
     * 
     * @param index The column index to retrieve.
     * @return A reference to the designated column.
     */
    public UJMPDenseVector getColRef(int index) {
        Matrix m = map.selectColumns(Ret.LINK, index);
        return new UJMPDenseVector(m);
    }

    /**
     * Return a copy of a given column.
     * Use this if you do not want to affect to original data.
     * 
     * @param index The column index to retrieve.
     * @return A reference to the designated column.
     */
    public UJMPDenseVector getCol(int index) {
        Matrix m = map.selectColumns(Ret.NEW, index);
        return new UJMPDenseVector(m);
    }

    /**
     * Convert the matrix into sparse matrix.
     * 
     * @return The converted sparse matrix.
     */
    public SparseMatrix toSparseMatrix() {
        SparseMatrix m = new SparseMatrix(this.M, this.N);

        for (int i = 0; i < this.M; i++) {
            for (int j = 0; j < this.N; j++) {
                double value = this.getValue(i, j);

                if (value != 0.0) {
                    m.setValue(i, j, value);
                }
            }
        }

        return m;
    }

    /**
     * Condense the matrix only with given indices.
     * 
     * @param indexList The list of indices.
     * @return The converted matrix, only with given indices. 
     */
    public UJMPDenseMatrix toDenseSubset(int[] indexList) {
        if (indexList == null || indexList.length == 0)
            return null;

        UJMPDenseMatrix m = new UJMPDenseMatrix(indexList.length, indexList.length);

        int x = 0;
        for (int i : indexList) {
            int y = 0;
            for (int j : indexList) {
                m.setValue(x, y, this.getValue(i, j));
                y++;
            }
            x++;
        }

        return m;
    }

    /**
     * Condense the matrix only with given indices, both rows and columns separately.
     * 
     * @param rowList The list of row indices.
     * @param colList The list of column indices.
     * @return The converted matrix, only with given indices. 
     */
    public UJMPDenseMatrix toDenseSubset(int[] rowList, int[] colList) {
        if (rowList == null || colList == null || rowList.length == 0 || colList.length == 0)
            return null;

        UJMPDenseMatrix m = new UJMPDenseMatrix(rowList.length, colList.length);

        int x = 0;
        for (int i : rowList) {
            int y = 0;
            for (int j : colList) {
                m.setValue(x, y, this.getValue(i, j));
                y++;
            }
            x++;
        }

        return m;
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
    public void plusInRow(int u, UJMPDenseVector b) {
        if (this.N != b.length())
            throw new RuntimeException("Vector lengths disagree");

        for (int n = 0; n < N; n++) {
            this.setValue(u, n, this.getValue(u, n) + b.getValue(n));
        }
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
    public int[] length() {
        int[] lengthArray = new int[2];

        lengthArray[0] = this.M;
        lengthArray[1] = this.N;

        return lengthArray;
    }

    /**
     * Actual number of items in the matrix.
     * 
     * @return The number of items in the matrix.
     */
    public int itemCount() {
        return (int) map.getValueCount();
    }

    /**
     * Return items in the diagonal in vector form.
     * 
     * @return Diagonal vector from the matrix.
     */
    public UJMPDenseVector diagonal() {
        UJMPDenseVector v = new UJMPDenseVector(Math.min(this.M, this.N));

        for (int i = 0; i < Math.min(this.M, this.N); i++) {
            double value = this.getValue(i, i);
            if (value > 0.0) {
                v.setValue(i, value);
            }
        }

        return v;
    }

    /**
     * Sum of every element. It ignores non-existing values.
     * 
     * @return The sum value.
     */
    public double sum() {
        Matrix colSum = map.sum(Ret.LINK, 0, true);
        Matrix totalSum = colSum.sum(Ret.LINK, 1, true);
        return totalSum.getAsDouble(0, 0);
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

        for (int i = 0; i < this.M; i++) {
            for (int j : this.getRowRef(i).indexList()) {
                sum += Math.pow(this.getValue(i, j) - avg, 2);
            }
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
     * Matrix operations
     *========================================*/
    /**
     * Scalar multiplication (aX).
     * 
     * @param alpha The scalar value to be multiplied to this matrix.
     * @return The resulting matrix after scaling.
     */
    public UJMPDenseMatrix scale(double alpha) {
        return new UJMPDenseMatrix(map.times(alpha));
    }

    /**
     * Scalar multiplication (aX) on the matrix itself.
     * This is used for minimizing memory usage.
     * 
     * @param alpha The scalar value to be multiplied to this matrix.
     */
    public void selfScale(double alpha) {
        map = map.times(alpha);
    }

    /**
     * Scalar addition.
     * @param alpha The scalar value to be added to this matrix.
     * @return The resulting matrix after addition.
     */
    public UJMPDenseMatrix add(double alpha) {
        return new UJMPDenseMatrix(map.plus(alpha));
    }

    /**
     * Scalar addition on the matrix itself.
     * @param alpha The scalar value to be added to this matrix.
     */
    public void selfAdd(double alpha) {
        map = map.plus(alpha);
    }

    /**
     * Exponential of a given constant.
     * 
     * @param alpha The exponent.
     * @return The resulting exponential matrix.
     */
    public UJMPDenseMatrix exp(double alpha) {
        for (int i = 0; i < this.M; i++) {
            for (int j = 0; j < this.N; j++) {
                this.setValue(i, j, Math.pow(alpha, this.getValue(i, j)));
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
    public UJMPDenseMatrix transpose() {
        return new UJMPDenseMatrix(map.transpose());
    }

    /**
     * Matrix-vector product (b = Ax)
     * 
     * @param x The vector to be multiplied to this matrix.
     * @throws RuntimeException when dimensions disagree
     * @return The resulting vector after multiplication.
     */
    public UJMPDenseVector times(UJMPDenseVector x) {
        if (N != x.length())
            throw new RuntimeException("Dimensions disagree");

        Matrix m = map.mtimes(x.getVector());
        return new UJMPDenseVector(m);
    }

    /**
     * Matrix-matrix product (C = AB)
     * 
     * @param B The matrix to be multiplied to this matrix.
     * @throws RuntimeException when dimensions disagree
     * @return The resulting matrix after multiplication.
     */
    public UJMPDenseMatrix times(UJMPDenseMatrix B) {
        if (N != (B.length())[0])
            throw new RuntimeException("Dimensions disagree");

        return new UJMPDenseMatrix(map.mtimes(B.getMatrix()));
    }

    // Matrix-Matrix product (A = AB), without using extra memory.
    //  public void selfTimes(FastMatrix B) {
    //      if (N != (B.length())[0])
    //          throw new RuntimeException("Dimensions disagree");
    //      
    //      // Do not work correctly....
    //      st.mtimes(Ret.ORIG, true, B.getMatrix());
    //  }

    /**
     * Matrix-matrix sum (C = A + B)
     * 
     * @param B The matrix to be added to this matrix.
     * @throws RuntimeException when dimensions disagree
     * @return The resulting matrix after summation.
     */
    public UJMPDenseMatrix plus(UJMPDenseMatrix B) {
        UJMPDenseMatrix A = this;
        if (A.M != B.M || A.N != B.N)
            throw new RuntimeException("Dimensions disagree");

        Matrix m1 = A.map;
        Matrix m2 = B.getMatrix();

        return new UJMPDenseMatrix(m1.plus(m2));
    }

    /**
     * Generate an identity matrix with the given size.
     * 
     * @param n The size of requested identity matrix.
     * @return An identity matrix with the size of n by n. 
     */
    public static UJMPDenseMatrix makeIdentity(int n) {
        return new UJMPDenseMatrix(org.ujmp.core.DenseMatrix.Factory.eye(n, n));
    }

    /**
     * Calculate inverse matrix.
     * 
     * @throws RuntimeException when dimensions disagree.
     * @return The inverse of current matrix.
     */
    public UJMPDenseMatrix inverse() {
        if (this.M != this.N)
            throw new RuntimeException("Dimensions disagree");

        return new UJMPDenseMatrix(this.map.inv());
    }

    /**
     * Calculate Cholesky decomposition of the matrix.
     * 
     * @throws RuntimeException when matrix is not square.
     * @return The Cholesky decomposition result.
     */
    public UJMPDenseMatrix cholesky() {
        if (this.M != this.N)
            throw new RuntimeException("Matrix is not square");

        // ToDo: Need to check whether A is symmetric...

        UJMPDenseMatrix A = this.transpose();

        int n = A.M;
        UJMPDenseMatrix L = new UJMPDenseMatrix(n, n);

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
    public UJMPDenseMatrix covariance() {
        return new UJMPDenseMatrix(map.cov(Ret.NEW, true, true));
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
    public UJMPDenseMatrix partScale(double alpha, int[] indexList) {
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
    public UJMPDenseMatrix partPlus(UJMPDenseMatrix B, int[] indexList) {
        if (indexList != null) {
            if (this.M != B.M || this.N != B.N)
                throw new RuntimeException("Dimensions disagree");

            for (int i : indexList) {
                for (int j : indexList) {
                    this.setValue(i, j, this.getValue(i, j) + B.getValue(i, j));
                }
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
    public UJMPDenseMatrix partMinus(UJMPDenseMatrix B, int[] indexList) {
        if (indexList != null) {
            if (this.M != B.M || this.N != B.N)
                throw new RuntimeException("Dimensions disagree");

            for (int i : indexList) {
                for (int j : indexList) {
                    this.setValue(i, j, this.getValue(i, j) - B.getValue(i, j));
                }
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
    public UJMPDenseVector partTimes(UJMPDenseVector x, int[] indexList) {
        if (indexList == null)
            return x;

        UJMPDenseVector b = new UJMPDenseVector(M);

        for (int i : indexList) {
            b.setValue(i, this.getRowRef(i).partInnerProduct(x, indexList));
        }

        return b;
    }

    /**
     * Inverse of matrix only with indices in indexList.
     * 
     * @param indexList The list of indices to be applied multiplication.
     * @throws RuntimeException when dimensions disagree.
     * @return The resulting inverse matrix.
     */
    public UJMPDenseMatrix partInverse(int[] indexList) {
        if (indexList == null)
            return this;

        if (this.M != this.N)
            throw new RuntimeException("Dimensions disagree");

        UJMPDenseMatrix original = this;
        UJMPDenseMatrix newMatrix = makeIdentity(this.M);

        int n = indexList.length;

        if (n == 1) {
            newMatrix.setValue(0, 0, 1 / original.getValue(0, 0));
            return newMatrix;
        }

        UJMPDenseMatrix b = new UJMPDenseMatrix(original.M, original.N);
        for (int i : indexList) {
            for (int j : indexList) {
                b.setValue(i, j, original.getValue(i, j));
            }
        }

        for (int ii = 0; ii < n; ii++) {
            int i = indexList[ii];

            // find pivot:
            double mag = 0;
            int pivot = -1;

            for (int jj = i; jj < n; jj++) {
                int j = indexList[jj];

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
                for (int jj = i; jj < n; jj++) {
                    int j = indexList[jj];

                    temp = b.getValue(i, j);
                    b.setValue(i, j, b.getValue(pivot, j));
                    b.setValue(pivot, j, temp);
                }

                for (int jj = i; jj < n; jj++) {
                    int j = indexList[jj];

                    temp = newMatrix.getValue(i, j);
                    newMatrix.setValue(i, j, newMatrix.getValue(pivot, j));
                    newMatrix.setValue(pivot, j, temp);
                }
            }

            // normalize pivot row:
            mag = b.getValue(i, i);
            for (int jj = i; jj < n; jj++) {
                int j = indexList[jj];
                b.setValue(i, j, b.getValue(i, j) / mag);
            }
            for (int jj = 0; jj < n; jj++) {
                int j = indexList[jj];
                newMatrix.setValue(i, j, newMatrix.getValue(i, j) / mag);
            }

            // eliminate pivot row component from other rows:
            for (int kk = 0; kk < n; kk++) {
                int k = indexList[kk];

                if (k == i)
                    continue;

                double mag2 = b.getValue(k, i);

                for (int jj = i; jj < n; jj++) {
                    int j = indexList[jj];
                    b.setValue(k, j, b.getValue(k, j) - mag2 * b.getValue(i, j));
                }
                for (int jj = 0; jj < n; jj++) {
                    int j = indexList[jj];
                    newMatrix.setValue(k, j,
                        newMatrix.getValue(k, j) - mag2 * newMatrix.getValue(i, j));
                }
            }
        }

        return newMatrix;
    }

    /**
     * Convert the matrix to a printable string.
     * 
     * @return The resulted string."
     */
    @Override
    public String toString() {
        return map.toString();
    }
}
