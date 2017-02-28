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

            RecConfigEnv rce = new RecConfigEnv(conf);
            auxRec = (MatrixFactorizationRecommender) RecommenderFactory
                .instance((String) rce.get("ALG_NAME"), rce);
            auxRec.buildModel(tnMatrix, null);
            SerializeUtil.writeObject(auxRec, auxRcmmdPath);
        }

        if (this.successor != null) {
            this.successor.handler(conf);
        } else {
            LoggerUtil.info(normalLogger, "...check...passed");
        }
    }

    protected void parseModelParameter(RecConfigEnv rce, String auxRcmmdPath) {
        //auxRcmmdPath: ../[ALG]_[LEARNING_RATE]_[REGULIRIZER].OBJ
        int begConf = auxRcmmdPath.lastIndexOf("/]") + 1;
        int endConf = auxRcmmdPath.lastIndexOf("].OBJ");

        String[] conf = auxRcmmdPath.substring(begConf, endConf).split("]_[");
        rce.put("ALG_NAME", conf[0].trim());
        rce.put("LEARNING_RATE_VALUE", Double.valueOf(conf[1].trim()).doubleValue() * 0.001);
        rce.put("REGULAIZED_VALUE", Double.valueOf(conf[1].trim()).doubleValue() * 0.001);
        rce.put("VERBOSE_BOOLEAN", false);
    }
}
