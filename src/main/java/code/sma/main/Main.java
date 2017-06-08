package code.sma.main;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import code.sma.core.impl.DenseVector;
import code.sma.core.impl.Tuples;
import code.sma.dpncy.AbstractDpncyChecker;
import code.sma.dpncy.ClusteringDpncyChecker;
import code.sma.thread.SimpleLearner;
import code.sma.thread.SimpleTaskMsgDispatcherImpl;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.util.ConfigureUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixIOUtil;
import code.sma.util.StringUtil;

/**
 * This class implemented MA-based methods.
 * The parameters for algorithm and data is in src/java/resources/rcmd.properties
 * 
 * @author Chao Chen
 * @version $Id: MABasedParallel.java, v 0.1 Feb 4, 2016 4:26:03 PM chench Exp $
 */
public class Main {

    /** the logger instance*/
    protected final static Logger logger = Logger.getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            //load dataset configure file
            Configures conf = ConfigureUtil.read("src/main/resources/rcmd.properties");
            String[] rootDirs = conf.getProperty("ROOT_DIRs").split("\\,");

            for (String rootDir : rootDirs) {
                LoggerUtil.info(logger, "1. loading " + rootDir);
                conf.setProperty("ROOT_DIR", rootDir);
                String trainFile = rootDir + "trainingset";
                String testFile = rootDir + "testingset";
                String dconfFile = rootDir + "dConfig.properties";
                ConfigureUtil.addConfig(conf, dconfFile);

                String algName = conf.getProperty("ALG_NAME");
                LoggerUtil.info(logger, "2. running " + algName);
                if (StringUtil.isBlank(algName)) {
                    continue;
                } else if (StringUtil.equalsIgnoreCase(algName, "WEMAREC")) {
                    AbstractDpncyChecker checker = new ClusteringDpncyChecker();
                    checker.handler(conf);

                    Tuples train = MatrixIOUtil.loadTuples(trainFile,
                        ((Float) conf.get("TRAIN_VAL_NUM_VALUE")).intValue());
                    Tuples test = MatrixIOUtil.loadTuples(testFile,
                        ((Float) conf.get("TEST_VAL_NUM_VALUE")).intValue());
                    Configures lconf = new Configures(conf);
                    RecommenderFactory.instance(algName, lconf).buildModel(train, test);
                } else if (StringUtil.equalsIgnoreCase(algName, "GBMA")) {
                    int threadNum = ((Double) conf.get("THREAD_NUMBER_VALUE")).intValue();
                    int userCount = ((Double) conf.get("USER_COUNT_VALUE")).intValue();
                    int itemCount = ((Double) conf.get("ITEM_COUNT_VALUE")).intValue();

                    Tuples train = MatrixIOUtil.loadTuples(trainFile,
                        ((Float) conf.get("TRAIN_VAL_NUM_VALUE")).intValue());
                    Tuples test = MatrixIOUtil.loadTuples(testFile,
                        ((Float) conf.get("TEST_VAL_NUM_VALUE")).intValue());

                    DenseVector avgUser = new DenseVector(userCount);
                    DenseVector avgItem = new DenseVector(itemCount);
                    avgRatingAndAdjustData(train, userCount, itemCount, avgUser, avgItem);
                    conf.setVector("AVG_USER", avgUser);
                    conf.setVector("AVG_ITEM", avgItem);

                    TaskMsgDispatcher stkmImpl = new SimpleTaskMsgDispatcherImpl(conf);
                    try {
                        ExecutorService exec = Executors.newCachedThreadPool();
                        for (int t = 0; t < threadNum; t++) {
                            exec.execute(new SimpleLearner(stkmImpl, train, test));
                        }
                        exec.shutdown();
                        exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
                    } catch (InterruptedException e) {
                        ExceptionUtil.caught(e, "Stand-alone model Thead!");
                    }

                } else {
                    TaskMsgDispatcher stkmImpl = new SimpleTaskMsgDispatcherImpl(conf);
                    int threadNum = ((Double) conf.get("THREAD_NUMBER_VALUE")).intValue();

                    Tuples train = MatrixIOUtil.loadTuples(trainFile,
                        ((Float) conf.get("TRAIN_VAL_NUM_VALUE")).intValue());
                    Tuples test = MatrixIOUtil.loadTuples(testFile,
                        ((Float) conf.get("TEST_VAL_NUM_VALUE")).intValue());

                    try {
                        ExecutorService exec = Executors.newCachedThreadPool();
                        for (int t = 0; t < threadNum; t++) {
                            exec.execute(new SimpleLearner(stkmImpl, train, test));
                        }
                        exec.shutdown();
                        exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
                    } catch (InterruptedException e) {
                        ExceptionUtil.caught(e, "Stand-alone model Thead!");
                    }
                }
            }
        } catch (IOException e) {
            ExceptionUtil.caught(e, "FILE: src/main/resources/rcmd.properties");
        }
    }

    protected static void avgRatingAndAdjustData(Tuples tnMatrix, int userCount, int itemCount,
                                                 DenseVector avgUser, DenseVector avgItem) {
        int rateCount = tnMatrix.getNnz();
        int[] uIndx = tnMatrix.getRowIndx();
        int[] iIndx = tnMatrix.getColIndx();
        float[] Auis = tnMatrix.getVals();

        // user average rating
        {
            int[] uRatingCount = new int[userCount];
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int u = uIndx[numSeq];
                avgUser.setValue(u, avgUser.floatValue(u) + Auis[numSeq]);
                uRatingCount[u]++;
            }

            for (int u = 0; u < userCount; u++) {
                if (uRatingCount[u] == 0) {
                    continue;
                }
                avgUser.setValue(u, avgUser.floatValue(u) / uRatingCount[u]);
            }
        }

        // item average rating
        {
            int[] iRatingCount = new int[itemCount];
            for (int numSeq = 0; numSeq < rateCount; numSeq++) {
                int i = iIndx[numSeq];
                avgItem.setValue(i, avgItem.floatValue(i) + Auis[numSeq]);
                iRatingCount[i]++;
            }

            for (int i = 0; i < itemCount; i++) {
                if (iRatingCount[i] == 0) {
                    continue;
                }
                avgItem.setValue(i, avgItem.floatValue(i) / iRatingCount[i]);
            }
        }

        // re-adjust training data
        for (int numSeq = 0; numSeq < rateCount; numSeq++) {
            int u = uIndx[numSeq];
            int i = iIndx[numSeq];
            Auis[numSeq] = (float) (Auis[numSeq]
                                    - (avgUser.floatValue(u) + avgItem.floatValue(i)) / 2.0);
        }
    }

}
