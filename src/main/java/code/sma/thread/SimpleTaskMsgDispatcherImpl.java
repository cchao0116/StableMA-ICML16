package code.sma.thread;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import code.sma.core.impl.DenseVector;
import code.sma.core.impl.Tuples;
import code.sma.main.Configures;
import code.sma.main.RecommenderFactory;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.Recommender;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.StringUtil;

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

        List<String> suffArrKeys = new ArrayList<String>();
        List<DenseVector> suffArrVals = new ArrayList<DenseVector>();
        for (Object k : conf.keySet()) {
            String key = String.valueOf(k);
            if (key.endsWith("ARR")) {
                suffArrKeys
                    .add(StringUtil.reverse(StringUtil.reverse(key).replace("RRA", "EULAV")));
                suffArrVals.add(conf.getVector(key));
            }
        }

        if (!suffArrKeys.isEmpty()) {
            // Deep-First-Search: the ARR-end configure entries
            int maxLayer = suffArrKeys.size();
            int lastLayerWidth = suffArrVals.get(maxLayer - 1).length();
            List<Integer> nodes = new ArrayList<Integer>();
            nodes.add(0);

            int nextLayer = 0;
            while (!nodes.isEmpty()) {
                while (nextLayer < maxLayer - 1) {
                    nodes.add(0);
                    nextLayer++;
                }

                // store all possible configures
                for (int l = 0; l < lastLayerWidth; l++) {
                    RecConfigEnv rce = new RecConfigEnv(conf);
                    for (int c = 0; c < maxLayer - 1; c++) {
                        rce.put(suffArrKeys.get(c), suffArrVals.get(c).floatValue(nodes.get(c)));
                    }
                    rce.put(suffArrKeys.get(nextLayer), suffArrVals.get(nextLayer).floatValue(l));
                    recmmdsBuffer.add(RecommenderFactory.instance(algName, rce));
                }

                // trace back to next node
                while (nextLayer >= 0) {
                    nodes.remove(nextLayer);
                    nextLayer--;
                    if (nextLayer < 0) {
                        break;
                    }

                    int nextLayerPivot = nodes.get(nextLayer) + 1;
                    if (nextLayerPivot < suffArrVals.get(nextLayer).length()) {
                        nodes.set(nextLayer, nextLayerPivot);
                        break;
                    }
                }
            }
        } else {
            RecConfigEnv rce = new RecConfigEnv(conf);
            recmmdsBuffer.add(RecommenderFactory.instance(algName, rce));
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
     * @see code.sma.thread.TaskMsgDispatcher#reduce(code.sma.recmmd.Recommender)
     */
    @Override
    public void reduce(Object recmmd, Tuples tnMatrix, Tuples ttMatrix) {
        LoggerUtil.info(normalLogger, (new StringBuilder(recmmd.toString())).append(": ")
            .append((((Recommender) recmmd).evaluate(ttMatrix)).printOneLine()));
    }

}
