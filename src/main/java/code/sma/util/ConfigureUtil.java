package code.sma.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.io.Files;

import code.sma.core.impl.DenseVector;
import code.sma.main.Configures;

/**
 * 
 * @author Chao Chen
 * @version $Id: ConfigureUtil.java, v 0.1 Nov 17, 2015 12:48:12 PM chench Exp $
 */
public final class ConfigureUtil {

    /**
     * forbidden construction
     */
    private ConfigureUtil() {

    }

    /**
     * read the configure file, and parse it.
     * Note that the key of "DUMP" is for the anonymous parameter
     * 
     * @param fileName
     * @return
     * @throws IOException 
     */
    public static Configures read(String fileName) throws IOException {
        Configures conf = new Configures();

        // parsing files
        List<String> lines = Files.readLines(new File(fileName), Charset.defaultCharset());
        StringBuilder anonymousParam = new StringBuilder();
        for (String line : lines) {
            if (StringUtil.isBlank(line) | line.startsWith("#")) {
                // filtering footnotes
                continue;
            } else if (line.startsWith("$")) {
                String key = line.substring(1, line.indexOf('='));
                String val = line.substring(line.indexOf('=') + 1);

                if (StringUtil.isBlank(val)) {
                    continue;
                } else if (key.endsWith("_ARR")) {
                    String[] elmnts = val.split("\\,");

                    int num = elmnts.length;
                    DenseVector dv = new DenseVector(num);
                    for (int n = 0; n < num; n++) {
                        dv.setValue(n, Float.valueOf(elmnts[n].trim()));
                    }
                    conf.setVector(key, dv);
                } else if (key.endsWith("_VALUE")) {
                    conf.put(key, Float.valueOf(val.trim()));
                } else if (key.endsWith("_BOOLEAN")) {
                    conf.put(key, Boolean.valueOf(val.trim()));
                } else {
                    conf.setProperty(key, val);
                }

            } else {
                anonymousParam.append(line).append('\t');
            }
        }

        // add anonymous parameter
        conf.setProperty("DUMP", anonymousParam.toString());
        return conf;
    }

}
