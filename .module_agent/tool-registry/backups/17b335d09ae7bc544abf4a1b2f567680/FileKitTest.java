package com.datanew.core.toolkit;

import com.datanew.core.io.LineHandler;
import com.datanew.core.io.file.FileReader;
import com.datanew.core.io.resource.Resource;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static org.junit.Assert.*;

public class FileKitTest {

    //磁盘上的空文件
    File file = new File("E:\\Download\\empty");
    //磁盘上的非空文件
    File file1 = new File("E:\\Download\\postman");
    File file2 = new File("E:\\Download\\FileTest");
    //磁盘上的空文件路径
    String path = "E:\\Download\\empty";
    //磁盘上的非空文件路径
    String path1 = "E:\\Download\\postman";
    String path2 = "E:\\Download\\FileTest";

    @Test
    public void ls() {
        // 列出目录文件  path 目录绝对路径或者相对路径 返回 文件列表（包含目录）
        assertNotNull(FileKit.ls(path));
        assertNotNull(FileKit.ls(path1));
        assertNotNull(FileKit.ls(path2));
    }

    @Test
    public void isEmpty() {
        // 文件是否为空  目录：里面没有文件时为空 文件：文件大小为0时为空
        assertTrue(FileKit.isEmpty(file));
        assertFalse(FileKit.isEmpty(file1));
        assertFalse(FileKit.isEmpty(file2));
    }

    @Test
    public void isNotEmpty() {
        // 文件是否不为空
        assertFalse(FileKit.isNotEmpty(file));
        assertTrue(FileKit.isNotEmpty(file1));
        assertTrue(FileKit.isNotEmpty(file2));
    }

    @Test
    public void isDirEmpty() {
        // 目录是否为空
        assertTrue(FileKit.isDirEmpty(file));
        assertFalse(FileKit.isDirEmpty(file1));
        assertFalse(FileKit.isDirEmpty(file2));
    }

    @Test
    public void loopFiles() {
        // 递归遍历目录以及子目录中的所有文件
        List<File> files = FileKit.loopFiles(file);
        List<File> files1 = FileKit.loopFiles(file1);
        List<File> files2 = FileKit.loopFiles(file2);

        List<File> filesBypath = FileKit.loopFiles(path);
        List<File> filesBypath1 = FileKit.loopFiles(path1);
        List<File> filesBypath2 = FileKit.loopFiles(path2);

        assertEquals(files, filesBypath);
        assertEquals(files1, filesBypath1);
        assertEquals(files2, filesBypath2);
    }

    @Test
    public void listFileNames() {
        // 获得指定目录下所有文件 不会扫描子目录
        List<String> strings = FileKit.listFileNames(path);
        List<String> strings1 = FileKit.listFileNames(path1);
        List<String> strings2 = FileKit.listFileNames(path2);

        System.out.println(strings);
        System.out.println(strings1);
        System.out.println(strings2);
    }

    @Test
    public void newFile() {
        // 创建File对象，相当于调用new File()，不做任何处理
        assertNotNull(FileKit.newFile("E:\\Download"));
    }

    @Test
    public void file() {
        // 创建File对象，自动识别相对或绝对路径，相对路径将自动从ClassPath下寻找
        assertNotNull(FileKit.file("E:\\Download"));
    }

    @Test
    public void file1() {
        // 创建File对象  parent 父目录  path  文件路径
        assertNotNull(FileKit.file("E:\\", "\\Download"));
    }

    @Test
    public void file2() {
        // 通过多层目录参数创建文件  directory 父目录  names 元素名（多层目录名）
        File file = new File("E:\\");
        assertNotNull(FileKit.file(file, "Download", "FileTest", "1.txt"));
    }

    @Test
    public void file3() {
        // 通过多层目录创建文件
        assertNotNull(FileKit.file("E:\\", "Download", "FileTest", "1.txt"));
    }

    @Test
    public void file4() throws IOException {
        // 通过文件URI创建File对象
        File file = new File("E:\\Download\\FileTest\\1.txt");
        URI uri = file.toURI();
        assertNotNull(FileKit.file(uri));
    }

    @Test
    public void file5() throws IOException {
        // 通过文件URI创建File对象
        File file = new File("E:\\Download\\FileTest\\1.txt");
        URL url = file.toURL();
        assertNotNull(FileKit.file(url));
    }

    @Test
    public void getTmpDirPath() {
        // 获取临时文件路径（绝对路径）
        assertNotNull(FileKit.getTmpDirPath());
    }

    @Test
    public void getTmpDir() {
        // 获取临时文件目录
        assertNotNull(FileKit.getTmpDir());
    }

    @Test
    public void getUserHomePath() {
        // 获取用户路径（绝对路径）
        assertNotNull(FileKit.getUserHomePath());
    }

    @Test
    public void getUserHomeDir() {
        // 获取用户目录
        assertNotNull(FileKit.getUserHomeDir());
    }

