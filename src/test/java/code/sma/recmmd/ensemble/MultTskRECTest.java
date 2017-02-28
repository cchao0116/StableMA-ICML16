package code.sma.recmmd.ensemble;

import org.apache.log4j.Logger;
import org.junit.Test;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.dpncy.AbstractDpncyChecker;
import code.sma.dpncy.ModelDpncyChecker;
import code.sma.main.Configures;
import code.sma.main.RecommenderFactory;
import code.sma.recmmd.RecConfigEnv;
import code.sma.util.ConfigureUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;
import code.sma.util.StringUtil;
import junit.framework.TestCase;

/**
 * 
 * @author Chao.Chen
 * @version $Id: MultTskREC.java, v 0.1 2017年2月28日 下午4:23:17 Chao.Chen Exp $
 */
public class MultTskRECTest extends TestCase {
    /** the logger instance*/
    protected final static Logger logger = Logger.getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /** 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testAlg() {
        Configures conf = ConfigureUtil.read("src/main/resources/samples/MTREC.properties");
        String[] rootDirs = conf.getProperty("ROOT_DIRs").split("\\,");

        for (String rootDir : rootDirs) {
            LoggerUtil.info(logger, "1. loading " + rootDir);
            conf.setProperty("ROOT_DIR", rootDir);
            String trainFile = rootDir + "trainingset";
            String testFile = rootDir + "testingset";

            String algName = conf.getProperty("ALG_NAME");
            LoggerUtil.info(logger, "2. running " + algName);

            if (StringUtil.equalsIgnoreCase(algName, "MTREC")) {
                AbstractDpncyChecker checker = new ModelDpncyChecker();
                checker.handler(conf);

                MatlabFasionSparseMatrix tnMatrix = MatrixFileUtil.reads(trainFile);
                MatlabFasionSparseMatrix tttMatrix = MatrixFileUtil.reads(testFile);
                RecConfigEnv rce = new RecConfigEnv(conf);
                RecommenderFactory.instance(algName, rce).buildModel(tnMatrix, tttMatrix);
            }
        }
    }

    /** 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {

    }

}
