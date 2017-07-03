package code.sma.recmmd.tree.gbm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.core.DataElem;
import code.sma.core.TreeNode;
import code.sma.core.TreeNode.NodeStat;
import code.sma.core.impl.CRefVector;
import code.sma.main.Configures;
import code.sma.model.AbstractModel;
import code.sma.model.GBTreeModel;
import code.sma.plugin.Plugin;
import code.sma.recmmd.RuntimeEnv;
import code.sma.recmmd.tree.gbm.CSRMImage.CSREntry;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Trainer for Classification and Regression Tree (CaRT)
 * 
 * @author Chao.Chen
 * @version $Id: GBMachine.java, v 0.1 Jun 28, 2017 11:56:39 AM$
 */
public class CARTBooster implements Booster {
    /** Runtime environment*/
    public RuntimeEnv      runtimes;
    /** Task list*/
    protected Queue<Task>  tasks;

    /** tree model*/
    private GBTreeModel    tree;
    /** the gradients of each training data*/
    private FloatArrayList grad;
    /** the hessians of each training data*/
    private FloatArrayList hess;

    public CARTBooster(Configures conf, Map<String, Plugin> plugins) {
        runtimes = new RuntimeEnv(conf);
        runtimes.plugins = plugins;
        tasks = new LinkedList<Task>();
    }

    /** 
     * @see code.sma.recmmd.tree.gbm.Booster#doBoost(code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix, it.unimi.dsi.fastutil.floats.FloatArrayList, it.unimi.dsi.fastutil.floats.FloatArrayList)
     */
    @Override
    public AbstractModel doBoost(AbstractMatrix train, AbstractMatrix test, FloatArrayList grad,
                                 FloatArrayList hess) {
        this.grad = grad;
        this.hess = hess;
        this.tree = new GBTreeModel(runtimes.conf);

        // prepare run-times
        initTasks(train, test);

        Task next = null;
        while ((next = nextTask()) != null) {
            expand(next, train, test);
        }

        return tree;
    }

    protected void initTasks(AbstractMatrix train, AbstractMatrix test) {
        assert train != null : "Training data cannot be null.";

        // prepare run-times
        boolean[] acc_ufi = runtimes.acc_uf_indicator;
        boolean[] acc_ifi = runtimes.acc_if_indicator;

        runtimes.itrain = (acc_ufi == null && acc_ifi == null) ? (AbstractIterator) train.iterator()
            : (AbstractIterator) train.iteratorJion(acc_ufi, acc_ifi);
        runtimes.itest = (test == null) ? null
            : ((acc_ufi == null && acc_ifi == null) ? (AbstractIterator) test.iterator()
                : (AbstractIterator) test.iteratorJion(acc_ufi, acc_ifi));
        runtimes.nnz = runtimes.itrain.get_num_row();

        {
            //maintain a list of row indices, which indicate the members in the group
            int num_row = runtimes.nnz;
            IntArrayList ids = new IntArrayList(num_row);
            for (int i = 0; i < num_row; i++) {
                ids.set(i, i);
            }
            addTask(new Task(0, ids));
        }
    }

    protected void addTask(Task tsk) {
        tasks.add(tsk);
    }

    protected Task nextTask() {
        return tasks.poll();
    }

