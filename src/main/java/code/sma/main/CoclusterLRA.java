package code.sma.main;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import code.sma.clustering.Cluster;
import code.sma.clustering.CoclusterUtil;
import code.sma.datastructure.SparseMatrix;
import code.sma.util.ClusterInfoUtil;
import code.sma.util.ConfigureUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;

/**
 * 
 * @author Hanke Chen
 * @version $Id: CoclusterLRA.java, v 0.1 2014-10-28 下午12:37:36 chench Exp $
 */
public class CoclusterLRA {
    //==========================
    //      Common variable
    //==========================
    public final static int[]    DIVERGENCE    = { CoclusterUtil.EUCLIDEAN_DIVERGENCE,
                                                   CoclusterUtil.I_DIVERGENCE };
    public final static String[] DIR           = { "EW", "IW" };
    public final static int[]    CONSTRAINTS   = { CoclusterUtil.C_1, CoclusterUtil.C_2,
                                                   CoclusterUtil.C_3, CoclusterUtil.C_4,
                                                   CoclusterUtil.C_5, CoclusterUtil.C_6 };
    /** the number of  row_column classes*/
    public final static String[] DIMEN_SETTING = { "2_2" };
    /** the maximum number of iterations*/
    public final static int      maxIteration  = 10;

    /** logger */
    private final static Logger  logger        = Logger
        .getLogger(LoggerDefineConstant.SERVICE_TEST);

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        Configures conf = ConfigureUtil.read("src/main/resources/rcmd.properties");
        String[] rootDirs = conf.getProperty("ROOT_DIRs").split("\\,");
        int rowCount = ((Double) conf.get("USER_COUNT_VALUE")).intValue();
        int colCount = ((Double) conf.get("ITEM_COUNT_VALUE")).intValue();

        // coclustering
        for (String rootDir : rootDirs) {
            // load dataset
            String sourceFile = rootDir + "trainingset";
            String targetCoclusterRoot = rootDir + "Cocluster/";
            SparseMatrix rateMatrix = MatrixFileUtil.read(sourceFile, rowCount, colCount);
            LoggerUtil.info(logger, (new StringBuilder("0. load dataset: ")).append(sourceFile));

            int idCount = 0;
            Queue<CoclusterTask> tasks = new LinkedList<CoclusterTask>();
            for (int diverIndx = 0; diverIndx < DIVERGENCE.length; diverIndx++) {
                for (int consts : CONSTRAINTS) {
                    for (String dimsn : DIMEN_SETTING) {
                        String[] dimenVal = dimsn.split("\\_");
                        int k = Integer.valueOf(dimenVal[0]);
                        int l = Integer.valueOf(dimenVal[1]);

                        String clusterDir = (new StringBuilder(targetCoclusterRoot))
                            .append(DIR[diverIndx]).append(consts).append('_').append(k).append('_')
                            .append(l).append(File.pathSeparator).toString();

                        tasks.add(new CoclusterTask(idCount, clusterDir, DIVERGENCE[diverIndx],
                            consts, k, l));
                        idCount++;
                    }
                }
            }
            CoclusterWorker.tasks = tasks;

            try {
                ExecutorService exec = Executors.newCachedThreadPool();
                exec.execute(new CoclusterWorker(rateMatrix));
                exec.execute(new CoclusterWorker(rateMatrix));
                exec.execute(new CoclusterWorker(rateMatrix));
                exec.execute(new CoclusterWorker(rateMatrix));
                exec.shutdown();
                exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                ExceptionUtil.caught(e, "ExecutorService await crush! ");
            }
        }

    }

    protected static class CoclusterWorker extends Thread {
        SparseMatrix                       rateMatrix;

        public static Queue<CoclusterTask> tasks;

        public static synchronized CoclusterTask task() {
            return tasks.poll();
        }

        /**
         * @param rateMatrix
         * @param settingFile
         * @param rowMappingFile
         * @param colMappingFile
         * @param diverType
         * @param constrains
         * @param k
         * @param l
         */
        public CoclusterWorker(SparseMatrix rateMatrix) {
            super();
            this.rateMatrix = rateMatrix;
        }

        /** 
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            CoclusterTask task = null;
            while ((task = task()) != null) {
                int id = task.id;
                int diverType = task.diverType;
                int constrains = task.constrains;
                int K = task.K;
                int L = task.L;

                LoggerUtil.info(logger,
                    (new StringBuilder("[" + id + "]1. start to cocluster. ")).append(diverType)
                        .append('\t').append(constrains).append('_').append(K).append('_')
                        .append(L));
                LoggerUtil.info(logger, "[" + id + "]\ta. start to cocluster.");
                Cluster[][] result = CoclusterUtil.divideWithConjugateAssumption(rateMatrix, K, L,
                    maxIteration, constrains, diverType);
                ClusterInfoUtil.saveClustering(result, task.clusterDir);
            }
        }

    }

    protected static class CoclusterTask {
        int    id;
        String clusterDir;
        int    diverType;
        int    constrains;
        int    K;
        int    L;

        /**
         * @param settingFile
         * @param rowMappingFile
         * @param colMappingFile
         * @param diverType
         * @param constrains
         * @param k
         * @param l
         */
        public CoclusterTask(int id, String clusterDir, int diverType, int constrains, int k,
                             int l) {
            super();
            this.id = id;
            this.clusterDir = clusterDir;
            this.diverType = diverType;
            this.constrains = constrains;
            K = k;
            L = l;
        }

    }
}
