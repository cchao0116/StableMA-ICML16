package code.sma.thread;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.recommender.Recommender;

/**
 * the general thread learner for recommender system
 * 
 * @author Chao.Chen
 * @version $Id: SimpleLearner.java, v 0.1 2016年7月21日 下午1:05:21 Chao.Chen Exp $
 */
public class SimpleLearner extends Thread {
    /** learning task dispatcher*/
    private TaskMsgDispatcher        dispatcher;
    /** training data*/
    private MatlabFasionSparseMatrix trainMatrix;
    /** testing data*/
    private MatlabFasionSparseMatrix testMatrix;

    /**
     * @param recmmnd       cf learner
     * @param trainMatrix   training data
     * @param testMatrix    testing data
     */
    public SimpleLearner(TaskMsgDispatcher dispatcher, MatlabFasionSparseMatrix trainMatrix,
                         MatlabFasionSparseMatrix testMatrix) {
        super();
        this.dispatcher = dispatcher;
        this.trainMatrix = trainMatrix;
        this.testMatrix = testMatrix;
    }

    /** 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        Recommender recmmnd = null;
        while ((recmmnd = dispatcher.map()) != null) {
            recmmnd.buildModel(trainMatrix, testMatrix);
            dispatcher.reduce(recmmnd, trainMatrix, testMatrix);
        }
    }

}
