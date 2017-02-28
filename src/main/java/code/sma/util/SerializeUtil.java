package code.sma.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * The SerializeUtil is used to simplify the way to Read/Write the Object from/to file
 * 
 * @author Hanke
 * @version $Id: SerializeUtil.java, v 0.1 2015-4-27 上午10:44:52 Exp $
 */
public final class SerializeUtil {

    /**
     * 禁用构造函数
     */
    private SerializeUtil() {

    }

    /**
     * Write Object into files
     * 
     * @param obj
     * @param outputFile
     */
    public static void writeObject(Object obj, String outputFile) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(outputFile));
            out.writeObject(obj);
            out.close();
        } catch (FileNotFoundException e) {
            ExceptionUtil.caught(e, outputFile + " Not found.");
        } catch (IOException e) {
            ExceptionUtil.caught(e, outputFile + " IO crushed.");
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Read Object from files
     * 
     * @param intputFile
     * @return
     */
    public static Object readObject(String intputFile) {
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(new FileInputStream(intputFile));
            return oin.readObject();
        } catch (IOException e) {
            ExceptionUtil.caught(e, intputFile + " IO crushed.");
        } catch (ClassNotFoundException e) {
            ExceptionUtil.caught(e, intputFile + " ClassNotFound.");
        } finally {
            IOUtils.closeQuietly(oin);
        }

        return null;
    }
}
