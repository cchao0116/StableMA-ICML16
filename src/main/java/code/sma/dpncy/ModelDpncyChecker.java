package code.sma.dpncy;

import java.io.File;
import java.nio.file.Files;

import code.sma.core.AbstractIterator;
import code.sma.core.AbstractMatrix;
import code.sma.main.Configures;
import code.sma.main.RecommenderFactory;
import code.sma.recmmd.standalone.MFRecommender;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixIOUtil;
import code.sma.util.SerializeUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: ModelDpncyChecker.java, v 0.1 2017年2月28日 下午4:34:13 Chao.Chen Exp $
 */
public class ModelDpncyChecker extends AbstractDpncyChecker {

    /** 
     * @see code.sma.dpncy.AbstractDpncyChecker#handler(code.sma.main.Configures)
     */
    @Override
    public void handler(Configures conf) {
        String auxRcmmdPath = conf.getProperty("AUXILIARY_RCMMD_MODEL_PATH");
        Configures lconf = new Configures(conf);
        auxRcmmdPath = parseModelParameter(lconf, auxRcmmdPath);
        conf.setProperty("AUXILIARY_RCMMD_MODEL_PATH", auxRcmmdPath);

        MFRecommender auxRec = null;

        if (Files.exists((new File(auxRcmmdPath)).toPath())) {
            auxRec = (MFRecommender) SerializeUtil.readObject(auxRcmmdPath);
        }

        // failed in reading the object or these object doesn't exist
        if (auxRec == null) {
            LoggerUtil.info(normalLogger, "...check...missing: " + auxRcmmdPath);

            String rootDir = conf.getProperty("ROOT_DIR");
            String trainFile = rootDir + "trainingset";
            AbstractMatrix train = MatrixIOUtil.loadCSRMatrix(trainFile,
                conf.getInteger("TRAIN_ROW_NUM_VALUE"), conf.getInteger("TRAIN_VAL_NUM_VALUE"));

            auxRec = (MFRecommender) RecommenderFactory.instance(lconf.getProperty("ALG_NAME"),
                lconf);
            auxRec.buildModel(train, null);
            SerializeUtil.writeObject(auxRec, auxRcmmdPath);
        }

        // debug information
        if (normalLogger.isDebugEnabled()) {
            String rootDir = conf.getProperty("ROOT_DIR");
            String testFile = rootDir + "testingset";
            auxRec.runtimes.itest = (AbstractIterator) MatrixIOUtil.loadCSRMatrix(testFile,
                conf.getInteger("TEST_ROW_NUM_VALUE"), conf.getInteger("TEST_VAL_NUM_VALUE"))
                .iterator();
            LoggerUtil.debug(normalLogger,
                String.format("ModelDpncyChecker:%s", auxRec.evaluate(null).printOneLine()));
        }

        if (this.successor != null) {
            this.successor.handler(conf);
        } else {
            LoggerUtil.info(normalLogger, "...check...passed");
        }
    }

    protected String parseModelParameter(Configures lconf, String auxRcmmdPath) {
        //auxRcmmdPath: ../[ALG]_[MAX_ITERATION]_[LEARNING_RATE]_[REGULIRIZER].OBJ
        int begConf = auxRcmmdPath.lastIndexOf("/[") + 2;
        int endConf = auxRcmmdPath.lastIndexOf("].OBJ");

        String[] conf = auxRcmmdPath.substring(begConf, endConf).split("\\]_\\[");
        lconf.put("ALG_NAME", conf[0].trim());
        lconf.put("MAX_ITERATION_VALUE", Double.valueOf(conf[1].trim()).doubleValue());
        lconf.put("LEARNING_RATE_VALUE", Double.valueOf(conf[2].trim()).doubleValue() * 0.001);
        lconf.put("REGULAIZED_VALUE", Double.valueOf(conf[3].trim()).doubleValue() * 0.001);
        lconf.put("VERBOSE_BOOLEAN", false);

        // ../[ALG]_[FEATURE_COUNT]_[MAX_ITERATION]_[LEARNING_RATE]_[REGULIRIZER].OBJ
        return String.format("%s%s]_[%d]_[%s]_[%s]_[%s].OBJ", auxRcmmdPath.substring(0, begConf),
            conf[0].trim(), lconf.getInteger("FEATURE_COUNT_VALUE"), conf[1].trim(), conf[2].trim(),
            conf[3].trim());
    }
}
