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
     * @param  fileName         configure file
     * @return Configures       configure object
     * @throws IOException      signals that an I/O exception of some sort has occurred.
     */
    public static Configures read(String fileName) throws IOException {
        Configures conf = new Configures();

        // parsing files
        List<String> lines = Files.readLines(new File(fileName), Charset.defaultCharset());
        StringBuilder anonymousParam = new StringBuilder();
        for (String line : lines) {
            line = StringUtil.trim(line);

            if (StringUtil.isBlank(line) | line.startsWith("#")) {
                // filtering footnotes
                continue;
            } else if (line.startsWith("$")) {
                read_param(conf, line, true);
            } else {
                anonymousParam.append(line).append('\t');
            }
        }

        // add anonymous parameter
        conf.setProperty("DUMP", anonymousParam.toString());
        return conf;
    }

    /**
     * add extra configures
     * 
     * @param conf          configure object
     * @param fileName      configure file
     * @throws IOException  signals that an I/O exception of some sort has occurred.
     */
    public static void addConfig(Configures conf, String fileName) throws IOException {
        // parsing files
        List<String> lines = Files.readLines(new File(fileName), Charset.defaultCharset());
        for (String line : lines) {
            line = StringUtil.trim(line);
            if (StringUtil.isBlank(line) | line.startsWith("#")) {
                // filtering footnotes
                continue;
            } else if (line.startsWith("$")) {
                read_param(conf, line, false);
            }
        }
    }

    /**
     * parse one line in configure file into  parameters 
     * 
     * @param conf      configure objects
     * @param line      each line in configure file
     */
    protected static void read_param(Configures conf, String line, boolean needOverwrite) {
        int index_delimiter = line.indexOf('=');
        assert index_delimiter != -1 : "Every variable mush have a right-hand value. WRONG: "
                                       + line;

        String key = line.substring(1, index_delimiter);
        String val = line.substring(index_delimiter + 1);
        if (!needOverwrite && conf.containsKey(key)) {
            return;
        }

        if (StringUtil.isBlank(val)) {
            return;
        } else if (key.endsWith("_ARR")) {
            String[] elmnts = val.split("\\,");

            int num = elmnts.length;
            DenseVector dv = new DenseVector(num);
            for (int n = 0; n < num; n++) {
                dv.setValue(n, Double.valueOf(elmnts[n].trim()));
            }
            conf.setVector(key, dv);
        } else if (key.endsWith("_VALUE")) {
            conf.put(key, Double.valueOf(val.trim()));
        } else if (key.endsWith("_BOOLEAN")) {
            conf.put(key, Boolean.valueOf(val.trim()));
        } else {
            conf.setProperty(key, val);
        }
    }
}
