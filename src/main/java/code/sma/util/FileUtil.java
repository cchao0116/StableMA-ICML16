/**
 * Tongji Edu.
 * Copyright (c) 2004-2013 All Rights Reserved.
 */
package code.sma.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * 简单文件处理工具，读取文件中所有行。
 * 
 * @author Hanke Chen
 * @version $Id: FileUtil.java, v 0.1 2013-11-26 上午11:16:49 chench Exp $
 */
public final class FileUtil {

    /** 配置文件中所使用的路径分隔符*/
    public static final char   UNION_DIR_SEPERATOR = '/';

    /** 文件换行符*/
    public static final char   BREAK_LINE          = '\n';

    /** 文件后缀 */
    public static final String TXT_FILE_SUFFIX     = ".txt";

    /** 文件格式的填充字符 */
    public final static char   ZERO_PAD_CHAR       = '0';

    /**
     * 禁用构造函数
     */
    private FileUtil() {

    }

    /**
     * 简单读取文件，
     * 返回文件所有行，且去掉空格.
     * 
     * @param path   文件路径
     * @return
     */
    public static String readLinesAsStream(String path) {
        File file = new File(path);

        //读取并解析数据
        if (!file.isFile() | !file.exists()) {
            ExceptionUtil.caught(new FileNotFoundException("File Not Found"), "读取文件发生异常，校验文件路径: "
                                                                              + path);
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));

            StringBuilder stringBuilder = new StringBuilder();
            String context = null;
            while ((context = reader.readLine()) != null) {
                stringBuilder.append(StringUtil.trim(context)).append(StringUtil.BREAK_LINE);
            }

            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            ExceptionUtil.caught(e, "无法找到对应的加载文件: " + path);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "读取文件发生异常，校验文件格式");
        } finally {
            IOUtils.closeQuietly(reader);
        }

        //出现异常，返回null
        return null;
    }

    /**
     * 简单读取文件，
     * 返回文件所有行，且去掉空格.
     * 
     * @param path   文件路径
     * @return
     */
    public static String[] readLines(String path) {
        File file = new File(path);

        //读取并解析数据
        if (!file.isFile() | !file.exists()) {
            ExceptionUtil.caught(new FileNotFoundException("File Not Found"), "读取文件发生异常，校验文件路径: "
                                                                              + path);
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));

            List<String> lines = new ArrayList<String>();
            String context = null;
            while ((context = reader.readLine()) != null) {
                lines.add(StringUtil.trim(context));
            }

            return lines.toArray(new String[lines.size()]);
        } catch (FileNotFoundException e) {
            ExceptionUtil.caught(e, "无法找到对应的加载文件: " + path);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "读取文件发生异常，校验文件格式");
        } finally {
            IOUtils.closeQuietly(reader);
        }

        //出现异常，返回null
        return null;
    }

    /**
     * 根据通配符规则，读取多个文件
     * 
     * @param path
     * @return
     */
    public static String[] readLinesByPattern(String path) {
        if (StringUtil.isEmpty(path)) {
            return null;
        }

        //拆分目录和正则表达式
        int index = path.lastIndexOf(UNION_DIR_SEPERATOR);
        String dirValue = path.substring(0, index);
        String regexValue = path.substring(index + 1);
        File dir = new File(dirValue);
        if (!dir.isDirectory() | StringUtil.isBlank(regexValue)) {
            ExceptionUtil.caught(new FileNotFoundException("File Not Found"), "目录不存在，校验文件路径: "
                                                                              + path);
            return null;
        }

        //批量读取文件
        List<String> context = new ArrayList<String>();
        File[] files = dir.listFiles();
        Pattern p = Pattern.compile(regexValue);
        for (File file : files) {
            if (file.isFile() && p.matcher(file.getName()).matches()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(file));

                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        context.add(StringUtil.trim(line));
                    }

                } catch (FileNotFoundException e) {
                    ExceptionUtil.caught(e, "无法找到对应的加载文件: " + path);
                } catch (IOException e) {
                    ExceptionUtil.caught(e, "读取文件发生异常，校验文件格式");
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
        }

        return context.toArray(new String[context.size()]);
    }

    /**
     * 获取符合模式的文件路径
     * 
     * @param path  文件路径的RE
     * @return
     */
    public static File[] parserFilesByPattern(String path) {
        if (StringUtil.isEmpty(path)) {
            return null;
        }

        //拆分目录和正则表达式
        int index = path.lastIndexOf(UNION_DIR_SEPERATOR);
        String dirValue = path.substring(0, index);
        String regexValue = path.substring(index + 1);
        File dir = new File(dirValue);
        if (!dir.isDirectory() | StringUtil.isBlank(regexValue)) {
            ExceptionUtil.caught(new FileNotFoundException("File Not Found"), "目录不存在，校验文件路径: "
                                                                              + path);
            return null;
        }

        //批量读取文件
        List<File> filePaths = new ArrayList<File>();
        Pattern p = Pattern.compile(regexValue);
        for (File file : dir.listFiles()) {
            if (file.isFile() && p.matcher(file.getName()).matches()) {
                filePaths.add(file);
            }
        }
        return filePaths.toArray(new File[filePaths.size()]);
    }

    /**
     * 简单写文件
     * 
     * @param file
     * @param context
     */
    public static void write(String file, String context) {

        BufferedWriter writer = null;
        try {
            //make sure parent directory exist
            File f = new File(file);
            File dir = f.getParentFile();
            if (f.isFile() && f.exists()) {
                f.delete();
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }

            //write to file
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(context);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "写文件发生异常，校验文件格式");
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Append the content into the given file
     * 
     * @param file      the file to write
     * @param context   the content to write
     */
    public static void writeAsAppendWithDirCheck(String file, String context) {
        FileWriter writer = null;
        try {
            //make sure parent directory exist
            File f = new File(file);
            File dir = f.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            //write information to disk
            writer = new FileWriter(file, true);
            writer.append(context);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "写文件发生异常，校验文件格式");
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Append the content into the given file
     * 
     * @param file      the file to write
     * @param context   the content to write
     */
    public static void writeAsAppend(String file, String context) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            writer.append(context);
        } catch (IOException e) {
            ExceptionUtil.caught(e, "写文件发生异常，校验文件格式");
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Tests whether the file or directory denoted by this abstract pathname exists
     * 
     * @param file the file to check
     * @return
     */
    public static boolean exists(String file) {
        File f = new File(file);
        return f.exists();
    }

    /**
     * Delete the given file 
     * 
     * @param file
     * @return
     */
    public static boolean delete(String file) {
        File f = new File(file);
        if (f.exists()) {
            return f.delete();
        }

        return false;
    }

    /**
     * Check whether the director of this file exists, if not, then make this directory.
     * 
     * @param file the file to check
     * @return
     */
    public static boolean existDirAndMakeDir(String file) {
        File dir = (new File(file)).getParentFile();
        if (!dir.exists()) {
            return dir.mkdirs();
        }

        return false;
    }
}