    @Test
    public void exist() {
        // 判断文件是否存在，如果path为null，则返回false
        String path1 = "E:\\Download\\FileTest\\1.txt";
        String path2 = "E:\\Download\\FileTest\\3.txt";
        assertTrue(FileKit.exist(path1));
        assertFalse(FileKit.exist(path2));
    }

    @Test
    public void exist1() {
        //  判断文件是否存在，如果file为null，则返回false
        File file1 = new File("E:\\Download\\FileTest\\1.txt");
        File file2 = new File("E:\\Download\\FileTest\\3.txt");
        assertTrue(FileKit.exist(file1));
        assertFalse(FileKit.exist(file2));
    }

    @Test
    public void exist2() {
        //  是否存在匹配文件  directory 文件夹路径  regexp  文件夹中所包含文件名的正则表达式
        String directory = "E:\\Download\\FileTest";
        String regexp = "^.+\\.txt";
        assertTrue(FileKit.exist(directory, regexp));
    }

    @Test
    public void lastModifiedTime() {
        //  指定文件最后修改时间
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.lastModifiedTime(file));
    }

    @Test
    public void lastModifiedTime1() {
        //  指定路径文件最后修改时间
        String path = "E:\\Download\\FileTest\\1.txt";
        assertNotNull(FileKit.lastModifiedTime(path));
    }

    @Test
    public void size() {
        //  计算目录或文件的总大小
        File file = new File("E:\\Download");
        assertNotNull(FileKit.size(file));
    }

    @Test
    public void newerThan() {
        //  给定文件或目录的最后修改时间是否晚于给定时间  file 文件或目录  reference 参照文件
        File file = new File("E:\\Download\\FileTest\\1.txt");
        File reference = new File("E:\\Download\\FileTest\\2021615.txt");
        File reference1 = null;
        assertFalse(FileKit.newerThan(file, reference));
        // 文件一定比一个不存在的文件新
        assertTrue(FileKit.newerThan(file, reference1));
    }

    @Test
    public void newerThan1() {
        //  给定文件或目录的最后修改时间是否晚于给定时间  file 文件或目录  timeMillis 做为对比的时间
        File file = new File("E:\\Download\\FileTest\\1.txt");
        Date date = new Date();
        SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        long s = 0;
        String time = sim.format(date);
        try {
            s = sim.parse(time).getTime();
            assertFalse(FileKit.newerThan(file, s));
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void touch() {
        //  创建文件及其父目录，如果这个文件存在，直接返回这个文件  fullFilePath 文件的全路径
        String fullFilePath = "E:\\Download\\FileTest\\1.txt";
        assertNotNull(FileKit.touch(fullFilePath));
    }

    @Test
    public void touch1() {
        //  创建文件及其父目录，如果这个文件存在，直接返回这个文件   file 文件对象
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.touch(file));
    }

    @Test
    public void touchDir() {
        //  创建文件及其父目录，如果这个文件存在，直接返回这个文件
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.touchDir(file));
    }

    @Test
    public void touch2() {
        //  创建文件及其父目录，如果这个文件存在，直接返回这个文件  parent 父文件对象  path  文件路径
        File parent = new File("E:\\Download\\FileTest");
        String path = "\\1.txt";
        assertNotNull(FileKit.touch(parent, path));
    }

    @Test
    public void touch3() {
        //  创建文件及其父目录，如果这个文件存在，直接返回这个文件  parent 父文件对象  path  文件路径
        String parent = "E:\\Download\\FileTest";
        String path = "\\1.txt";
        assertNotNull(FileKit.touch(parent, path));
    }

    @Test
    public void mkParentDirs() {
        //  创建所给文件或目录的父目录
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.mkParentDirs(file));
    }

    @Test
    public void mkParentDirs1() {
        //  创建父文件夹，如果存在直接返回此文件夹  path 文件夹路径
        String path = "E:\\Download\\FileTest\\1.txt";
        assertNotNull(FileKit.mkParentDirs(path));
    }

    @Test
    public void mkdir() {
        //  创建文件夹，如果存在直接返回此文件夹 dirPath 文件夹路径
        String dirPath = "E:\\Download\\FileTest";
        assertNotNull(FileKit.mkdir(dirPath));
    }

    @Test
    public void mkdir1() {
        //  创建文件夹，会递归自动创建其不存在的父文件夹，如果存在直接返回此文件夹 dir 目录
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.mkdir(file));
    }

    @Test
    public void createTempFile() {
        //   创建临时文件  创建后的文件名为 prefix[Randon].tmp  dir 临时文件创建的所在目录
        File dir = new File("E:\\Download\\FileTest");
        assertNotNull(FileKit.createTempFile(dir));
    }

    @Test
    public void createTempFile1() {
        //   创建临时文件  isReCreat 是否重新创建文件（删掉原来的，创建新的）
        File dir = new File("E:\\Download\\FileTest");
        assertNotNull(FileKit.createTempFile(dir, false));
    }


    @Test
    public void createTempFile2() {
        //  创建临时文件  创建后的文件名为 prefix[Randon].suffix From com.data.io.FileUtil
        //  prefix  前缀，至少3个字符 suffix 后缀，如果null则使用默认.tmp  dir 临时文件创建的所在目录  isReCreat 是否重新创建文件
        File dir = new File("E:\\Download\\FileTest");
        assertNotNull(FileKit.createTempFile("xxx", "zzz", dir, false));
    }


    @Test
    public void copy() {
        // 复制文件或目录  srcPath 源文件或目录  destPath 目标文件或目录，目标不存在会自动创建（目录、文件都创建） isOverride 是否覆盖目标文件
        String srcPath = "E:\\Download\\FileTest\\2021615.txt";
        String destPath = "E:\\Download\\FileTest\\2021615Copy.txt";
        assertNotNull(FileKit.copy(srcPath, destPath, false));
    }

    @Test
    public void copy1() {
        // 复制文件或目录
        File src = new File("E:\\Download\\FileTest\\1.txt");
        File dest = new File("E:\\Download\\FileTest\\1.txtCopy");
        assertNotNull(FileKit.copy(src, dest, false));
    }

    @Test
    public void copyContent() {
        // 复制文件或目录
        File src = new File("E:\\Download\\FileTest");
        File dest = new File("E:\\Download\\FileTestCopy");
        assertNotNull(FileKit.copyContent(src, dest, false));
    }

    @Test
    public void copyFilesFromDir() {
        // 复制文件或目录
        File src = new File("E:\\Download\\303.txt");
        File dest = new File("E:\\Download\\303Copy.txt");
        assertNotNull(FileKit.copyFilesFromDir(src, dest, false));
    }

    @Test
    public void move() {
        // 移动文件或者目录  src 源文件或者目录  dest  目标文件或者目录   isOverride 是否覆盖目标，只有目标为文件才覆盖
        File src = new File("E:\\Download\\303Copy.txt");
        File dest = new File("E:\\Download\\moveFile");
        FileKit.move(src, dest, false);
    }

    @Test
    public void getCanonicalPath() {
        // 获取规范的绝对路径
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertEquals("E:\\Download\\FileTest\\1.txt", FileKit.getCanonicalPath(file));
    }

    @Test
    public void getAbsolutePath() {
        // 获取绝对路径
        // 此方法不会判定给定路径是否有效（文件或目录存在）
        String path = "test.txt";
        Class<Resource> baseClass = Resource.class;
        String absolutePath = FileKit.getAbsolutePath(path, baseClass);
        assertNotNull(absolutePath);
    }

    @Test
    public void getAbsolutePath1() {
        // 获取绝对路径，相对于ClassPath的目录
        // 此方法不会判定给定路径是否有效（文件或目录存在）
        String path = "test.txt";
        String absolutePath = FileKit.getAbsolutePath(path);
        assertNotNull(absolutePath);
    }

    @Test
    public void getAbsolutePath2() {
        // 获取标准的绝对路径
        File file = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt");
        String absolutePath = FileKit.getAbsolutePath(file);
        assertNotNull(absolutePath);
    }

    @Test
    public void isAbsolutePath() {
        // 给定路径是否已经是绝对路径
        String path = "D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt";
        String path1 = "test.txt";
        assertTrue(FileKit.isAbsolutePath(path));
        assertFalse(FileKit.isAbsolutePath(path1));
    }

    @Test
    public void isDirectory() {
        // 判断是否为目录，如果path为null，则返回false
        String path = "D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt";
        String path1 = "D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources";
        String path2 = "test.txt";
        String path3 = null;
        assertFalse(FileKit.isDirectory(path));
        assertTrue(FileKit.isDirectory(path1));
        assertFalse(FileKit.isDirectory(path2));
        assertFalse(FileKit.isDirectory(path3));
    }

    @Test
    public void isDirectory1() {
        // 判断是否为目录，如果file为null，则返回false
        File file = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt");
        File file1 = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources");
        File file2 = new File("test.txt");
        File file3 = null;
        assertFalse(FileKit.isDirectory(file));
        assertTrue(FileKit.isDirectory(file1));
        assertFalse(FileKit.isDirectory(file2));
        assertFalse(FileKit.isDirectory(file3));
    }

    @Test
    public void isFile() {
        // 判断是否为文件，如果path为null，则返回false
        String path = "D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt";
        String path1 = "D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources";
        String path2 = "test.txt";
        String path3 = null;
        assertTrue(FileKit.isFile(path));
        assertFalse(FileKit.isFile(path1));
        assertTrue(FileKit.isFile(path2));
        assertFalse(FileKit.isFile(path3));
    }

    @Test
    public void isFile1() {
        // 判断是否为文件，如果file为null，则返回false
        File file = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt");
        File file1 = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources");
        File file2 = new File("test.txt");
        File file3 = null;
        assertTrue(FileKit.isFile(file));
        assertFalse(FileKit.isFile(file1));
        assertFalse(FileKit.isFile(file2));
        assertFalse(FileKit.isFile(file3));
    }

    @Test
    public void pathEquals() {
        // 文件路径是否相同
        // 取两个文件的绝对路径比较，在Windows下忽略大小写，在Linux下不忽略
        File file = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt");
        File file1 = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt");
        File file2 = new File("D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources");
        assertTrue(FileKit.pathEquals(file, file1));
        assertFalse(FileKit.pathEquals(file, file2));
    }

    @Test
    public void lastIndexOfSeparator() {
        //  获得最后一个文件路径分隔符的位置
        String path = "D:\\DHW\\LCZ\\server\\lczLib\\HappyCore\\src\\test\\resources\\test.txt";
        assertEquals(53, FileKit.lastIndexOfSeparator(path));
    }

    @Test
    public void isModifed() {
        //  判断文件是否被改动  file 文件对象   lastModifyTime 上次的改动时间
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        Date date = new Date();
        SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        long s = 0;
        String time = sim.format(date);
        try {
            s = sim.parse(time).getTime();
            System.out.println(FileKit.isModified(file, s));
            assertTrue(FileKit.isModified(file, s));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void subPath() {
        //  获得相对子路径
        String rootDir = "E:\\Download\\FileTest";
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertEquals("1.txt", FileKit.subPath(rootDir, file));
    }

    @Test
    public void subPath1() {
        //  获得相对子路径
        String rootDir = "E:\\Download\\FileTest";
        String filePath = "E:\\Download\\FileTest\\1.txt";
        assertEquals("1.txt", FileKit.subPath(rootDir, filePath));
    }

    @Test
    public void getName() {
        //  返回文件名
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertEquals("1.txt", FileKit.getName(file));
    }

    @Test
    public void getName1() {
        //  返回文件名
        String filePath = "E:\\Download\\FileTest\\1.txt";
        assertEquals("1.txt", FileKit.getName(filePath));
    }

    @Test
    public void mainName() {
        //  返回主文件名
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertEquals("1", FileKit.mainName(file));
    }

    @Test
    public void mainName1() {
        //  返回主文件名
        String fileName = "E:\\Download\\FileTest\\1.txt";
        assertEquals("1", FileKit.mainName(fileName));
    }

    @Test
    public void extName() {
        //  获取文件扩展名，扩展名不带“.”
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertEquals("txt", FileKit.extName(file));
    }

    @Test
    public void extName1() {
        //  返回主文件名
        String fileName = "E:\\Download\\FileTest\\1.txt";
        assertEquals("txt", FileKit.extName(fileName));
    }

    @Test
    public void pathEndsWith() {
        //  判断文件路径是否有指定后缀，忽略大小写
        File file = new File("E:\\Download\\FileTest\\1.txt");
        String suffix = ".txt";
        assertTrue(FileKit.pathEndsWith(file, suffix));
    }

    @Test
    public void getInputStream() {
        //  获得输入流
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.getInputStream(file));
    }

    @Test
    public void getInputStream1() {
        //  获得输入流
        String path = "E:\\Download\\FileTest\\1.txt";
        assertNotNull(FileKit.getInputStream(path));
    }

    @Test
    public void getUtf8Reader() {
        //  获得一个文件读取器
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.getUtf8Reader(file));
    }

    @Test
    public void getUtf8Reader1() {
        //  获得一个文件读取器
        String path = "E:\\Download\\FileTest\\1.txt";
        assertNotNull(FileKit.getUtf8Reader(path));
    }

    @Test
    public void getReader() {
        //  获得一个文件读取器  file  文件  charsetName 字符集
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.getReader(file, "UTF-8"));
    }

    @Test
    public void getReader1() {
        //  获得一个文件读取器   file  文件   charset 字符集
        File file = new File("E:\\Download\\FileTest\\1.txt");
        assertNotNull(FileKit.getReader(file, Charset.forName("UTF-8")));
    }

    @Test
    public void getReader2() {
        //  获得一个文件读取器   path  绝对路径   charset 字符集
        String path = "E:\\Download\\FileTest\\1.txt";
        assertNotNull(FileKit.getReader(path, "UTF-8"));
    }

    @Test
    public void getReader3() {
        //  获得一个文件读取器   path  绝对路径   charset 字符集
        String path = "E:\\Download\\FileTest\\1.txt";
        assertNotNull(FileKit.getReader(path, Charset.forName("UTF-8")));
    }

    @Test
    public void readBytes() {
        //  读取文件所有数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.readBytes(file));
    }

    @Test
    public void readBytes1() {
        //  读取文件所有数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.readBytes(filePath));
    }

    @Test
    public void readUtf8String() {
        //  读取文件所有数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.readUtf8String(file));
    }

    @Test
    public void readUtf8String1() {
        //  读取文件所有数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.readUtf8String(filePath));
    }

    @Test
    public void readString() {
        //  读取文件内容
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.readString(file, "UTF-8"));
    }

    @Test
    public void readString1() {
        //  读取文件内容
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.readString(file, Charset.forName("UTF-8")));
    }

    @Test
    public void readString2() {
        //  读取文件内容
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.readString(filePath, "UTF-8"));
    }

    @Test
    public void readString3() {
        //  读取文件内容
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.readString(filePath, Charset.forName("UTF-8")));
    }

    @Test
    public void readString4() throws IOException {
        //  读取文件内容
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        URL url = file.toURL();
        assertNotNull(FileKit.readString(url, "UTF-8"));
    }

    @Test
    public void readUtf8Lines() {
        //  从文件中读取每一行的UTF-8编码数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        Set set = new HashSet();
        assertNotNull(FileKit.readUtf8Lines(filePath, set));
    }

    @Test
    public void readUtf8Lines1() {
        //  从文件中读取每一行的UTF-8编码数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        Set set = new HashSet();
        assertNotNull(FileKit.readUtf8Lines(file, set));
    }

    @Test
    public void readUtf8Lines2() throws IOException {
        //  从文件中读取每一行的UTF-8编码数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        URL url = file.toURL();
        Set set = new HashSet();
        assertNotNull(FileKit.readUtf8Lines(url, set));
    }

    @Test
    public void readUtf8Lines3() throws IOException {
        //  从文件中读取每一行的UTF-8编码数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        URL url = file.toURL();
        assertNotNull(FileKit.readUtf8Lines(url));
    }

    @Test
    public void readUtf8Lines4() {
        //  从文件中读取每一行数据，编码为UTF-8
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.readUtf8Lines(filePath));
    }

    @Test
    public void readUtf8Lines5() {
        //  从文件中读取每一行数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.readUtf8Lines(file));
    }

    @Test
    public void readUtf8Lines6() {
        //  按行处理文件内容，编码为UTF-8
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        LineHandler lineHandler = new LineHandler() {
            @Override
            public void handle(String line) {
                System.out.println(line);
                String convertString = CharsetKit.convert(line, "UTF-8", "GBK");
                System.out.println(convertString);
            }
        };
        FileKit.readUtf8Lines(file, lineHandler);
    }


    @Test
    public void readLines() {
        //  从文件中读取每一行数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        Set set = new HashSet();
        assertNotNull(FileKit.readLines(filePath, "UTF-8", set));
    }

    @Test
    public void readLines1() {
        //  从文件中读取每一行数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        Set set = new HashSet();
        assertNotNull(FileKit.readLines(filePath, Charset.forName("UTF-8"), set));
    }

    @Test
    public void readLines2() throws IOException {
        //  从文件中读取每一行数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        URL url = file.toURL();
        Set set = new HashSet();
        assertNotNull(FileKit.readLines(url, "UTF-8", set));
    }

    @Test
    public void readLines3() throws IOException {
        //  从文件中读取每一行数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        URL url = file.toURL();
        Set set = new HashSet();
        assertNotNull(FileKit.readLines(url, Charset.forName("UTF-8"), set));
    }

    @Test
    public void readLines4() throws IOException {
        //  从文件中读取每一行数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        URL url = file.toURL();
        assertNotNull(FileKit.readLines(url, "UTF-8"));
    }

    @Test
    public void readLines5() throws IOException {
        //  从文件中读取每一行数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        URL url = file.toURL();
        assertNotNull(FileKit.readLines(url, Charset.forName("UTF-8")));
    }

    @Test
    public void readLines6() {
        //  从文件中读取每一行数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.readLines(filePath, "UTF-8"));
    }

    @Test
    public void readLines7() {
        //  从文件中读取每一行数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.readLines(filePath, Charset.forName("UTF-8")));
    }

    @Test
    public void readLines8() {
        //  从文件中读取每一行数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.readLines(file, "UTF-8"));
    }

    @Test
    public void readLines9() {
        //  从文件中读取每一行数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.readLines(file, Charset.forName("UTF-8")));
    }

    @Test
    public void readLines10() {
        //  按行处理文件内容
        File file = new File("E:\\Download\\FileTest\\2021615Copy.txt");
        FileKit.readLines(file, Charset.forName("GBK"), new LineHandler() {
            @Override
            public void handle(String line) {
                System.out.println(line);
                String convertString = CharsetKit.convert(line, "UTF-8", "GBK");
                System.out.println(convertString);
            }
        });
    }

    @Test
    public void loadUtf8() {
        //  按照给定的readerHandler读取文件中的数据
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        System.out.println(FileKit.loadUtf8(filePath, new FileReader.ReaderHandler<String>() {
            @Override
            public String handle(BufferedReader reader) throws IOException {
                return reader.readLine();
            }
        }));

    }

    @Test
    public void loadUtf81() {
        //  按照给定的readerHandler读取文件中的数据
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        System.out.println(FileKit.loadUtf8(file, new FileReader.ReaderHandler<String>() {
            @Override
            public String handle(BufferedReader reader) throws IOException {
                return reader.readLine();
            }
        }));
    }

    @Test
    public void load() {
        //  按照给定的readerHandler读取文件中的数据
        String filePath = "E:\\Download\\FileTest\\2021615Copy.txt";
        System.out.println(FileKit.load(filePath, "GBK", new FileReader.ReaderHandler<String>() {
            @Override
            public String handle(BufferedReader reader) throws IOException {
                return reader.readLine();
            }
        }));
    }

    @Test
    public void load1() {
        //  按照给定的readerHandler读取文件中的数据
        String filePath = "E:\\Download\\FileTest\\2021615Copy.txt";
        System.out.println(FileKit.load(filePath, Charset.forName("GBK"), new FileReader.ReaderHandler<String>() {
            @Override
            public String handle(BufferedReader reader) throws IOException {
                return reader.readLine();
            }
        }));
    }

    @Test
    public void load2() {
        //  按照给定的readerHandler读取文件中的数据
        File file = new File("E:\\Download\\FileTest\\2021615Copy.txt");
        System.out.println(FileKit.load(file, Charset.forName("GBK"), new FileReader.ReaderHandler<String>() {
            @Override
            public String handle(BufferedReader reader) throws IOException {
                return reader.readLine();
            }
        }));
    }

    @Test
    public void getOutputStream() {
        //  获得一个输出流对象
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.getOutputStream(file));
    }

    @Test
    public void getOutputStream1() {
        //  获得一个输出流对象
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.getOutputStream(path));
    }

    @Test
    public void getWriter() {
        //  获得一个带缓存的写入对象  isAppend  是否追加
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.getWriter(path, "UTF-8", true));
    }

    @Test
    public void getWriter1() {
        //  获得一个带缓存的写入对象  isAppend  是否追加
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.getWriter(path, Charset.forName("UTF-8"), true));
    }

    @Test
    public void appendUtf8String() {
        //  将String写入文件，UTF-8编码追加模式
        String content = "新增文本1";
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.appendUtf8String(content, path));
    }

    @Test
    public void appendUtf8String1() {
        //  将String写入文件，UTF-8编码追加模式
        String content = "新增文本2";
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.appendUtf8String(content, file));
    }

    @Test
    public void appendString() {
        //  将String写入文件，追加模式
        String content = "新增文本3";
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.appendString(content, path, "UTF-8"));
    }

    @Test
    public void appendString1() {
        //  将String写入文件，追加模式
        String content = "新增文本4";
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.appendString(content, path, Charset.forName("UTF-8")));
    }

    @Test
    public void appendString2() {
        //  将String写入文件，追加模式
        String content = "新增文本5";
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.appendString(content, file, "UTF-8"));
    }

    @Test
    public void appendString3() {
        //  将String写入文件，追加模式
        String content = "新增文本6";
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.appendString(content, file, Charset.forName("UTF-8")));
    }

    @Test
    public void writeUtf8Lines() {
        //  将列表写入文件，覆盖模式，编码为UTF-8
        List list = new ArrayList();
        list.add("writeUtf8Lines列表内容1(覆盖模式)");
        list.add("writeUtf8Lines列表内容2(覆盖模式)");
        list.add("writeUtf8Lines列表内容3(覆盖模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.writeUtf8Lines(list, path));
    }

    @Test
    public void writeUtf8Lines1() {
        //  将列表写入文件，覆盖模式，编码为UTF-8
        List list = new ArrayList();
        list.add("writeUtf8Lines1列表内容1(覆盖模式)");
        list.add("writeUtf8Lines1列表内容2(覆盖模式)");
        list.add("writeUtf8Lines1列表内容3(覆盖模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeUtf8Lines(list, file));
    }

    @Test
    public void writeLines() {
        //  将列表写入文件，覆盖模式
        List list = new ArrayList();
        list.add("writeLines列表内容1(覆盖模式)");
        list.add("writeLines列表内容2(覆盖模式)");
        list.add("writeLines列表内容3(覆盖模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.writeLines(list, path, "UTF-8"));
    }

    @Test
    public void writeLines1() {
        //  将列表写入文件，覆盖模式
        List list = new ArrayList();
        list.add("writeLines1列表内容1(覆盖模式)");
        list.add("writeLines1列表内容2(覆盖模式)");
        list.add("writeLines1列表内容3(覆盖模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.writeLines(list, path, Charset.forName("UTF-8")));
    }

    @Test
    public void writeLines3() {
        //  将列表写入文件，覆盖模式
        List list = new ArrayList();
        list.add("writeLines3列表内容1(覆盖模式)");
        list.add("writeLines3列表内容2(覆盖模式)");
        list.add("writeLines3列表内容3(覆盖模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeLines(list, file, "UTF-8"));
    }

    @Test
    public void writeLines4() {
        //  将列表写入文件，覆盖模式
        List list = new ArrayList();
        list.add("writeLines4列表内容1(覆盖模式)");
        list.add("writeLines4列表内容2(覆盖模式)");
        list.add("writeLines4列表内容3(覆盖模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeLines(list, file, Charset.forName("UTF-8")));
    }

    @Test
    public void writeLines5() {
        //  将列表写入文件， isAppend 是否追加（true追加 false覆盖）
        List list = new ArrayList();
        list.add("writeLines5列表内容1(覆盖模式)");
        list.add("writeLines5列表内容2(覆盖模式)");
        list.add("writeLines5列表内容3(覆盖模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.writeLines(list, path, "UTF-8", false));
    }

    @Test
    public void writeLines6() {
        //  将列表写入文件， isAppend 是否追加（true追加 false覆盖）
        List list = new ArrayList();
        list.add("writeLines6列表内容1(覆盖模式)");
        list.add("writeLines6列表内容2(覆盖模式)");
        list.add("writeLines6列表内容3(覆盖模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.writeLines(list, path, Charset.forName("UTF-8"), true));
    }

    @Test
    public void writeLines7() {
        //  将列表写入文件， isAppend 是否追加（true追加 false覆盖）
        List list = new ArrayList();
        list.add("writeLines7列表内容1(覆盖模式)");
        list.add("writeLines7列表内容2(覆盖模式)");
        list.add("writeLines7列表内容3(覆盖模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeLines(list, file, "UTF-8", true));
    }

    @Test
    public void writeLines8() {
        //  将列表写入文件， isAppend 是否追加（true追加 false覆盖）
        List list = new ArrayList();
        list.add("writeLines8列表内容1(覆盖模式)");
        list.add("writeLines8列表内容2(覆盖模式)");
        list.add("writeLines8列表内容3(覆盖模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeLines(list, file, Charset.forName("UTF-8"), false));
    }

    @Test
    public void appendUtf8Lines() {
        //  将列表写入文件，追加模式
        List list = new ArrayList();
        list.add("appendUtf8Lines列表内容1(追加模式)");
        list.add("appendUtf8Lines列表内容2(追加模式)");
        list.add("appendUtf8Lines列表内容3(追加模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.appendUtf8Lines(list, file));
    }

    @Test
    public void appendUtf8Lines1() {
        //  将列表写入文件，追加模式
        List list = new ArrayList();
        list.add("appendUtf8Lines1列表内容1(追加模式)");
        list.add("appendUtf8Lines1列表内容2(追加模式)");
        list.add("appendUtf8Lines1列表内容3(追加模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.appendUtf8Lines(list, path));
    }

    @Test
    public void appendLines() {
        //  将列表写入文件，追加模式
        List list = new ArrayList();
        list.add("appendLines列表内容1(追加模式)");
        list.add("appendLines列表内容2(追加模式)");
        list.add("appendLines列表内容3(追加模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.appendLines(list, path, "UTF-8"));
    }

    @Test
    public void appendLines1() {
        //  将列表写入文件，追加模式
        List list = new ArrayList();
        list.add("appendLines1列表内容1(追加模式)");
        list.add("appendLines1列表内容2(追加模式)");
        list.add("appendLines1列表内容3(追加模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.appendLines(list, file, "UTF-8"));
    }

    @Test
    public void appendLines2() {
        //  将列表写入文件，追加模式
        List list = new ArrayList();
        list.add("appendLines2列表内容1(追加模式)");
        list.add("appendLines2列表内容2(追加模式)");
        list.add("appendLines2列表内容3(追加模式)");
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.appendLines(list, path, Charset.forName("UTF-8")));
    }

    @Test
    public void appendLines3() {
        //  将列表写入文件，追加模式
        List list = new ArrayList();
        list.add("appendLines3列表内容1(追加模式)");
        list.add("appendLines3列表内容2(追加模式)");
        list.add("appendLines3列表内容3(追加模式)");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.appendLines(list, file, Charset.forName("UTF-8")));
    }

    @Test
    public void writeUtf8Map() {
        //  将Map写入文件，每个键值对为一行，一行中键与值之间使用kvSeparator分隔
        //  kvSeparator 键和值之间的分隔符，如果传入null使用默认分隔符" = "
        Map map = new HashMap();
        map.put("1", "writeUtf8Map内容1");
        map.put("2", "writeUtf8Map内容2");
        map.put("3", "writeUtf8Map内容3");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeUtf8Map(map, file, null, true));
    }

    @Test
    public void writeMap() {
        //  将Map写入文件，每个键值对为一行，一行中键与值之间使用kvSeparator分隔
        //  kvSeparator 键和值之间的分隔符，如果传入null使用默认分隔符" = "
        Map map = new HashMap();
        map.put("1", "writeMap内容1");
        map.put("2", "writeMap内容2");
        map.put("3", "writeMap内容3");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeMap(map, file, Charset.forName("UTF-8"), "====", true));
    }

    @Test
    public void writeBytes() {
        // 写数据到文件中
        byte[] data = {97, 98, 99, 100, 101};
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.writeBytes(data, path));
    }

    @Test
    public void writeBytes1() {
        // 写数据到文件中
        byte[] data = {97, 98, 99, 100, 101, 102};
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeBytes(data, file));
    }

    @Test
    public void writeBytes2() {
        // 写数据到文件中  off 数据开始位置   len  数据长度   isAppend 是否追加模式
        byte[] data = {97, 98, 99, 100, 101, 102, 103};
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeBytes(data, file, 0, 7, true));
    }

    @Test
    public void writeFromStream() throws IOException {
        // 将流的内容写入文件
        URL url = new URL("http://www.baidu.com");
        InputStream in = url.openStream();
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeFromStream(in, file));
    }

    @Test
    public void writeFromStream1() throws IOException {
        // 将流的内容写入文件
        URL url = new URL("http://www.baidu.com");
        InputStream in = url.openStream();
        String fullFilePath = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.writeFromStream(in, fullFilePath));
    }

    @Test
    public void writeToStream() throws IOException {
        // 将流的内容写入文件
        OutputStream out = null;
        File f = new File("E:\\Download\\FileTest\\2021615Copy.txt");
        out = new FileOutputStream(f);
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.writeToStream(file, out));
    }

    @Test
    public void writeToStream1() throws IOException {
        // 将流的内容写入文件
        OutputStream out = null;
        File f = new File("E:\\Download\\FileTest\\2021615Copy.txt");
        out = new FileOutputStream(f);
        String fullFilePath = "E:\\Download\\FileTest\\2021615.txt";
        FileKit.writeToStream(fullFilePath, out);
    }

    @Test
    public void readableFileSize() {
        // 可读的文件大小
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        System.out.println(FileKit.readableFileSize(file));
    }

    @Test
    public void readableFileSize1() {
        // Long类型大小
        System.out.println(FileKit.readableFileSize(854654));
    }

    @Test
    public void convertCharset() {
        // 转换文件编码
        // 此方法用于转换文件编码，读取的文件实际编码必须与指定的srcCharset编码一致，否则导致乱码
        // file 文件  srcCharset  原文件的编码，必须与文件内容的编码保持一致  destCharset 转码后的编码
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.convertCharset(file, Charset.forName("GBK"), Charset.forName("UTF-8")));
    }

    @Test
    public void cleanInvalid() {
        // 清除文件名中的在Windows下不支持的非法字符，包括： \ / : * ? " &lt; &gt; |
        String fileName = "E:\\Download\\FileTest\\2021615.txt";
        System.out.println(FileKit.cleanInvalid(fileName));
    }

    @Test
    public void containsInvalid() {
        // 文件名中是否包含在Windows下不支持的非法字符，包括： \ / : * ? " &lt; &gt; |
        String fileName = "E:\\Download\\FileTest\\2021615.txt";
        assertTrue(FileKit.containsInvalid(fileName));
    }

    @Test
    public void checksumCRC32() {
        // 计算文件CRC32校验码
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.checksumCRC32(file));
    }

    @Test
    public void checksum() {
        // 计算文件校验码
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        Checksum checksum = new CRC32();
        assertNotNull(FileKit.checksum(file, checksum));
    }

    @Test
    public void getWebRoot() {
        // 获取Web项目下的web root路径
        // 原理是首先获取ClassPath路径，由于在web项目中ClassPath位于 WEB-INF/classes/下，故向上获取两级目录即可
        assertNotNull(FileKit.getWebRoot());
    }

    @Test
    public void getParent() {
        // 获取指定层级的父路径  filePath 目录或文件路径  level    层级
        String filePath = "E:\\Download\\FileTest\\2021615.txt";
        assertEquals("E:\\Download\\FileTest\\2021615.txt", FileKit.getParent(filePath, 0));
        assertEquals("E:\\Download\\FileTest", FileKit.getParent(filePath, 1));
        assertEquals("E:\\Download", FileKit.getParent(filePath, 2));
        assertEquals("E:\\", FileKit.getParent(filePath, 3));
        assertEquals(null, FileKit.getParent(filePath, 4));
    }

    @Test
    public void getParent1() {
        // 获取指定层级的父路径  filePath 目录或文件路径  level    层级
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.getParent(file, 0));
        assertNotNull(FileKit.getParent(file, 1));
        assertNotNull(FileKit.getParent(file, 2));
        assertNotNull(FileKit.getParent(file, 3));
        assertNull(FileKit.getParent(file, 4));
    }

    @Test
    public void checkSlip() {
        // 检查父完整路径是否为自路径的前半部分，如果不是说明不是子路径，可能存在slip注入
        File parentFile = new File("E:\\Download\\FileTest");
        File file = new File("E:\\Download\\FileTest\\2021615.txt");
        assertNotNull(FileKit.checkSlip(parentFile, file));
    }

    @Test
    public void getMimeType() {
        // 根据文件扩展名获得MimeType
        String path = "E:\\Download\\FileTest\\2021615.txt";
        assertNotNull(FileKit.getMimeType(path));
    }

}


