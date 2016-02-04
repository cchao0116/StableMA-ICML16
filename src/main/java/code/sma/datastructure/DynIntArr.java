package code.sma.datastructure;

import java.util.Arrays;

/**
 * Dynamic Primary int array list
 * @author Hanke
 * @version $Id: DynIntArr.java, v 0.1 Jan 28, 2016 8:30:26 PM Exp $
 */
public final class DynIntArr {
    /** the value list*/
    int[] values;
    /** the number of elements in the list*/
    int   nnz;

    /**
     * Construction
     * @param nnz   the number of elements in the list
     */
    public DynIntArr(int nnz) {
        super();
        this.nnz = 0;
        this.values = new int[nnz];
    }

    /**
     * Construction
     * @param values he inner values
     */
    public DynIntArr(int[] values) {
        this.values = values;
        this.nnz = values.length;
    }

    /**
     * add one value to this array
     * 
     * @param val   the value to set
     */
    public void addValue(int val) {
        if (nnz == values.length) {
            int[] newArr = new int[2 * nnz];
            System.arraycopy(values, 0, newArr, 0, nnz);
            this.values = newArr;
        }

        values[nnz] = val;
        nnz++;
    }

    /**
     * retrieve the value given the index
     * 
     * @param i     the index of the value
     * @return      the disired value
     */
    public int getValue(int i) {
        if (i >= nnz) {
            return Integer.MIN_VALUE;
        }

        return values[i];
    }

    /**
     * get the compact int array
     * 
     * @return  the compact array
     */
    public int[] getArr() {
        reduceMem();
        return values;
    }

    /**
     * make array compact
     */
    public void reduceMem() {
        if (nnz < values.length) {
            values = Arrays.copyOf(values, nnz);
        }
    }

}
