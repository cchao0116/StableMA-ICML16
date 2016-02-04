package code.sma.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.parser.Ml10mParser;
import code.sma.recommender.ma.GroupSparsityMF;
import code.sma.recommender.ma.MatrixFactorizationRecommender;
import code.sma.recommender.ma.RegularizedSVD;
import code.sma.recommender.ma.StableMA;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;
import code.sma.util.StringUtil;

public class StableSVDMT {

    /** file to store the original data and cocluster directory. 10M100K 1m*/
    public static String[]      rootDirs     = { "C:/Users/chench/Desktop/Dataset/ml-10M100K/1/",
                                                 "C:/Users/chench/Desktop/Dataset/ml-10M100K/2/",
                                                 "C:/Users/chench/Desktop/Dataset/ml-10M100K/3/",
                                                 "C:/Users/chench/Desktop/Dataset/ml-10M100K/4/",
                                                 "C:/Users/chench/Desktop/Dataset/ml-10M100K/5/" };
    /** The number of users. 943 6040 69878  480189*/
    public final static int     userCount    = 69878;
    /** The number of items. 1682 3706 10677 17770*/
    public final static int     itemCount    = 10677;
    public final static double  maxValue     = 5.0;
    public final static double  minValue     = 0.5;
    public final static double  lrate        = 0.001;
    public final static double  regularized  = 0.06;
    public final static int     maxIteration = 100;
    public final static boolean showProgress = true;

    protected final static Logger logger = Logger.getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        // draw console information
        String consoleStr = "<1>. RMSE VS NUM_OF_PARTIONTION\n\t<11>. RSVD\n\t<12>. StableMA\t<13>. GroupSparsityMA\n"
                            + "<2>. RMSE VS FEATURE_COUNT\n\t<21>. RSVD\n\t<22>. StableMA\t<23>. GroupSparsityMA\n"
                            + "CHOSE: ";
        System.out.print(consoleStr);

        // catch input from console
        String cmd = null;
        try {
            BufferedReader bfReader = new BufferedReader(new InputStreamReader(System.in));
            cmd = bfReader.readLine();
            if (!StringUtil.isNumeric(cmd)) {
                return;
            }

            int option = Integer.valueOf(cmd);
            switch (option) {
                case 11: {
                    //RMSE VS NUM_OF_PARTIONTION
                    int[] ks = { 2, 3, 4, 5 };
                    for (String rootDir : rootDirs) {
                        rmseVSNumOfPartition(200, rootDir, ks, 1);
                    }
                    break;
                }
                case 12: {
                    //RMSE VS NUM_OF_PARTIONTION
                    int[] ks = { 2, 3, 4, 5 };
                    for (String rootDir : rootDirs) {
                        rmseVSNumOfPartition(200, rootDir, ks, 2);
                    }
                    break;
                }
                case 13: {
                    //RMSE VS NUM_OF_PARTIONTION
                    int[] ks = { 2, 3, 4, 5 };
                    for (String rootDir : rootDirs) {
                        rmseVSNumOfPartition(200, rootDir, ks, 3);
                    }
                    break;
                }
                case 21: {
                    //RMSE VS FEATURE_COUNT
                    int[] featureCount = { 150, 200, 250 };
                    for (String rootDir : rootDirs) {
                        rmseVSRank(featureCount, rootDir, 2, 1);
                    }
                    break;
                }
                case 22: {
                    //RMSE VS FEATURE_COUNT
                    int[] featureCount = { 150, 200, 250 };
                    for (String rootDir : rootDirs) {
                        rmseVSRank(featureCount, rootDir, 2, 2);
                    }
                    break;
                }
                case 23: {
                    //RMSE VS FEATURE_COUNT
                    int[] featureCount = { 150, 200, 250 };
                    for (String rootDir : rootDirs) {
                        rmseVSRank(featureCount, rootDir, 2, 3);
                    }
                    break;
                }
                default:
                    break;
            }

        } catch (IOException e) {
            ExceptionUtil.caught(e, "Input: " + cmd);
        }
    }

    protected static void rmseVSNumOfPartition(int featureCount, String rootDir, int[] ks,
                                               int algorithmId) {
        //loading dataset
        LoggerUtil.info(logger, "Dataset: " + rootDir);
        String trainFile = rootDir + "trainingset";
        String testFile = rootDir + "testingset";
        MatlabFasionSparseMatrix rateMatrix = MatrixFileUtil.reads(trainFile, 20 * 1000 * 1000,
            new Ml10mParser());
        MatlabFasionSparseMatrix testMatrix = MatrixFileUtil.reads(testFile, 20 * 1000 * 1000,
            new Ml10mParser());

        try {
            ExecutorService exec = Executors.newCachedThreadPool();
            for (int k : ks) {
                exec.execute(new BiSVDWorker(rateMatrix, testMatrix, featureCount, k, algorithmId));
            }
            exec.shutdown();
            exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            ExceptionUtil.caught(e, "ExecutorService await crush! ");
        }
    }

    protected static void rmseVSRank(int[] featureCount, String rootDir, int k, int algorithmId) {
        //loading dataset
        LoggerUtil.info(logger, "Dataset: " + rootDir);
        String trainFile = rootDir + "trainingset";
        String testFile = rootDir + "testingset";
        MatlabFasionSparseMatrix rateMatrix = MatrixFileUtil.reads(trainFile, 20 * 1000 * 1000,
            null);
        MatlabFasionSparseMatrix testMatrix = MatrixFileUtil.reads(testFile, 20 * 1000 * 1000,
            null);

        try {
            ExecutorService exec = Executors.newCachedThreadPool();
            for (int fc : featureCount) {
                exec.execute(new BiSVDWorker(rateMatrix, testMatrix, fc, k, algorithmId));
            }
            exec.shutdown();
            exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            ExceptionUtil.caught(e, "ExecutorService await crush! ");
        }

    }

    protected static class BiSVDWorker extends Thread {
        MatlabFasionSparseMatrix rateMatrix;
        MatlabFasionSparseMatrix testMatrix;
        int                      featureCount;
        int                      k;
        int                      algorithmId;

        /**
         * @param rateMatrix
         * @param testMatrix
         * @param featureCount
         * @param clusterDir
         * @param rootDir
         */
        public BiSVDWorker(MatlabFasionSparseMatrix rateMatrix, MatlabFasionSparseMatrix testMatrix,
                           int featureCount, int k, int algorithmId) {
            super();
            this.rateMatrix = rateMatrix;
            this.testMatrix = testMatrix;
            this.featureCount = featureCount;
            this.k = k;
            this.algorithmId = algorithmId;
        }

        /** 
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            //build model
            MatrixFactorizationRecommender recmmd = null;

            if (algorithmId == 1) {
                recmmd = new StableMA(userCount, itemCount, maxValue, minValue, featureCount, lrate,
                    regularized, 0, maxIteration, k, showProgress, 7);
            } else if (algorithmId == 2) {
                recmmd = new RegularizedSVD(userCount, itemCount, maxValue, minValue, featureCount,
                    lrate, regularized, 0, maxIteration, showProgress);
            } else if (algorithmId == 3) {
                recmmd = new GroupSparsityMF(userCount, itemCount, maxValue, minValue, featureCount,
                    1, 70, 0.05, maxIteration, 3, showProgress);
            }
            recmmd.buildModel(rateMatrix, testMatrix);
        }

    }

}
