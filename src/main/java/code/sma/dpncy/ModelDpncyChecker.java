package code.sma.dpncy;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.main.Configures;
import code.sma.main.RecommenderFactory;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.standalone.MatrixFactorizationRecommender;
import code.sma.util.FileUtil;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;
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
        RecConfigEnv rce = new RecConfigEnv(conf);
        auxRcmmdPath = parseModelParameter(rce, auxRcmmdPath);
        conf.setProperty("AUXILIARY_RCMMD_MODEL_PATH", auxRcmmdPath);

        MatrixFactorizationRecommender auxRec = null;
        if (FileUtil.exists(auxRcmmdPath)) {
            auxRec = (MatrixFactorizationRecommender) SerializeUtil.readObject(auxRcmmdPath);
        }

        // failed in reading the object or these object doesn't exist
        if (auxRec == null) {
            LoggerUtil.info(normalLogger, "...check...missing: " + auxRcmmdPath);

            String rootDir = conf.getProperty("ROOT_DIR");
            String trainFile = rootDir + "trainingset";
            MatlabFasionSparseMatrix tnMatrix = MatrixFileUtil.reads(trainFile);

            auxRec = (MatrixFactorizationRecommender) RecommenderFactory
                .instance((String) rce.get("ALG_NAME"), rce);
            auxRec.buildModel(tnMatrix, null);
            SerializeUtil.writeObject(auxRec, auxRcmmdPath);
        }

        // debug information
        if (normalLogger.isDebugEnabled()) {
            String rootDir = conf.getProperty("ROOT_DIR");
            String testFile = rootDir + "testingset";
            MatlabFasionSparseMatrix ttMatrix = MatrixFileUtil.reads(testFile);
            LoggerUtil.debug(normalLogger,
                String.format("ModelDpncyChecker:%s", auxRec.evaluate(ttMatrix).printOneLine()));
        }

        if (this.successor != null) {
            this.successor.handler(conf);
        } else {
            LoggerUtil.info(normalLogger, "...check...passed");
        }
    }

    protected String parseModelParameter(RecConfigEnv rce, String auxRcmmdPath) {
        //auxRcmmdPath: ../[ALG]_[MAX_ITERATION]_[LEARNING_RATE]_[REGULIRIZER].OBJ
        int begConf = auxRcmmdPath.lastIndexOf("/[") + 2;
        int endConf = auxRcmmdPath.lastIndexOf("].OBJ");

        String[] conf = auxRcmmdPath.substring(begConf, endConf).split("\\]_\\[");
        rce.put("ALG_NAME", conf[0].trim());
        rce.put("MAX_ITERATION_VALUE", Double.valueOf(conf[1].trim()).doubleValue());
        rce.put("LEARNING_RATE_VALUE", Double.valueOf(conf[2].trim()).doubleValue() * 0.001);
        rce.put("REGULAIZED_VALUE", Double.valueOf(conf[3].trim()).doubleValue() * 0.001);
        rce.put("VERBOSE_BOOLEAN", false);

        // ../[ALG]_[FEATURE_COUNT]_[MAX_ITERATION]_[LEARNING_RATE]_[REGULIRIZER].OBJ
        return String.format("%s%s]_[%d]_[%s]_[%s]_[%s].OBJ", auxRcmmdPath.substring(0, begConf),
            conf[0].trim(), ((Double) rce.get("FEATURE_COUNT_VALUE")).intValue(), conf[1].trim(),
            conf[2].trim(), conf[3].trim());
    }
}
