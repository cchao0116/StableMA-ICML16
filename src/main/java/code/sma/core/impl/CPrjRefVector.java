package code.sma.core.impl;

/**
 * 
 * @author Chao.Chen
 * @version $Id: CPrjRefVector.java, v 0.1 2017年6月6日 下午1:36:49 Chao.Chen Exp $
 */
public class CPrjRefVector extends CRefVector {
    private static final long serialVersionUID = 1L;
    /** projection mappings*/
    protected short[]         prj_mpg;

    public CPrjRefVector(float[] data, int ptr_offset, int num_factors) {
        super(data, ptr_offset, num_factors);
    }

    public CPrjRefVector(float[] data, int ptr_offset, int num_factors, short[] prj_mpg) {
        super(data, ptr_offset, num_factors);
        this.prj_mpg = prj_mpg;
    }

    public CPrjRefVector(int[] data, int ptr_offset, int num_factors) {
        super(data, ptr_offset, num_factors);
    }

    public CPrjRefVector(int[] data, int ptr_offset, int num_factors, short[] prj_mpg) {
        super(data, ptr_offset, num_factors);
        this.prj_mpg = prj_mpg;
    }

    /** 
     * @see code.sma.core.impl.CRefVector#floatValue(int)
     */
    @Override
    public float floatValue(int i) {
        assert i >= 0 && i < num_factors : String.format("index should be in [0, %d)", num_factors);
        if (prj_mpg == null)
            return super.floatValue(i);

        switch (refType) {
            case Ints:
                return intPtr[ptr_offset + prj_mpg[i]];
            case Floats:
                return floatPtr[ptr_offset + prj_mpg[i]];
            default:
                throw new RuntimeException("CRefArray only support Ints, Floats.");
        }
    }

    /** 
     * @see code.sma.core.impl.CRefVector#intValue(int)
     */
    @Override
    public int intValue(int i) {
        assert i >= 0 && i < num_factors : String.format("index should be in [0, %d)", num_factors);
        if (prj_mpg == null)
            return super.intValue(i);

        switch (refType) {
            case Ints:
                return intPtr[ptr_offset + prj_mpg[i]];
            case Floats:
                return (int) floatPtr[ptr_offset + prj_mpg[i]];
            default:
                throw new RuntimeException("CRefArray only support Ints, Floats.");
        }
    }

    /**
     * Getter method for property <tt>prj_mpg</tt>.
     * 
     * @return property value of prj_mpg
     */
    public short[] getPrj_mpg() {
        return prj_mpg;
    }

    /**
     * Setter method for property <tt>prj_mpg</tt>.
     * 
     * @param prj_mpg value to be assigned to property prj_mpg
     */
    public void setPrj_mpg(short[] prj_mpg) {
        this.prj_mpg = prj_mpg;
    }

}