    /**
     * Find split for current task.
     * 
     * @param tsk       task information       
     * @param train     training data 
     * @param test      testing data
     */
    protected void expand(Task tsk, AbstractMatrix train, AbstractMatrix test) {
        // if reach maximum depth, make leaf from current node
        int depth = tree.getDetph(tsk.nid);
        if (depth > runtimes.maxDepth) {
            makeLeaf(tsk, 0.0, 0.0, false);
            return;
        }

        //In GBM, all features are seen as user-factors, 
        //EXT: the group/leaf information/index is treated as item-factors
        double rsum_grad = 0.0d, rsum_hess = 0.0d; // the statistics of current node
        CSRMImage image = new CSRMImage(runtimes.conf.getInteger("MAX_NUM_FEATURE"));
        {
            // 1. compute number of samples belonging to each feature
            int length = tsk.idset.size();
            for (int l = 0; l < length; l++) {
                int row_id = tsk.idset.getInt(l);
                rsum_grad += this.grad.getFloat(row_id);
                rsum_hess += this.hess.getFloat(row_id);

                CRefVector index_user = train.rowRef(row_id).getIndex_user();
                for (int u = 0; u < index_user.length(); u++) {
                    image.addBudget(index_user.intValue(u));
                }
            }
            // 1+. check whether meet the minimum split requirement
            if (rsum_hess <= 2.0 * runtimes.minChildNum) {
                makeLeaf(tsk, rsum_grad, rsum_hess, true);
                return;
            }

            //2. initialize storage
            image.buildStorage();

            //3. add data element
            for (int l = 0; l < length; l++) {
                int row_id = tsk.idset.getInt(l);

                DataElem e = train.rowRef(row_id);
                CRefVector index_user = e.getIndex_user();
                CRefVector value_ufactor = e.getValue_ufactor();
                for (int u = 0; u < index_user.length(); u++) {
                    image.addElement(row_id, index_user.intValue(u), value_ufactor.floatValue(u));
                }
            }
        }

        // find the best split which has the greatest information gain
        double root_cost = runtimes.regType.calcCost(rsum_grad, rsum_hess);
        GBTreeSelector sGlobal = new GBTreeSelector();
        // enumerate active feature index
        for (int fIndx : image.getAcctvInd()) {
            List<CSREntry> fEntries = image.getFeatureRef(fIndx);

            // sort the entries in terms of factor value
            Collections.sort(fEntries);
            enumerate(sGlobal, fIndx, fEntries, rsum_grad, rsum_hess, root_cost);
        }

        if (sGlobal.largestChg() > runtimes.rt_epsilon) {
            // allow to split
            double baseWeight = runtimes.regType.calcWeight(rsum_grad, rsum_hess);
            makeSplit(tsk, sGlobal, baseWeight);
        } else {
            makeLeaf(tsk, rsum_grad, rsum_hess, true);
        }

    }

    /**
     * enumerate all possible split point to find the best split
     */
    protected void enumerate(GBTreeSelector sGlobal, int fIndx, List<CSREntry> fEntries,
                             double rsum_grad, double rsum_hess, double root_cost) {
        GBTreeSelector sLocal = new GBTreeSelector();
        double csum_grad = 0.0, csum_hess = 0.0;

        int length = fEntries.size();
        for (int i = 0; i < length; i++) {
            CSREntry entry = fEntries.get(i);
            int rIndex = entry.rIndex();
            csum_grad += grad.getFloat(rIndex);
            csum_hess += hess.getFloat(rIndex);
            if (csum_hess < runtimes.minChildNum) {
                continue;
            }

            // check for split
            if (i == length - 1 || fEntries.get(i).fValue() + runtimes.rt_epsilon * 2 < fEntries
                .get(i + 1).fValue()) {
                //skip if two neighbor data are too close 
                double dsum_hess = rsum_hess - csum_hess;
                if (dsum_hess < runtimes.minChildNum) {
                    break;
                }

                double loss_chg = runtimes.regType.calcCost(csum_grad, csum_hess)
                                  + runtimes.regType.calcCost(rsum_grad - csum_grad, dsum_hess)
                                  - root_cost;
                sLocal.push(fEntries, loss_chg, fIndx, i,
                    i == length - 1 ? entry.fValue() + runtimes.rt_epsilon
                        : 0.5 * (fEntries.get(i).fValue() + fEntries.get(i + 1).fValue()));
            }
        }

        sGlobal.push(sLocal);
    }

