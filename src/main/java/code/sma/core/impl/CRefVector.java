package code.sma.core.impl;

import code.sma.core.AbstractVector;

/**
 * 
 * @author Chao.Chen
 * @version $Id: CRefVector.java, v 0.1 2017年6月5日 上午11:08:39 Chao.Chen Exp $
 */
public class CRefVector extends AbstractVector {
    private static final long serialVersionUID = 1L;

    /** number of nonzero feature*/
    protected int             num_factors;
    /** offset to initial data point*/
    protected int             ptr_offset;
    /** the type of array*/
    CRefType                  refType;

    //note: point to any arrays
    /** point to integer array*/
    protected int[]           intPtr;
    /** point to float array*/
    protected float[]         floatPtr;

    public CRefVector(float[] data, int ptr_offset, int num_factors) {
        this.floatPtr = data;
        this.num_factors = num_factors;
        this.ptr_offset = ptr_offset;
        refType = CRefType.Floats;
    }

    public CRefVector(int[] data, int ptr_offset, int num_factors) {
        this.intPtr = data;
        this.num_factors = num_factors;
        this.ptr_offset = ptr_offset;
        refType = CRefType.Ints;
    }

    /** 
     * @see code.sma.core.AbstractVector#setValue(int, double)
     */
    @Override
    public void setValue(int i, double value) {
        assert i >= 0 && i < num_factors : String.format("index should be in [%d, %d)", ptr_offset,
            ptr_offset + num_factors);

        switch (refType) {
            case Ints:
                intPtr[ptr_offset + i] = (int) value;
            case Floats:
                floatPtr[ptr_offset + i] = (float) value;
            default:
                throw new RuntimeException("CRefArray only support Ints, Floats.");
        }
    }

    /** 
     * @see code.sma.core.AbstractVector#setValue(int, float)
     */
    //    @Override
    public void setValue(int i, float value) {
        assert i >= 0 && i < num_factors : String.format("index should be in [%d, %d)", ptr_offset,
            ptr_offset + num_factors);

        switch (refType) {
            case Ints:
                intPtr[ptr_offset + i] = (int) value;
            case Floats:
                floatPtr[ptr_offset + i] = (float) value;
            default:
                throw new RuntimeException("CRefArray only support Ints, Floats.");
        }
    }

    /** 
     * @see code.sma.core.AbstractVector#floatValue(int)
     */
    @Override
    public float floatValue(int i) {
        assert i >= 0 && i < num_factors : String.format("index should be in [%d, %d)", ptr_offset,
            ptr_offset + num_factors);

        switch (refType) {
            case Ints:
                return intPtr[ptr_offset + i];
            case Floats:
                return floatPtr[ptr_offset + i];
            default:
                throw new RuntimeException("CRefArray only support Ints, Floats.");
        }
    }

    /** 
     * @see code.sma.core.AbstractVector#intValue(int)
     */
    @Override
    public int intValue(int i) {
        assert i >= 0 && i < num_factors : String.format("index should be in [%d, %d)", ptr_offset,
            ptr_offset + num_factors);

        switch (refType) {
            case Ints:
                return intPtr[ptr_offset + i];
            case Floats:
                return (int) floatPtr[ptr_offset + i];
            default:
                throw new RuntimeException("CRefArray only support Ints, Floats.");
        }
    }

    /** 
     * @see code.sma.core.AbstractVector#length()
     */
    @Override
    public int length() {
        return num_factors;
    }

    /** 
     * @see code.sma.core.AbstractVector#innerProduct(code.sma.core.AbstractVector)
     */
    @Override
    public double innerProduct(AbstractVector b) {
        throw new RuntimeException("This method has not been implemented in CRefVector!");
    }

    /**
     * Setter method for property <tt>num_factors</tt>.
     * 
     * @param num_factors value to be assigned to property num_factors
     */
    public void setNum_factors(int num_factors) {
        this.num_factors = num_factors;
    }

    /**
     * Setter method for property <tt>ptr_offset</tt>.
     * 
     * @param ptr_offset value to be assigned to property ptr_offset
     */
    public void setPtr_offset(int ptr_offset) {
        this.ptr_offset = ptr_offset;
    }

    /**
     * Setter method for property <tt>intPtr</tt>.
     * 
     * @param intPtr value to be assigned to property intPtr
     */
    public void setIntPtr(int[] intPtr) {
        this.intPtr = intPtr;
        this.refType = CRefType.Ints;
    }

    /**
     * Setter method for property <tt>floatPtr</tt>.
     * 
     * @param floatPtr value to be assigned to property floatPtr
     */
    public void setFloatPtr(float[] floatPtr) {
        this.floatPtr = floatPtr;
        this.refType = CRefType.Floats;
    }

    /**
     * The type of arrays
     * 
     * @author Chao.Chen
     * @version $Id: CRefVector.java, v 0.1 2017年6月5日 下午12:43:22 Chao.Chen Exp $
     */
    protected enum CRefType {
                             Ints, Floats;
    }

}
