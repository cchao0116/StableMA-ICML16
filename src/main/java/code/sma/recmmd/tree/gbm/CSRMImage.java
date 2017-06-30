package code.sma.recmmd.tree.gbm;

import code.sma.core.impl.CRefVector;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * The image store the data in compressed column storage format
 * 
 * @author Chao.Chen
 * @version $Id: CSRMImage.java, Jun 30, 2017 2:02:49 PM$
 */
public final class CSRMImage {
    // the rowId-factorValue pair
    /** REF: the row IDs*/
    private int[]   index_row;
    /** REF: the feature value in corresponding row*/
    private float[] value_factor;
    /** the pointer of each feature with length N+1, N is the number of features */
    private int[]   ptr_feature;
    /** active feature indices*/
    private IntSet  active_index;

    public CSRMImage(int num_feature) {
        this.ptr_feature = new int[num_feature + 1];
        this.active_index = new IntOpenHashSet();
    }

    /**
     * add budget in order to compute the number of factors in one feature
     * 
     * @param index_feature index/id of the feature to add
     */
    public void addBudget(int index_feature) {
        active_index.add(index_feature);
        ptr_feature[index_feature]++;
    }

    /**
     * build the storage, namely build the pointer of each feature
     */
    public void buildStorage() {
        int start = 0;

        for (int i = 1; i < ptr_feature.length; i++) {
            int fSize = ptr_feature[i];
            ptr_feature[i] = start;
            start += fSize;
        }

        int num_val = start;
        this.index_row = new int[num_val];
        this.value_factor = new float[num_val];
    }

    /**
     * add element to the image
     * 
     * @param index_row         the index/id of row
     * @param index_factor      the factor index
     * @param value_factor      the factor value
     */
    public void addElement(int index_row, int index_factor, float value_factor) {
        this.index_row[ptr_feature[index_factor]] = index_row;
        this.value_factor[ptr_feature[index_factor]] = value_factor;
        ptr_feature[index_factor]++;
    }

    public IntSet getAcctvInd() {
        return active_index;
    }

    public CRefVector getRowIndxRef(int index_factor) {
        return new CRefVector(index_row, ptr_feature[index_factor - 1], ptr_feature[index_factor]);
    }

    public CRefVector getFactorValRef(int index_factor) {
        return new CRefVector(value_factor, ptr_feature[index_factor - 1],
            ptr_feature[index_factor]);
    }
}
