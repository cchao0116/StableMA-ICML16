package code.sma.core;

import java.io.Serializable;

/**
 * Implementation of node in trees.
 * 
 * @author Chao.Chen
 * @version $Id: TreeNode.java, v 0.1 Jun 28, 2017 1:45:57 PM Exp $
 */
public class TreeNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private float             leafValue;
    /** pointer to parent, highest bit is used to indicate whether it's a left child or not*/
    private int               parent;
    /**  pointer to left, right */
    public int                left, right;
    /**  split feature index */
    private int               splitIndex;
    /**  split condition*/
    private float             splitCond;
    /** statistics of current node*/
    private NodeStat          nodeStat;

    public void set_parent(int pidx) {
        parent = pidx | (1 << 31);
    }

    public void set_parent(int pidx, boolean is_left_child) {
        if (is_left_child) {
            pidx |= (1 << 31);
        }
        parent = pidx;
    }

    public void setSplit(int splitIndex, float splitCond) {
        this.splitIndex = splitIndex;
        this.splitCond = splitCond;
    }

    public void setLeaf(double leafVal) {
        this.leafValue = (float) leafVal;
        this.left = -1;
        this.right = -1;
    }

    public boolean isRoot() {
        return parent == -1;
    }

    public boolean isLeaf() {
        return left == -1 && right == -1;
    }

    public boolean isLeftChild() {
        return (parent >> 31) != 0;
    }

    /**
     * Getter method for property <tt>leafValue</tt>.
     * 
     * @return property value of leafValue
     */
    public float getLeafValue() {
        return leafValue;
    }

    /**
     * Getter method for property <tt>splitCond</tt>.
     * 
     * @return property value of splitCond
     */
    public float getSplitCond() {
        return splitCond;
    }

    /**
     * Getter method for property <tt>parent</tt>.
     * 
     * @return property value of parent
     */
    public int getParent() {
        return parent & ((1 << 31) - 1);
    }

    /**
     * Getter method for property <tt>splitIndex</tt>.
     * 
     * @return property value of splitIndex
     */
    public int getSplitIndex() {
        return splitIndex;
    }

    /**
     * Getter method for property <tt>nodeStat</tt>.
     * 
     * @return property value of nodeStat
     */
    public NodeStat getNodeStat() {
        return nodeStat;
    }

    public class NodeStat implements Serializable {
        private static final long serialVersionUID = 1L;

        /** loss chg caused by current split*/
        public float              loss_chg;
        /** weight of current node*/
        public float              base_weight;
        /** number of child that is leaf node known up to now */
        public int                leaf_child_cnt;
    }
}
