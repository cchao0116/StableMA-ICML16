package code.sma.thread;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.main.Configures;
import code.sma.main.StandAloneRecommenderFactory;
import code.sma.recommender.RecConfigEnv;
import code.sma.recommender.Recommender;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;

/**
 * A simple implementation of Task Message Dispatcher
 * 
 * @author Chao.Chen
 * @version $Id: SimpleTaskMsgDispatcher.java, v 0.1 2016年9月27日 下午12:24:16 Chao.Chen Exp $
 */
public class SimpleTaskMsgDispatcherImpl implements TaskMsgDispatcher {

    /** the learning task buffer*/
    protected Queue<Recommender>  recmmdsBuffer;

    /** mutex using in map procedure*/
    protected static Object       MAP_MUTEX    = new Object();
    /** mutex using in reduce procedure*/
    protected static Object       REDUCE_MUTEX = new Object();

    protected final static Logger normalLogger = Logger
        .getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /**
     * Construction 
     * 
     * @param threadNum     the number of threads
     * @param tkEnv         the task environments
     */
    public SimpleTaskMsgDispatcherImpl(Configures conf) {
        super();
        recmmdsBuffer = new LinkedList<Recommender>();

        String algName = conf.getProperty("ALG_NAME");
        for (double featureCount : conf.getVector("FEATURE_COUNT_ARR")) {
            for (double learningRate : conf.getVector("LEARNING_RATE_ARR")) {
                for (double regulizer : conf.getVector("REGULAIZED_ARR")) {
                    for (double maxIter : conf.getVector("MAX_ITERATION_ARR")) {
                        RecConfigEnv rce = new RecConfigEnv();
                        rce.put("FEATURE_COUNT_VALUE", featureCount);
                        rce.put("LEARNING_RATE_VALUE", learningRate);
                        rce.put("REGULAIZED_VALUE", regulizer);
                        rce.put("MAX_ITERATION_VALUE", maxIter);

                        for (Object k : conf.keySet()) {
                            String key = (String) k;
                            if (key.endsWith("_VALUE") | key.endsWith("_BOOLEAN")) {
                                rce.put(key, conf.get(key));
                            }
                        }
                        recmmdsBuffer.add(StandAloneRecommenderFactory.instance(algName, rce));
                    }
                }
            }
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#map()
     */
    @Override
    public Recommender map() {
        synchronized (MAP_MUTEX) {
            return recmmdsBuffer.poll();
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#reduce(code.sma.recommender.Recommender)
     */
    @Override
    public void reduce(Recommender recmmd, MatlabFasionSparseMatrix tnMatrix,
                       MatlabFasionSparseMatrix ttMatrix) {
        LoggerUtil.info(normalLogger, (new StringBuilder(recmmd.toString()))
            .append(String.format("\tRMSE: %.6f", recmmd.evaluate(ttMatrix))));
    }

}