    /** 
     * make split for current task, re-arrange positions in idset
     * 
     * @param tsk   task information
     */
    protected void makeSplit(Task tsk, GBTreeSelector sGlobal, double baseWeight) {
        // update non-leaf node and its statistics
        NodeStat ns = tree.get(tsk.nid).getNodeStat();
        ns.leaf_child_cnt = 0;
        ns.base_weight = (float) baseWeight;
        ns.loss_chg = sGlobal.largestChg();

        sGlobal.select(tree.get(tsk.nid));

        // make child nodes
        tree.addChildren(tsk.nid);

        // row members in the left tree node
        IntList memb_lnode = new IntArrayList(sGlobal.split_index);
        for (int i = 0; i < sGlobal.split_index; i++) {
            memb_lnode.add(sGlobal.ref_entries.get(i).rIndex());
        }
        Collections.sort(memb_lnode);

        // assume the list is sorted 
        IntList idset = tsk.idset;
        // move the members of right tree node in left
        // in merge-sort fashion
        for (int i = 0, top = 0; i < idset.size(); i++) {
            if (top < memb_lnode.size()) {
                if (idset.getInt(i) != memb_lnode.getInt(top)) {
                    idset.set(i - top, idset.getInt(i));
                } else {
                    top++;
                }
            } else {
                // here, top == memb_lnode.size()
                idset.set(i - top, idset.getInt(i));
            }
        }

        TreeNode n = tree.get(tsk.nid);
        Task left = new Task(n.left,
            idset.subList(idset.size() - sGlobal.split_index, idset.size()));
        Task right = new Task(n.right, idset.subList(0, idset.size() - sGlobal.split_index));

        // re-store left tree members
        for (int i = 0; i < memb_lnode.size(); i++) {
            left.idset.set(i, memb_lnode.getInt(i));
        }

        addTask(left);
        addTask(right);
    }

    /**
     * make leaf for current node
     * 
     * @param tsk        task information
     * @param sum_grad   sum of gradients
     * @param sum_hess   sum of hessians
     */
    protected void makeLeaf(Task tsk, double sum_grad, double sum_hess, boolean isComputed) {
        if (!isComputed) {
            int length = tsk.idset.size();
            for (int l = 0; l < length; l++) {
                int row_id = tsk.idset.getInt(l);

                sum_grad += this.grad.getFloat(row_id);
                sum_hess += this.hess.getFloat(row_id);
            }
        }

        // make leaf
        tree.get(tsk.nid)
            .setLeaf(runtimes.learningRate * runtimes.regType.calcWeight(sum_grad, sum_hess));
        tryPruneLeaf(tsk.nid, tree.getDetph(tsk.nid));
    }

    /**
     * try to prune off current leaf, return true if successful
     * 
     * @param tsk   task information
     */
    protected void tryPruneLeaf(int nid, int depth) {
        if (tree.get(nid).isRoot())
            return;

        int pid = tree.get(nid).getParent();
        NodeStat n = tree.get(pid).getNodeStat();
        n.leaf_child_cnt++;

        if (n.leaf_child_cnt >= 2 && n.loss_chg <= runtimes.minSplitLoss) {
            tree.change2Leaf(pid, (float) (runtimes.learningRate * n.base_weight));
            tryPruneLeaf(pid, depth - 1);
        }
    }

    /**
     * training task, element of single task
     * 
     * @author Chao.Chen
     * @version $Id: CARTBooster.java, Jun 30, 2017 5:24:14 PM$
     */
    private class Task {
        /** id of tree node*/
        public int     nid;
        /** the set of row IDs contained in one group, namely leaf*/
        public IntList idset;

        public Task(int nid, IntList idset) {
            this.nid = nid;
            this.idset = idset;
        }
    }

    /**
     * selector of regression-tree to find the suitable candidate
     * 
     * @author Chao.Chen
     * @version $Id: CARTBooster.java, Jun 30, 2017 5:25:00 PM$
     */
    private class GBTreeSelector {
        /**  information gain*/
        private float         loss_chg;

        /** feature index*/
        public int            index_feature;
        /** REF: a point to all data of one feature*/
        public List<CSREntry> ref_entries;

        // Tree node basics
        public int            split_index;
        public float          split_cond;

        public void push(List<CSREntry> ref_entries, double loss_chg, int index_feature,
                         int split_index, double split_cond) {
            if (loss_chg > this.loss_chg) {
                this.ref_entries = ref_entries;
                this.loss_chg = (float) loss_chg;
                this.index_feature = index_feature;
                this.split_index = split_index;
                this.split_cond = (float) split_cond;
            }
        }

        public void push(GBTreeSelector s) {
            if (s.loss_chg > this.loss_chg) {
                this.ref_entries = s.ref_entries;
                this.loss_chg = s.loss_chg;
                this.index_feature = s.index_feature;
                this.split_index = s.split_index;
                this.split_cond = s.split_cond;
            }
        }

        public void select(TreeNode node) {
            node.setSplit(split_index, split_cond);
        }

        public float largestChg() {
            return loss_chg;
        }
    }
}
