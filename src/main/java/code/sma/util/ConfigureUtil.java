package code.sma.util;

import java.util.Properties;

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
     */
    public static Properties read(String fileName) {
        Properties properties = new Properties();

        // parsing files
        String[] lines = FileUtil.readLines(fileName);
        StringBuilder anonymousParam = new StringBuilder();
        for (String line : lines) {
            if (StringUtil.isBlank(line) | line.startsWith("#")) {
                // filtering footnotes
                continue;
            } else if (line.startsWith("$")) {
                String key = line.substring(1, line.indexOf('='));
                String value = line.substring(line.indexOf('=') + 1);
                properties.setProperty(key, value);
            } else {
                anonymousParam.append(line).append('\t');
            }
        }

        // add anonymous parameter
        properties.setProperty("DUMP", anonymousParam.toString());
        return properties;
    }

}
