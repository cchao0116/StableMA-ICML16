package code.sma.main;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import code.sma.dpncy.NetflixMovieLensDiscretizer;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.Recommender;
import code.sma.recmmd.ensemble.MultTskREC;
import code.sma.recmmd.ensemble.WEMAREC;
import code.sma.recmmd.rank.SMARank;
import code.sma.recmmd.standalone.GroupSparsityMF;
import code.sma.recmmd.standalone.RegularizedSVD;
import code.sma.recmmd.standalone.StableMA;
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

    public static Recommender instance(String algName, RecConfigEnv rce) {
        if (StringUtil.equalsIgnoreCase(algName, "RegSVD")) {
            // Improving Regularized Singular Value Decomposition Collaborative Filtering
            return new RegularizedSVD(rce);
        } else if (StringUtil.equalsIgnoreCase(algName, "SMA")) {
            // Stable Matrix Approximation
            return new StableMA(rce);
        } else if (StringUtil.equalsIgnoreCase(algName, "GSMF")) {
            // Recommendation by Mining Multiple User Behaviors with Group Sparsity
            return new GroupSparsityMF(rce);
        } else if (StringUtil.equalsIgnoreCase(algName, "WEMAREC")) {
            // WEMAREC: Accurate and Scalable Recommendation through Weighted and Ensemble Matrix Approximation
            String rootDir = (String) rce.get("ROOT_DIR");
            String[] cDirStrs = ((String) rce.get("CLUSTERING_SET")).split("\\,");

            Queue<String> clusterDirs = new LinkedList<String>();
            for (String cDirStr : cDirStrs) {
                String clusterDir = rootDir + cDirStr + File.separator;
                clusterDirs.add(clusterDir);
            }

            return new WEMAREC(rce, new NetflixMovieLensDiscretizer(rce), clusterDirs);
        } else if (StringUtil.equalsIgnoreCase(algName, "MTREC")) {
            // unpublished
            return new MultTskREC(rce);
        } else if (StringUtil.equalsIgnoreCase(algName, "SMARank")) {
            // unpublished
            return new SMARank(rce);
        } else {
            return null;
        }
    }
}
