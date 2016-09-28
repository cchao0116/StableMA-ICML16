package code.sma.thread;

import code.sma.datastructure.MatlabFasionSparseMatrix;

/**
 * The learning task dispatcher
 * 
 * @author Chao.Chen
 * @version $Id: TaskMsgDispacher.java, v 0.1 2016年9月26日 下午4:11:20 Chao.Chen Exp $
 */
public interface TaskMsgDispatcher {

    /**
     * dispatch learning task
     * 
     * @return configured model 
     */
    public Object map();

    /**
     * merge results
     * 
     * @param recmmd    the resulting recommender model
     * @param tnMatrix  the training data
     * @param ttMatrix  the testing data
     */
    public void reduce(Object recmmd, MatlabFasionSparseMatrix tnMatrix,
                       MatlabFasionSparseMatrix ttMatrix);

}
