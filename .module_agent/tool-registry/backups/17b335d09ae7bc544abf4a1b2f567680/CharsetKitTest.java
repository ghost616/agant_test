package com.datanew.core.toolkit;

import com.google.common.base.Utf8;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

public class CharsetKitTest {

    @Test
    public void charset(){

        // 转换为Charset对象
        Charset utf = CharsetKit.charset("UTF-8");
        Charset iso = CharsetKit.charset("ISO-8859-1");
        Charset gbk = CharsetKit.charset("GBK");
        assertNotNull(utf);
        assertNotNull(iso);
        assertNotNull(gbk);

    }

    @Test
    public void convert(){

        // 转换字符串的字符集编码
        // source  字符串  srcCharset  源字符集，默认ISO-8859-1  destCharset 目标字符集，默认UTF-8
        String convertContext = CharsetKit.convert("转换文字", CharsetKit.UTF_8, CharsetKit.GBK);
        String convertContext1 = CharsetKit.convert("杞\uE101崲鏂囧瓧", CharsetKit.GBK, CharsetKit.UTF_8);
        assertEquals("杞\uE101崲鏂囧瓧",convertContext);
        assertEquals("转换文字",convertContext1);

    }


    @Test
    public void convert1() throws IOException {

        // 转换文件编码
        String data = "转换文字";
        String fileName = "charsetTest.txt";
        File testFile = new File("E:" + File.separator + "filepath" + File.separator + "test" + File.separator + fileName);
        File fileParent = testFile.getParentFile();

        if (!fileParent.exists()) {
            // 能创建多级目录
            fileParent.mkdirs();
        }
        if (!testFile.exists())
            //有路径才能创建文件
            testFile.createNewFile();

        //在E:\filepath\test\charsetTest.txt 中写入UTF-8形式的文本”转换文字“
        String result = "";
        FileWriter fileWriter = new FileWriter(testFile);
        fileWriter.write(data);
        fileWriter.close();

        //用BufferedReader读出charsetTest.txt的文字内容
        BufferedReader br = new BufferedReader(new FileReader(testFile));
        String s = null;
        while ((s = br.readLine()) != null){
            result = result +s;
        }

        assertEquals("转换文字",result);

        //转换文件当中的文字编码 从“UTF-8”转为“GBK”
        CharsetKit.convert(testFile,Charset.forName("GBK"),Charset.forName("UTF-8"));

        //用BufferedReader读出转换后的charsetTest.txt的文字内容
        String result1 = "";
        BufferedReader br1 = new BufferedReader(new FileReader(testFile));
        String s1 = null;
        while ((s1 = br1.readLine()) != null){
            result1 = result1 +s1;
        }
        br.close();
        br1.close();

        assertEquals("杞\uE101崲鏂囧瓧",result1);

    }

    @Test
    public void systemCharsetName(){

        // 系统字符集编码，如果是Windows，则默认为GBK编码
        String s = CharsetKit.systemCharsetName();
        assertEquals("GBK",s);

    }

    @Test
    public void defaultCharsetName(){

        // 系统默认字符集编码
        String s = CharsetKit.defaultCharsetName();
        System.out.println(s);
        assertEquals("UTF-8",s);

    }

}
