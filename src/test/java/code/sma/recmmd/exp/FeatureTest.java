package code.sma.recmmd.exp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.Test;

import code.sma.core.Tuples;
import code.sma.core.impl.DenseMatrix;
import code.sma.core.impl.DenseVector;
import code.sma.dpncy.AbstractDpncyChecker;
import code.sma.dpncy.ModelDpncyChecker;
import code.sma.main.Configures;
import code.sma.recmmd.RecConfigEnv;
import code.sma.recmmd.standalone.GLOMA;
import code.sma.recmmd.standalone.RegularizedSVD;
import code.sma.util.ConfigureUtil;
import code.sma.util.ExceptionUtil;
import code.sma.util.FileUtil;
import code.sma.util.LoggerDefineConstant;
import code.sma.util.LoggerUtil;
import code.sma.util.MatrixFileUtil;
import code.sma.util.PythonExecUtil;
import code.sma.util.SerializeUtil;
import code.sma.util.StringUtil;

public class FeatureTest {
    /** the logger instance */
    protected final static Logger logger               = Logger
        .getLogger(LoggerDefineConstant.SERVICE_TEST);
    protected final static int    ITER_SEQ             = 1;
    protected final static String OLD_MULTSK_R_PATTERN = "C:/Dataset/ml-10m/4/SerialObj/NIPS_Iter%d/GLOMA[%d].OBJ";
    protected final static String OLD_EMBD_R_PATTERN   = "C:/Dataset/ml-1m/1/SerialObj/[RegSVD]_[200]_[%d]_[1]_[20].OBJ";
    protected final static String NEW_EMBD_W_PATTERN   = "C:/Dataset/ml-1m/1/SerialObj/[RegSVD]_[200]_[%d]_[1]_[20].OBJ";

    protected final static String TEMP_FI              = "C:/TEMP";
    protected final static String PYTHON_PATH          = "C:/Users/Administrator/AppData/Local/Programs/Python/Python36/python";

    @Test
    public void test() {

        Configures conf = ConfigureUtil.read("src/main/resources/samples/MTREC.properties");
        String[] rootDirs = conf.getProperty("ROOT_DIRs").split("\\,");

        for (String rootDir : rootDirs) {
            LoggerUtil.info(logger, "1. loading " + rootDir);
            conf.setProperty("ROOT_DIR", rootDir);
            String testFile = rootDir + "testingset";

            String algName = conf.getProperty("ALG_NAME");
            LoggerUtil.info(logger, "2. running " + algName);

            if (StringUtil.equalsIgnoreCase(algName, "MTREC")) {
                AbstractDpncyChecker checker = new ModelDpncyChecker();
                checker.handler(conf);

                Tuples ttMatrix = MatrixFileUtil.reads(testFile);
                RecConfigEnv rce = new RecConfigEnv(conf);
                RegularizedSVD rcmmd = extctGlbl(rce);
                SerializeUtil.writeObject(rcmmd, String.format(NEW_EMBD_W_PATTERN, ITER_SEQ));
                LoggerUtil.info(logger, String.format("%s\n%s", rcmmd.toString(),
                    rcmmd.evaluate(ttMatrix).printOneLine()));

            }
        }

    }

    protected RegularizedSVD extctGlbl(RecConfigEnv rce) {
        RegularizedSVD auxRec = (RegularizedSVD) SerializeUtil
            .readObject(String.format(OLD_EMBD_R_PATTERN, ITER_SEQ - 1));
        List<GLOMA> modls = new ArrayList<GLOMA>();
        for (int i = 0; i < 30; i++) {
            GLOMA recmmd = (GLOMA) SerializeUtil
                .readObject(String.format(OLD_MULTSK_R_PATTERN, ITER_SEQ, i));
            modls.add(recmmd);

        }

        int threadNum = ((Double) rce.get("THREAD_NUMBER_VALUE")).intValue();
        int userCount = auxRec.userCount;
        int itemCount = auxRec.itemCount;
        int featCount = auxRec.featureCount;
        LoggerUtil.info(logger, "UFeats ");
        DenseMatrix userFeature = new DenseMatrix(userCount, featCount);
        try {
            MultThread.modls = modls;
            MultThread.auxRec = auxRec;
            MultThread.maxID = userCount;
            MultThread.isU = true;

            ExecutorService exec = Executors.newCachedThreadPool();
            for (int t = 0; t < threadNum; t++) {
                exec.execute(new MultThread(userFeature, TEMP_FI + t));
            }
            exec.shutdown();
            exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            ExceptionUtil.caught(e, "User Features");
        }

        LoggerUtil.info(logger, "VFeats ");
        DenseMatrix itemFeature = new DenseMatrix(itemCount, featCount);
        try {
            MultThread.maxID = itemCount;
            MultThread.isU = false;

            ExecutorService exec = Executors.newCachedThreadPool();
            for (int t = 0; t < threadNum; t++) {
                exec.execute(new MultThread(itemFeature, TEMP_FI + t));
            }
            exec.shutdown();
            exec.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            ExceptionUtil.caught(e, "Item Features");
        }

        return new RegularizedSVD(rce, userFeature, itemFeature);
    }

