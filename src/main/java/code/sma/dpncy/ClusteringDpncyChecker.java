package code.sma.dpncy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import code.sma.clustering.Cluster;
import code.sma.clustering.CoclusterUtil;
import code.sma.clustering.Distance;
import code.sma.core.AbstractMatrix;
import code.sma.core.impl.SparseMatrix;
import code.sma.main.Configures;
import code.sma.thread.TaskMsgDispatcher;
import code.sma.util.ClusterInfoUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixIOUtil;
import code.sma.util.StringUtil;

/**
 * 
 * @author Chao.Chen
 * @version $Id: ClusteringDpncyChecker.java, v 0.1 2016年9月28日 上午11:14:32 Chao.Chen Exp $
 */
public class ClusteringDpncyChecker extends AbstractDpncyChecker implements TaskMsgDispatcher {
    /** the clusting tasks */
    protected Queue<String> clusterDirs;
    /** mutex using in map procedure*/
    protected static Object MAP_MUTEX = new Object();

    public ClusteringDpncyChecker() {
        clusterDirs = new LinkedList<String>();
    }

    /** 
     * @see code.sma.dpncy.AbstractDpncyChecker#handler(code.sma.main.Configures)
     */
    @Override
    public void handler(Configures conf) {
        String rootDir = conf.getProperty("ROOT_DIR");
        String[] cDirStrs = ((String) conf.get("CLUSTERING_SET")).split("\\,");

        // find the clusterings which are not obtained 
        for (String cDirStr : cDirStrs) {
            String clusterDir = rootDir + cDirStr;
            if (Files.notExists((new File(clusterDir)).toPath())) {
                clusterDirs.add(cDirStr);
            }
        }

        if (!clusterDirs.isEmpty()) {
            // compute the undone clustering
            int threadNum = ((Float) conf.get("THREAD_NUMBER_VALUE")).intValue();
            int rowCount = ((Float) conf.get("USER_COUNT_VALUE")).intValue();
            int colCount = ((Float) conf.get("ITEM_COUNT_VALUE")).intValue();
            String trainFile = rootDir + "trainingset";
            SparseMatrix rateMatrix = MatrixIOUtil.loadSparseMatrix(trainFile, rowCount, colCount);

            try {
                ExecutorService exec = Executors.newCachedThreadPool();
                for (int t = 0; t < threadNum; t++) {
                    exec.execute(new ClusteringLearner(this, rateMatrix, conf));
                }
                exec.shutdown();
                exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                ExceptionUtil.caught(e, "Clustering Thead!");
            }
        }

        if (this.successor != null) {
            this.successor.handler(conf);
        } else {
            LoggerUtil.info(normalLogger, "...check...passed");
        }
    }

    /** 
     * @see code.sma.thread.TaskMsgDispatcher#map()
     */
    @Override
    public Object map() {
        synchronized (MAP_MUTEX) {
            return clusterDirs.poll();
        }
    }

    /** 
     * 
     * @see code.sma.thread.TaskMsgDispatcher#reduce(java.lang.Object, code.sma.core.AbstractMatrix, code.sma.core.AbstractMatrix)
     */
    @Override
    public void reduce(Object recmmd, AbstractMatrix tnMatrix, AbstractMatrix ttMatrix) {
    }

    protected class ClusteringLearner extends Thread {
        /** learning task dispatcher*/
        private TaskMsgDispatcher dispatcher;
        /** training data*/
        private SparseMatrix      tnMatrix;
        /** configure information*/
        private Configures        conf;

        public ClusteringLearner(TaskMsgDispatcher dispatcher, SparseMatrix tnMatrix,
                                 Configures conf) {
            super();
            this.dispatcher = dispatcher;
            this.tnMatrix = tnMatrix;
            this.conf = conf;
        }

        /** 
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            String rootDir = conf.getProperty("ROOT_DIR");
            String cDirStr = null;

            try {
                while ((cDirStr = (String) dispatcher.map()) != null) {
                    String[] info = cDirStr.substring(cDirStr.lastIndexOf('/') + 1).split("\\_");
                    String dstInfo = info[0];
                    int k = Integer.valueOf(info[1].trim());
                    int l = Integer.valueOf(info[2].trim());

                    Distance dtncConst = Distance
                        .valueOf(StringUtil.toUpperCase(dstInfo.substring(0, 2)));
                    int constrains = Integer.valueOf(dstInfo.substring(2, 3));

                    LoggerUtil.info(normalLogger, "...check...missing: " + cDirStr);
                    Cluster[][] result = CoclusterUtil.divideWithConjugateAssumption(tnMatrix, k, l,
                        15, constrains, dtncConst);
                    ClusterInfoUtil.saveClustering(result, rootDir + cDirStr + File.separator);
                }
            } catch (IOException e) {
                ExceptionUtil.caught(e, "FILE: " + cDirStr);
            }
        }

    }
}
