package code.sma.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import code.sma.datastructure.MatlabFasionSparseMatrix;
import code.sma.parser.Ml10mParser;
import code.sma.parser.Ml1mParser;
import code.sma.parser.NetflixParser;
import code.sma.parser.Parser;
import code.sma.recommender.ma.GroupSparsityMF;
import code.sma.recommender.ma.MatrixFactorizationRecommender;
import code.sma.recommender.ma.RegularizedSVD;
import code.sma.recommender.ma.StableMA;
import code.sma.util.ConfigureUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;
import code.sma.util.StringUtil;

/**
 * This class implemented MA-based methods.
 * The parameters for algorithm and data is in src/java/resources/rcmd.properties
 * 
 * @author Chao Chen
 * @version $Id: MABasedParallel.java, v 0.1 Feb 4, 2016 4:26:03 PM chench Exp $
 */
public class MABasedParallel {

    /** the logger instance*/
    protected final static Logger logger = Logger.getLogger(LoggerDefineConstant.SERVICE_NORMAL);

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        //load dataset configure file
        Properties properties = ConfigureUtil.read("src/main/resources/rcmd.properties");
        String[] rootDirs = properties.getProperty("ROOT_DIR_ARR").split("\\,");

        // parse dataset parser
        String parserParser = properties.getProperty("DATASET_PARSER");
        Parser parser = null;
        switch (parserParser) {
            case "NETFLIX":
                parser = new NetflixParser();
                break;
            case "ML1M":
                parser = new Ml1mParser();
                break;
            case "ML10M":
                parser = new Ml10mParser();
                break;
            default:
                parser = new Ml10mParser();
                break;
        }

        // parse subsetSize arry
        String[] subsetSizeArr = properties.getProperty("SUBSET_SIZE_ARR").split("\\,");
        int ssArrLen = subsetSizeArr.length;
        int[] subsetSize = new int[ssArrLen];
        for (int sIndx = 0; sIndx < ssArrLen; sIndx++) {
            subsetSize[sIndx] = Integer.valueOf(subsetSizeArr[sIndx].trim());
        }

        // parse featureCount array
        String[] featureCountArr = properties.getProperty("FEATURE_COUNT_ARR").split("\\,");
        int fcArrLen = featureCountArr.length;
        int[] featureCount = new int[fcArrLen];
        for (int fcIndx = 0; fcIndx < fcArrLen; fcIndx++) {
            featureCount[fcIndx] = Integer.valueOf(featureCountArr[fcIndx].trim());
        }

        // draw console information
        String consoleStr = "<1>. RMSE VS NUM_OF_PARTIONTION\n\t<11>. StableMA\n\t<12>. GroupSparsityMA\n"
                            + "<2>. RMSE VS FEATURE_COUNT\n\t<21>. StableMA\n\t<22>. GroupSparsityMA\n\t<23>. RSVD\n"
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
                    for (String rootDir : rootDirs) {
                        rmseVSNumOfPartition(featureCount[0], rootDir, subsetSize, 1, parser);
                    }
                    break;
                }
                case 12: {
                    //RMSE VS NUM_OF_PARTIONTION
                    for (String rootDir : rootDirs) {
                        rmseVSNumOfPartition(featureCount[0], rootDir, subsetSize, 3, parser);
                    }
                    break;
                }
                case 21: {
                    //RMSE VS FEATURE_COUNT
                    for (String rootDir : rootDirs) {
                        rmseVSRank(featureCount, rootDir, 3, 1, parser);
                    }
                    break;
                }
                case 22: {
                    //RMSE VS FEATURE_COUNT
                    for (String rootDir : rootDirs) {
                        rmseVSRank(featureCount, rootDir, 3, 3, parser);
                    }
                    break;
                }
                case 23: {
                    //RMSE VS FEATURE_COUNT
                    for (String rootDir : rootDirs) {
                        rmseVSRank(featureCount, rootDir, 3, 2, parser);
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
                                               int algorithmId, Parser parser) {
        //loading dataset
        LoggerUtil.info(logger, "Dataset: " + rootDir);
        String trainFile = rootDir + "trainingset";
        String testFile = rootDir + "testingset";
        MatlabFasionSparseMatrix rateMatrix = MatrixFileUtil.reads(trainFile, 20 * 1000 * 1000,
            parser);
        MatlabFasionSparseMatrix testMatrix = MatrixFileUtil.reads(testFile, 20 * 1000 * 1000,
            parser);

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

    protected static void rmseVSRank(int[] featureCount, String rootDir, int k, int algorithmId,
                                     Parser parser) {
        //loading dataset
        LoggerUtil.info(logger, "Dataset: " + rootDir);
        String trainFile = rootDir + "trainingset";
        String testFile = rootDir + "testingset";
        MatlabFasionSparseMatrix rateMatrix = MatrixFileUtil.reads(trainFile, 20 * 1000 * 1000,
            parser);
        MatlabFasionSparseMatrix testMatrix = MatrixFileUtil.reads(testFile, 20 * 1000 * 1000,
            parser);

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

    /**
     * The working thread
     * 
     * @author Chao Chen
     * @version $Id: StableSVDMT.java, v 0.1 Feb 4, 2016 4:18:16 PM chench Exp $
     */
    protected static class BiSVDWorker extends Thread {
        /** the training set*/
        MatlabFasionSparseMatrix rateMatrix;
        /**  the test set*/
        MatlabFasionSparseMatrix testMatrix;
        /** the feature size*/
        int                      featureCount;
        /** the clustering size*/
        int                      k;
        /** the algorithm index*/
        int                      algorithmId;

        /**
         * Construction
         * 
         * @param rateMatrix        the training set
         * @param testMatrix        the test set
         * @param featureCount      the feature size
         * @param k                 the clustering size        
         * @param algorithmId       the algorithm index           
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
            //load algorithm configure file
            Properties properties = ConfigureUtil.read("src/main/resources/rcmd.properties");
            int userCount = Integer.valueOf(properties.getProperty("USER_COUNT"));
            int itemCount = Integer.valueOf(properties.getProperty("ITEM_COUNT"));
            double maxValue = Double.valueOf(properties.getProperty("MAX_RATING_VALUE"));
            double minValue = Double.valueOf(properties.getProperty("MIN_RATING_VALUE"));
            double lrate = Double.valueOf(properties.getProperty("LEARNING_RATE"));
            double regularized = Double.valueOf(properties.getProperty("REGULAIZED_PARAM"));
            int maxIteration = Integer.valueOf(properties.getProperty("MAX_ITERATION"));
            boolean showProgress = Boolean
                .valueOf(properties.getProperty("SHOW_DETAIL_TRAIN_AND_TEST_ERROR"));

            //build model
            MatrixFactorizationRecommender recmmd = null;
            if (algorithmId == 1) {
                recmmd = new StableMA(userCount, itemCount, maxValue, minValue, featureCount, lrate,
                    regularized, 0, maxIteration, k, showProgress);
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
