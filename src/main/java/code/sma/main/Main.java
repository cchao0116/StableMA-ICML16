package code.sma.main;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.thread.SimpleLearner;
import code.sma.thread.SimpleTaskMsgDispatcherImpl;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.util.ConfigureUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;
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
        //load dataset configure file
        Configures conf = ConfigureUtil.read("src/main/resources/rcmd.properties");
        String[] rootDirs = conf.getProperty("ROOT_DIRs").split("\\,");

        for (String rootDir : rootDirs) {
            LoggerUtil.info(logger, "1. loading " + rootDir);
            String trainFile = rootDir + "trainingset";
            String testFile = rootDir + "testingset";
            MatlabFasionSparseMatrix tnMatrix = MatrixFileUtil.reads(trainFile);
            MatlabFasionSparseMatrix tttMatrix = MatrixFileUtil.reads(testFile);

            String algName = conf.getProperty("ALG_NAME");
            if (StringUtil.isBlank(algName)) {
                continue;
            } else if (StringUtil.equalsIgnoreCase(algName, "WEMAREC")) {

            } else {
                TaskMsgDispatcher stkmImpl = new SimpleTaskMsgDispatcherImpl(conf);
                int threadNum = ((Double) conf.get("THREAD_NUMBER_VALUE")).intValue();

                try {
                    ExecutorService exec = Executors.newCachedThreadPool();
                    for (int t = 0; t < threadNum; t++) {
                        exec.execute(new SimpleLearner(stkmImpl, tnMatrix, tttMatrix));
                    }
                    exec.shutdown();
                    exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    ExceptionUtil.caught(e, "Stand-alone model Thead!");
                }
            }
        }
    }

}
