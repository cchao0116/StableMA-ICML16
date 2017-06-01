package code.sma.recmmd.standalone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.Test;

import code.sma.core.impl.Tuples;
import code.sma.main.Configures;
import code.sma.thread.SimpleLearner;
import code.sma.thread.SimpleTaskMsgDispatcherImpl;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.util.ConfigureUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: RegSVDTest.java, v 0.1 2017年6月1日 下午2:08:30 Chao.Chen Exp $
 */
public class RegSVDTest {
    /** the logger instance*/
    protected final static Logger logger = Logger.getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    @Test
    public void test() {
        Configures conf = ConfigureUtil.read("src/main/resources/samples/RSVD.properties");
        String[] rootDirs = conf.getProperty("ROOT_DIRs").split("\\,");

        for (String rootDir : rootDirs) {
            LoggerUtil.info(logger, "1. loading " + rootDir);
            conf.setProperty("ROOT_DIR", rootDir);
            String trainFile = rootDir + "trainingset";
            String testFile = rootDir + "testingset";

            String algName = conf.getProperty("ALG_NAME");
            LoggerUtil.info(logger, "2. running " + algName);

            TaskMsgDispatcher stkmImpl = new SimpleTaskMsgDispatcherImpl(conf);
            int threadNum = ((Float) conf.get("THREAD_NUMBER_VALUE")).intValue();

            Tuples tnMatrix = MatrixFileUtil.reads(trainFile);
            Tuples tttMatrix = MatrixFileUtil.reads(testFile);

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
