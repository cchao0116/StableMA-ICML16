package code.sma.recmmd.cf.ma.ensemble;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;

import code.sma.core.AbstractMatrix;
import code.sma.eval.CollaFiltrMetrics;
import code.sma.eval.EvaluationMetrics;
import code.sma.main.Configures;
import code.sma.main.RecommenderFactory;
import code.sma.recmmd.cf.ma.ensemble.EnsembleFactorRecmmder;
import code.sma.util.ConfigureUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixIOUtil;
import code.sma.util.StringUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: AbstractEnsTest.java, v 0.1 2017年6月13日 下午1:22:54 Chao.Chen Exp $
 */
public abstract class AbstractEnsTest {
    /** the logger instance*/
    protected final static Logger logger = Logger.getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /**
     * Get the configure file 
     * @return
     */
    protected abstract String getConfig();

    @Test
    public void testAlg() throws IOException {
        Configures conf = ConfigureUtil.read(getConfig());
        String[] rootDirs = conf.getProperty("ROOT_DIRs").split("\\,");

        for (String rootDir : rootDirs) {
            rootDir = StringUtil.trim(rootDir);

            LoggerUtil.info(logger, "1. loading " + rootDir);
            conf.setProperty("ROOT_DIR", rootDir);
            String trainFile = rootDir + "trainingset";
            String testFile = rootDir + "testingset";

            Configures new_conf = new Configures(conf);
            String dconfFile = rootDir + "dConfig.properties";
            ConfigureUtil.addConfig(new_conf, dconfFile);

            String algName = new_conf.getProperty("ALG_NAME");
            LoggerUtil.info(logger, "2. loading data ");

            AbstractMatrix train = MatrixIOUtil.loadCSRMatrix(trainFile,
                new_conf.getInteger("TRAIN_ROW_NUM_VALUE"),
                new_conf.getInteger("TRAIN_VAL_NUM_VALUE"));
            AbstractMatrix test = MatrixIOUtil.loadCSRMatrix(testFile,
                new_conf.getInteger("TEST_ROW_NUM_VALUE"),
                new_conf.getInteger("TEST_VAL_NUM_VALUE"));
            LoggerUtil.info(logger, "3. running " + algName);

            EnsembleFactorRecmmder recmmd = (EnsembleFactorRecmmder) RecommenderFactory
                .instance(algName, new_conf);
            recmmd.buildModel(train, test);

            EvaluationMetrics m = new CollaFiltrMetrics();
            m.evalRating(recmmd, recmmd.runtimes.itest);
            LoggerUtil.info(logger, String.format("%s:%s", recmmd, m.printOneLine()));
        }
    }
}
