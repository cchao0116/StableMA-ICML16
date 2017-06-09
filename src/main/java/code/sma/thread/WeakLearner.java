package code.sma.thread;

import code.sma.core.AbstractMatrix;
import code.sma.recmmd.Recommender;

/**
 * the general thread learner for recommender system
 * 
 * @author Chao.Chen
 * @version $Id: WeakLearner.java, v 0.1 2016年7月21日 下午1:05:21 Chao.Chen Exp $
 */
public class WeakLearner extends Thread {
    /** learning task dispatcher*/
    private TaskMsgDispatcher dispatcher;
    /** training data*/
    private AbstractMatrix    train;
    /** testing data*/
    private AbstractMatrix    test;

    /**
     * @param recmmnd       cf learner
     * @param train   training data
     * @param test    testing data
     */
    public WeakLearner(TaskMsgDispatcher dispatcher, AbstractMatrix train, AbstractMatrix test) {
        super();
        this.dispatcher = dispatcher;
        this.train = train;
        this.test = test;
    }

    /** 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        Recommender recmmnd = null;
        while ((recmmnd = (Recommender) dispatcher.map()) != null) {
            recmmnd.buildModel(train, test);
            dispatcher.reduce(recmmnd, train, test);
        }
    }

}
