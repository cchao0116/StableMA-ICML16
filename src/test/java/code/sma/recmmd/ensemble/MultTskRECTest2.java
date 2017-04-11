package code.sma.recmmd.ensemble;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import code.sma.datastructure.DenseMatrix;
import code.sma.datastructure.DenseVector;
import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.dpncy.AbstractDpncyChecker;
import code.sma.dpncy.ModelDpncyChecker;
import code.sma.main.Configures;
import code.sma.main.RecommenderFactory;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.standalone.MatrixFactorizationRecommender;
import code.sma.recmmd.standalone.RegularizedSVD;
import code.sma.util.ConfigureUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;
import code.sma.util.StringUtil;
import junit.framework.TestCase;

public class MultTskRECTest2 extends TestCase {

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
                MatlabFasionSparseMatrix ttMatrix = MatrixFileUtil.reads(testFile);
                RecConfigEnv rce = new RecConfigEnv(conf);
                Recommender rcmmd = RecommenderFactory.instance(algName, rce);
                rcmmd.buildModel(tnMatrix, ttMatrix);

                Recommender lrcmmd = null;
                {
                    DenseMatrix userDenseFeature = null;
                    DenseMatrix itemDenseFeatures = null;

                    // Here should be more considered!
                    List<Recommender> lRec = new ArrayList<Recommender>();
                    userDenseFeature = ((MatrixFactorizationRecommender) lRec
                        .get(0)).userDenseFeatures;
                    itemDenseFeatures = ((MatrixFactorizationRecommender) lRec
                        .get(0)).itemDenseFeatures;

                    // user
                    int userCount = Integer.valueOf(conf.getProperty("USER_COUNT_VALUE"));
                    int itemCount = Integer.valueOf(conf.getProperty("ITEM_COUNT_VALUE"));
                    int featureCount = Integer.valueOf(conf.getProperty("FEATURE_COUNT_VALUE"));
                    for (int u = 0; u < userCount; u++) {
                        int c = 0;
                        for (int l = 1; l < lRec.size(); l++) {
                            MatrixFactorizationRecommender lrc = (MatrixFactorizationRecommender) lRec
                                .get(l);
                            DenseVector uFeatre = lrc.userDenseFeatures.getRowRef(u);
                            if (uFeatre == null) {
                                continue;
                            }
                            c++;

                            for (int k = 0; k < featureCount; k++) {
                                userDenseFeature.setValue(u, k,
                                    userDenseFeature.getValue(u, k) + uFeatre.getValue(k), false);
                            }
                        }

                        for (int k = 0; k < featureCount; k++) {
                            userDenseFeature.setValue(u, k, userDenseFeature.getValue(u, k) / c,
                                false);
                        }
                    }

                    //item
                    for (int i = 0; i < itemCount; i++) {
                        int c = 0;
                        for (int l = 1; l < lRec.size(); l++) {
                            MatrixFactorizationRecommender lrc = (MatrixFactorizationRecommender) lRec
                                .get(l);
                            DenseVector iFeatre = lrc.itemDenseFeatures.getRowRef(i);
                            if (iFeatre == null) {
                                continue;
                            }
                            c++;

                            for (int k = 0; k < featureCount; k++) {
                                itemDenseFeatures.setValue(i, k,
                                    itemDenseFeatures.getValue(i, k) + iFeatre.getValue(k), false);
                            }
                        }

                        for (int k = 0; k < featureCount; k++) {
                            itemDenseFeatures.setValue(i, k, itemDenseFeatures.getValue(i, k) / c,
                                false);
                        }
                    }

                    lrcmmd = new RegularizedSVD(new RecConfigEnv(conf), userDenseFeature,
                        itemDenseFeatures);
                }

                LoggerUtil.info(logger,
                    String.format("%s\n%s\n%s", rcmmd.toString(),
                        rcmmd.evaluate(ttMatrix).printOneLine(),
                        lrcmmd.evaluate(ttMatrix).printOneLine()));
            }
        }
    }

    /** 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {

    }

}
