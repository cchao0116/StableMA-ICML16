package code.sma.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 
 * @author Chao.Chen
 * @version $Id: PythonExecUtil.java, v 0.1 2017年4月14日 上午11:50:12 Chao.Chen Exp $
 */
public final class PythonExecUtil {
    private PythonExecUtil() {
    }

    public static String exec(String... args) {

        try {

            ProcessBuilder pb = new ProcessBuilder(args);
            Process p = pb.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

            StringBuilder featrs = new StringBuilder();
            String line = null;
            while ((line = in.readLine()) != null) {
                featrs.append(line);
            }

            return featrs.toString();
        } catch (Exception e) {
            ExceptionUtil.caught(e, "Python Scripts Error.");
        }
        return null;
    }

}
