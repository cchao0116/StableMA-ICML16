package code.sma.main;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import code.sma.dpncy.AbstractDpncyChecker;
import code.sma.dpncy.ModelDpncyChecker;
import code.sma.plugin.NetflixMovieLensDiscretizer;
import code.sma.plugin.Plugin;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.ensemble.MultTskREC;
import code.sma.recmmd.ensemble.WEMAREC;
import code.sma.recmmd.standalone.GroupSparsityMF;
import code.sma.recmmd.standalone.MFRecommender;
import code.sma.recmmd.standalone.RegSVD;
import code.sma.recmmd.standalone.StableMA;
import code.sma.util.SerializeUtil;
import code.sma.util.StringUtil;

/**
 * stand-alone recommender algorithm Factory class
 * 
 * @author Chao.Chen
 * @version $Id: RecommenderFactory.java, v 0.1 2016年9月27日 下午1:18:06 Chao.Chen Exp $
 */
public final class RecommenderFactory {
    private RecommenderFactory() {
    };

    public static Recommender instance(String algName, Configures conf) {
        if (StringUtil.equalsIgnoreCase(algName, "RegSVD")) {
            // Improving Regularized Singular Value Decomposition Collaborative Filtering
            return new RegSVD(conf, null);
        } else if (StringUtil.equalsIgnoreCase(algName, "SMA")) {
            AbstractDpncyChecker checker = new ModelDpncyChecker();
            checker.handler(conf);

            // Stable Matrix Approximation
            Map<String, Plugin> plugins = new HashMap<String, Plugin>();
            plugins.put("AUXILIARY_RCMMD_MODEL", (MFRecommender) SerializeUtil
                .readObject(conf.getProperty("AUXILIARY_RCMMD_MODEL_PATH")));

            return new StableMA(conf, plugins);
        } else if (StringUtil.equalsIgnoreCase(algName, "GSMF")) {
            // Recommendation by Mining Multiple User Behaviors with Group Sparsity
            return new GroupSparsityMF(conf, null);
        } else if (StringUtil.equalsIgnoreCase(algName, "WEMAREC")) {
            // WEMAREC: Accurate and Scalable Recommendation through Weighted and Ensemble Matrix Approximation
            String rootDir = (String) conf.get("ROOT_DIR");
            String[] cDirStrs = ((String) conf.get("CLUSTERING_SET")).split("\\,");

            Queue<String> clusterDirs = new LinkedList<String>();
            for (String cDirStr : cDirStrs) {
                String clusterDir = rootDir + cDirStr + File.separator;
                clusterDirs.add(clusterDir);
            }
            Map<String, Plugin> plugins = new HashMap<String, Plugin>();
            plugins.put("DISCRETIZER", new NetflixMovieLensDiscretizer(conf));

            return new WEMAREC(conf, plugins, clusterDirs);
        } else if (StringUtil.equalsIgnoreCase(algName, "MTREC")) {
            AbstractDpncyChecker checker = new ModelDpncyChecker();
            checker.handler(conf);

            Map<String, Plugin> plugins = new HashMap<String, Plugin>();
            plugins.put("DISCRETIZER", new NetflixMovieLensDiscretizer(conf));
            plugins.put("AUXILIARY_RCMMD_MODEL", (MFRecommender) SerializeUtil
                .readObject(conf.getProperty("AUXILIARY_RCMMD_MODEL_PATH")));

            return new MultTskREC(conf, plugins);
        } else {
            return null;
        }
    }
}
