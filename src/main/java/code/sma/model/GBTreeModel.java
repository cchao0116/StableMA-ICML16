package code.sma.model;

import java.util.ArrayList;
import java.util.List;

import code.sma.core.DataElem;
import code.sma.core.TreeNode;
import code.sma.core.impl.CRefVector;
import code.sma.core.impl.SparseVector;
import code.sma.main.Configures;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * implementation of regression tree
 * 
 * @author Chao.Chen
 * @version $Id: TreeModel.java, v 0.1 2017年6月19日 下午2:43:37$
 */
public class GBTreeModel extends AbstractModel {
    private static final long serialVersionUID = 1L;

    /** number of features*/
    protected SparseVector    sv_ufeatures;
    /** list of nodes */
    protected List<TreeNode>  nodes;
    /** list of deleted node IDs*/
    protected IntArrayList    deleted_nodes;
    /** maximum depth*/
    protected int             max_depth;

    // Statistics
    /** total number of nodes*/
    private int               num_nodes;
    /** number of deleted nodes*/
    private int               num_deleted;

    public GBTreeModel(Configures conf) {
        super(conf);
        nodes = new ArrayList<TreeNode>();
        max_depth = conf.getInteger("MAX_DEPTH_VALUE");

        deleted_nodes = new IntArrayList();
        sv_ufeatures = new SparseVector(conf.getInteger("MAX_FEATURE_NUM_VALUE"));
    }

    /**
     * @see code.sma.model.AbstractModel#predict(int, int)
     */
    @Override
    public double predict(int u, int i) {
        throw new RuntimeException("This method has not been implemented in TreeModel!");
    }

    /**
     * @see code.sma.model.AbstractModel#predict(code.sma.core.DataElem)
     */
    @Override
    public double predict(DataElem e) {
        CRefVector index_user = e.getIndex_user();
        CRefVector value_ufactor = e.getValue_ufactor();
        assert index_user != null && value_ufactor != null : "Feature should not be null";

        sv_ufeatures.clear();
        for (int f = 0; f < index_user.length(); f++) {
            sv_ufeatures.setValue(index_user.intValue(f), value_ufactor.floatValue(f));
        }

        TreeNode n = nodes.get(0);
        while (!n.isLeaf()) {
            int splitIndx = n.getSplitIndex();
            float splitCon = n.getSplitCond();

            if (sv_ufeatures.floatValue(splitIndx) > splitCon) {
                // >= split condition, then choose left child
                n = nodes.get(n.left);
            } else {
                // choose right child otherwise
                n = nodes.get(n.right);
            }
        }
        return n.getLeafValue();
    }

    /**
     * get the reference of the node
     * 
     * @param nid   the id of the node
     * @return
     */
    public TreeNode get(int nid) {
        return nodes.get(nid);
    }

    /**
     * change a non-leaf node to leaf
     *      
     * @param nid       node id
     * @param lVal      leaf value
     */
    public void change2Leaf(int nid, float lVal) {
        assert !nodes.get(nid).isLeaf() : "Leaf cannot be changed to leaf again.";

        TreeNode n = nodes.get(nid);
        deleteNode(n.left);
        deleteNode(n.right);
        n.setLeaf(lVal);
    }

    /**
     * add children to given node 
     * 
     * @param nid   the id of the node to add 
     */
    public void addChildren(int nid) {
        TreeNode n = nodes.get(nid);

        n.left = allocNode();
        nodes.get(n.left).set_parent(nid);

        n.right = allocNode();
        nodes.get(n.right).set_parent(nid);
    }

    /**
     * delete given node
     * 
     * @param nid   the id of the node to delete 
     */
    public void deleteNode(int nid) {
        nodes.get(nid).set_parent(-1);
        deleted_nodes.add(nid);
        num_deleted++;
    }

    protected int allocNode() {
        if (num_deleted != 0) {
            num_deleted--;
            int nid = deleted_nodes.popInt();
            return nid;
        }

        int nid = num_nodes++;
        nodes.add(new TreeNode());
        return nid;
    }

    /**
     * get the depth from current node to root
     * 
     * @param nid       node id
     * @return          depth 
     */
    public int getDetph(int nid) {
        int depth = 0;

        TreeNode n = nodes.get(nid);
        while (!n.isRoot()) {
            n = nodes.get(n.getParent());
            depth++;
        }
        return depth;
    }

}
