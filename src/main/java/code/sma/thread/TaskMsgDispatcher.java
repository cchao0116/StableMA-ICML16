package code.sma.thread;

import code.sma.recommender.Recommender;

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
    public abstract Recommender map();

    /**
     * merge results
     */
    public abstract void reduce(Recommender recmmd);

}