    public static class MultThread extends Thread {
        public static List<GLOMA>    modls;
        public static RegularizedSVD auxRec;
        public static int            maxID;
        public static boolean        isU;
        private static final Object  MUTEX = new Object();

        private DenseMatrix          feats;
        private String               lTEMP;
        private static int           step;

        public MultThread(DenseMatrix feats, String lTEMP) {
            this.feats = feats;
            this.lTEMP = lTEMP;
            step = maxID / 50;
        }

        private static int map() {
            synchronized (MUTEX) {
                if (maxID < 1) {
                    return -1;
                } else {
                    maxID--;
                    if (maxID % step == 0) {
                        LoggerUtil.info(logger, "..." + ((50 * step - maxID) / step) * 2 + "p");
                    }
                    return maxID;
                }
            }
        }

        /**
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            int ID = 0;
            while ((ID = map()) != -1) {
                if (isU) {
                    int u = ID;
                    String uFeats = getUFeats(modls, auxRec, u);
                    if (StringUtil.isBlank(uFeats)) {
                        continue;
                    }

                    FileUtil.write(lTEMP, uFeats);

                    DenseVector glbFeat = getGlblFeat(lTEMP);
                    feats.setRowRef(u, glbFeat);
                } else {
                    int i = ID;
                    String vFeats = getVFeats(modls, auxRec, i);
                    if (StringUtil.isBlank(vFeats)) {
                        continue;
                    }

                    FileUtil.write(lTEMP, vFeats);

                    DenseVector glbFeat = getGlblFeat(lTEMP);
                    feats.setRowRef(i, glbFeat);
                }
            }

            if (FileUtil.exists(lTEMP)) {
                FileUtil.delete(lTEMP);
            }
        }

        protected String getUFeats(List<GLOMA> modls, RegularizedSVD auxRec, int u) {
            DenseVector avec = auxRec.userDenseFeatures.getRowRef(u);

            StringBuilder uFeats = new StringBuilder();
            for (GLOMA recmmd : modls) {
                DenseVector vec = recmmd.userDenseFeatures.getRowRef(u);
                if (vec != null) {
                    DenseVector feat = new DenseVector(recmmd.featureCount);
                    for (int k = 0; k < recmmd.featureCount; k++) {
                        feat.setValue(k, (vec.getValue(k) + avec.getValue(k)) / 2.0);
                    }

                    uFeats.append(feat.toString() + '\n');
                }
            }
            return uFeats.toString();
        }

        protected String getVFeats(List<GLOMA> modls, RegularizedSVD auxRec, int i) {
            DenseVector avec = auxRec.itemDenseFeatures.getRowRef(i);

            StringBuilder uFeats = new StringBuilder();
            for (GLOMA recmmd : modls) {
                DenseVector vec = recmmd.itemDenseFeatures.getRowRef(i);
                if (vec != null) {
                    DenseVector feat = new DenseVector(recmmd.featureCount);
                    for (int k = 0; k < recmmd.featureCount; k++) {
                        feat.setValue(k, (vec.getValue(k) + avec.getValue(k)) / 2.0);
                    }

                    uFeats.append(feat.toString() + '\n');
                }
            }
            return uFeats.toString();
        }

        protected DenseVector getGlblFeat(String fi) {
            String featrs = PythonExecUtil.exec(PYTHON_PATH,
                "src/main/resources/scripts/featr_extr.py", fi);
            String[] fs = featrs.substring(featrs.indexOf("[") + 1, featrs.indexOf("]")).toString()
                .trim().split("\\s+");
            DenseVector fVec = new DenseVector(fs.length);
            for (int i = 0; i < fs.length; i++) {
                fVec.setValue(i, Double.valueOf(fs[i].trim()));
            }
            return fVec;
        }

    }

}
