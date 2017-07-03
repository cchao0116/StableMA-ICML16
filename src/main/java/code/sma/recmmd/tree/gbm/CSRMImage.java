package code.sma.recmmd.tree.gbm;

import java.util.ArrayList;
import java.util.List;

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
    /** REF: the row IDs  and the feature value in corresponding row*/
    private List<CSREntry> entries;
    /** the pointer of each feature with length N+1, N is the number of features */
    private int[]          ptr_feature;
    /** active feature indices*/
    private IntSet         active_index;

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
        entries = new ArrayList<CSREntry>(num_val);
    }

    /**
     * add element to the image
     * 
     * @param index_row         the index/id of row
     * @param index_factor      the factor index
     * @param value_factor      the factor value
     */
    public void addElement(int index_row, int index_factor, float value_factor) {
        entries.set(ptr_feature[index_factor], new CSREntry(index_row, value_factor));
        ptr_feature[index_factor]++;
    }

    public IntSet getAcctvInd() {
        return active_index;
    }

    public List<CSREntry> getFeatureRef(int index_factor) {
        return entries.subList(ptr_feature[index_factor - 1], ptr_feature[index_factor]);
    }

    public class CSREntry implements Comparable<CSREntry> {
        /** row index*/
        public int   index_row;
        /** factor value*/
        public float value_factor;

        public CSREntry(int index_row, float value_factor) {
            super();
            this.index_row = index_row;
            this.value_factor = value_factor;
        }

        /** 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(CSREntry o) {
            //in ascending order 
            return Float.compare(this.fValue(), o.fValue());
        }

        public float fValue() {
            return value_factor;
        }

        public int rIndex() {
            return index_row;
        }

    }
}
