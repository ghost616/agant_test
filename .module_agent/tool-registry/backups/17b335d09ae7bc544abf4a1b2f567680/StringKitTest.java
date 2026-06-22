package com.datanew.core.toolkit;


import com.datanew.core.lang.Matcher;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.*;

public class StringKitTest {

    @Test
    public void isBlank() {
        String str = "";
        assertTrue(StringKit.isBlank(str));
        String str2 = " ";
        assertTrue(StringKit.isBlank(str2));
        String str3 = null;
        assertTrue(StringKit.isBlank(str3));
        String str4 = "123";
        assertFalse(StringKit.isBlank(str4));
        String str5 = " 2 ";
        assertFalse(StringKit.isBlank(str5));
    }

    @Test
    public void hasText() {
        String text = "哈 哈 哈";
        assertTrue(StringKit.hasText(text));
        String text2 = "   ";
        assertFalse(StringKit.hasText(text2));
        String text3 = null;
        assertFalse(StringKit.hasText(text3));
    }

    @Test
    public void isBlankIfStr() {
        int num = 123;
        assertFalse(StringKit.isBlankIfStr(num));
        String str = "123";
        assertFalse(StringKit.isBlankIfStr(str));
        String str2 = null;
        assertTrue(StringKit.isBlankIfStr(str2));
        String str3 = "";
        assertTrue(StringKit.isBlankIfStr(str3));
        String str4 = " ";
        assertTrue(StringKit.isBlankIfStr(str4));

    }

    @Test
    public void isNotBlank() {
        String str = "";
        assertFalse(StringKit.isNotBlank(str));
        String str2 = " ";
        assertFalse(StringKit.isNotBlank(str2));
        String str3 = null;
        assertFalse(StringKit.isNotBlank(str3));
        String str4 = "123";
        assertTrue(StringKit.isNotBlank(str4));
    }

    @Test
    public void hasBlank() {
        String[] strings = new String[]{"1 1", "2 2", "3 3"};
        assertFalse(StringKit.hasBlank(strings));
        String[] strings1 = new String[]{};
        assertTrue(StringKit.hasBlank(strings1));
        String[] strings2 = new String[]{"11", "22", "33"};
        assertFalse(StringKit.hasBlank(strings2));
        String[] strings3 = new String[]{""};
        assertTrue(StringKit.hasBlank(strings3));
        String[] strings4 = new String[]{null};
        assertTrue(StringKit.hasBlank(strings4));
        String[] strings5 = new String[]{"11", "22", "33", "", "4  4"};
        assertTrue(StringKit.hasBlank(strings5));
        String[] strings6 = new String[]{"11", "22", "33", null, "4  4"};
        assertTrue(StringKit.hasBlank(strings6));

    }

    @Test
    public void isEmpty() {
        String str = "";
        assertTrue(StringKit.isEmpty(str));
        String str1 = "1";
        assertFalse(StringKit.isEmpty(str1));
        String str2 = null;
        assertTrue(StringKit.isEmpty(str2));
        String str3 = "  ";
        assertFalse(StringKit.isEmpty(str3));
    }

    @Test
    public void hasLength() {
        // 不为null或者长度大于0则返回true，其他返回false

        String str = "";
        assertFalse(StringKit.hasLength(str));
        String str1 = "1";
        assertTrue(StringKit.hasLength(str1));
        String str2 = null;
        assertFalse(StringKit.hasLength(str2));
        String str3 = "  ";
        assertTrue(StringKit.hasLength(str3));
    }

    @Test
    public void isEmptyIfStr() {
        int num = 123;
        assertFalse(StringKit.isEmptyIfStr(num));
        String str = "123";
        assertFalse(StringKit.isEmptyIfStr(str));
        String str2 = null;
        assertTrue(StringKit.isEmptyIfStr(str2));
        String str3 = "";
        assertTrue(StringKit.isEmptyIfStr(str3));
        String str4 = "  ";
        assertFalse(StringKit.isEmptyIfStr(str4));
    }

    @Test
    public void isNotEmpty() {
        String str = "";
        assertFalse(StringKit.isNotEmpty(str));
        String str1 = "1";
        assertTrue(StringKit.isNotEmpty(str1));
        String str2 = null;
        assertFalse(StringKit.isNotEmpty(str2));
        String str3 = "  ";
        assertTrue(StringKit.isNotEmpty(str3));
    }

    @Test
    public void nullToEmpty() {
        //当给定字符串为null时，转换为Empty
        String str = null;
        String str2 = "123";
        String ret = StringKit.nullToEmpty(str);
        String ret2 = StringKit.nullToEmpty(str2);
        assertSame(ret, StringKit.EMPTY);
        assertNotSame(ret2, StringKit.EMPTY);
    }

    @Test
    public void nullToDefault() {
        // 如果字符串是null，则返回指定默认字符串，否则返回字符串本身
        String str = null;
        String str2 = "123";
        String defaultStr1 = StringKit.EMPTY;
        String defaultStr2 = StringKit.EMPTY_JSON;
        assertSame(defaultStr1, StringKit.nullToDefault(str, defaultStr1));
        assertSame(defaultStr2, StringKit.nullToDefault(str, defaultStr2));
        assertSame(str2, StringKit.nullToDefault(str2, defaultStr1));

    }


    @Test
    public void emptyToDefault() {
        //  如果字符串是null或者"";，则返回指定默认字符串，否则返回字符串本身
        String str = null;
        String str2 = "123";
        String str3 = "";
        String defaultStr = "这是指定的默认字符串";
        String ret = StringKit.emptyToDefault(str, defaultStr);
        String ret2 = StringKit.emptyToDefault(str2, defaultStr);
        String ret3 = StringKit.emptyToDefault(str3, defaultStr);
        assertSame(defaultStr, ret);
        assertSame(str2, ret2);
        assertSame(defaultStr, ret3);
    }

    @Test
    public void blankToDefault() {
        // 如果字符串是null或者""或者空白;，则返回指定默认字符串，否则返回字符串本身
        String str = null;
        String str2 = "123";
        String str3 = "";
        String str4 = "             ";
        String defaultStr = "这是指定的默认字符串";
        String ret = StringKit.blankToDefault(str, defaultStr);
        String ret2 = StringKit.blankToDefault(str2, defaultStr);
        String ret3 = StringKit.blankToDefault(str3, defaultStr);
        String ret4 = StringKit.blankToDefault(str4, defaultStr);
        assertSame(defaultStr, ret);
        assertSame(str2, ret2);
        assertSame(defaultStr, ret3);
        assertSame(defaultStr, ret4);
    }

    @Test
    public void emptyToNull() {
        // 当给定字符串为空字符串时，转换为null,否则返回字符串本身。
        String str = null;
        String str2 = "123";
        String str3 = "";
        String ret = StringKit.emptyToNull(str);
        assertNull(ret);
        String ret2 = StringKit.emptyToNull(str2);
        assertNotNull(ret2);
        assertSame(str2, ret2);
        String ret3 = StringKit.emptyToNull(str3);
        assertNull(ret3);

    }

    @Test
    public void hasEmpty() {
        // 是否包含空字符串，包含返回true，否贼返回false
        String[] strings = new String[]{"1 1", "2 2", "3 3"};
        assertFalse(StringKit.hasEmpty(strings));
        String[] strings1 = new String[]{};
        assertTrue(StringKit.hasEmpty(strings1));
        String[] strings2 = new String[]{"11", "22", "33"};
        assertFalse(StringKit.hasEmpty(strings2));
        String[] strings3 = new String[]{""};
        assertTrue(StringKit.hasEmpty(strings3));
        String[] strings4 = new String[]{null};
        assertTrue(StringKit.hasEmpty(strings4));
        String[] strings5 = new String[]{"", "11", "22"};
        assertTrue(StringKit.hasEmpty(strings5));
        String[] strings6 = new String[]{null, "11", "22"};
        assertTrue(StringKit.hasEmpty(strings6));
    }

    @Test
    public void isAllEmpty() {
        // 是否全部为空字符串，如果全部为空字符串返回true，否则返回false
        String[] strings = new String[]{"1 1", "2 2", "3 3"};
        assertFalse(StringKit.isAllEmpty(strings));
        String[] strings1 = new String[]{};
        assertTrue(StringKit.isAllEmpty(strings1));
        String[] strings2 = new String[]{"11", "22", "33"};
        assertFalse(StringKit.isAllEmpty(strings2));
        String[] strings3 = new String[]{""};
        assertTrue(StringKit.isAllEmpty(strings3));
        String[] strings4 = new String[]{null};
        assertTrue(StringKit.isAllEmpty(strings4));
        String[] strings5 = new String[]{"", "11", "22"};
        assertFalse(StringKit.isAllEmpty(strings5));
        String[] strings6 = new String[]{"  "};
        assertFalse(StringKit.isAllEmpty(strings6));
    }

    @Test
    public void isNullOrUndefined() {
        // 检查字符串是否为null、“null”、“undefined”
        String str = null;
        String str2 = "null";
        String str3 = "undefined";
        String str4 = "123";
        assertTrue(StringKit.isNullOrUndefined(str));
        assertTrue(StringKit.isNullOrUndefined(str2));
        assertTrue(StringKit.isNullOrUndefined(str3));
        assertFalse(StringKit.isNullOrUndefined(str4));
    }

    @Test
    public void isEmptyOrUndefined() {
        // 检查字符串是否为null、“”、“null”、“undefined”
        String str = null;
        String str2 = "null";
        String str3 = "undefined";
        String str4 = "123";
        String str5 = "";
        assertTrue(StringKit.isEmptyOrUndefined(str));
        assertTrue(StringKit.isEmptyOrUndefined(str2));
        assertTrue(StringKit.isEmptyOrUndefined(str3));
        assertFalse(StringKit.isEmptyOrUndefined(str4));
        assertTrue(StringKit.isEmptyOrUndefined(str5));

    }

    @Test
    public void isBlankOrUndefined() {
        // 检查字符串是否为null、空白串、“null”、“undefined”
        String str = null;
        String str2 = "null";
        String str3 = "undefined";
        String str4 = "123";
        String str5 = "";
        String str6 = "   ";
        assertTrue(StringKit.isBlankOrUndefined(str));
        assertTrue(StringKit.isBlankOrUndefined(str2));
        assertTrue(StringKit.isBlankOrUndefined(str3));
        assertFalse(StringKit.isBlankOrUndefined(str4));
        assertTrue(StringKit.isBlankOrUndefined(str5));
        assertTrue(StringKit.isBlankOrUndefined(str6));
    }


    @Test
    public void trimLeadingCharacter() {
        //  删除指定文本前端的所有指定字符
        String str = "              le";
        char leadingCharacter = ' ';
        String ret = StringKit.trimLeadingCharacter(str, leadingCharacter);
        assertEquals("le", ret);
        String str1 = "aaaaaaaale";
        char leadingCharacter1 = 'a';
        String ret1 = StringKit.trimLeadingCharacter(str1, leadingCharacter1);
        assertEquals("le", ret1);

    }

    @Test
    public void trimTrailingCharacter() {
        //  删除指定文本后端的所有指定字符
        String str = "apple   ";
        char trailingCharacter = ' ';
        String ret = StringKit.trimTrailingCharacter(str, trailingCharacter);
        assertEquals("apple", ret);
        String str1 = "appleccccc";
        char trailingCharacter1 = 'c';
        String ret1 = StringKit.trimTrailingCharacter(str1, trailingCharacter1);
        assertEquals("apple", ret1);
    }

    @Test
    public void trim() {
        // 除去字符串头尾部的空白，如果字符串是null，依然返回null
        String str = "  apple  ";
        String ret = StringKit.trim(str);
        assertEquals("apple", ret);
        String str2 = null;
        String ret2 = StringKit.trim(str2);
        assertNull(ret2);
    }

    @Test
    public void testTrim() {
        // 给定字符串数组全部做去首尾空格
        String[] strings = new String[]{" a ", " b", "c ", "d"};
        StringKit.trim(strings);
        String[] rets = new String[]{"a", "b", "c", "d"};
        assertArrayEquals(strings, rets);
    }

    @Test
    public void trimToEmpty() {
        // 除去字符串头尾部的空白，如果字符串是null，返回""。
        String str = null;
        String str2 = "";
        String str3 = "     ";
        String str4 = " abc ";
        String ret = StringKit.trimToEmpty(str);
        String ret2 = StringKit.trimToEmpty(str2);
        String ret3 = StringKit.trimToEmpty(str3);
        String ret4 = StringKit.trimToEmpty(str4);
        assertEquals(ret, "");
        assertEquals(ret2, "");
        assertEquals(ret3, "");
        assertEquals(ret4, "abc");
    }

    @Test
    public void trimToNull() {
        // 除去字符串头尾部的空白，如果字符串是空，返回null。
        String str = null;
        String str2 = "";
        String str3 = "     ";
        String str4 = " abc ";
        String str5 = " abc d ";
        String ret = StringKit.trimToNull(str);
        String ret2 = StringKit.trimToNull(str2);
        String ret3 = StringKit.trimToNull(str3);
        String ret4 = StringKit.trimToNull(str4);
        String ret5 = StringKit.trimToNull(str5);
        assertEquals(ret, null);
        assertEquals(ret2, null);
        assertEquals(ret3, null);
        assertEquals(ret4, "abc");
        assertEquals(ret5, "abc d");
    }

    @Test
    public void trimStart() {
        // 除去字符串头部的空白，如果字符串是null，则返回null
        String str = null;
        String str2 = "";
        String str3 = "     ";
        String str4 = " abc ";
        String ret = StringKit.trimStart(str);
        String ret2 = StringKit.trimStart(str2);
        String ret3 = StringKit.trimStart(str3);
        String ret4 = StringKit.trimStart(str4);
        assertNull(ret);
        assertEquals("", ret2);
        assertEquals("", ret3);
        assertEquals(ret4, "abc ");
    }

    @Test
    public void trimEnd() {
        // 除去字符串尾部的空白，如果字符串是null，则返回null
        String str = null;
        String str2 = "";
        String str3 = "     ";
        String str4 = " abc ";
        String ret = StringKit.trimEnd(str);
        // 如果原字串为null或结果字符串为""，则返回null
        String ret2 = StringKit.trimEnd(str2).equals("") ? null : "";
        String ret3 = StringKit.trimEnd(str3).equals("") ? null : "";
        String ret4 = StringKit.trimEnd(str4);
        assertNull(ret);
        assertNull(ret2);
        assertNull(ret3);
        assertEquals(ret4, " abc");
    }

    @Test
    public void testTrim1() {
        // 除去字符串头尾部的空白符，如果字符串是null，依然返回null
        String str = null;
        String str2 = "";
        String str3 = "     ";
        String str4 = " abc ";
        // 如果原字串为null，则返回null
        String ret = StringKit.trim(str);
        String ret2 = StringKit.trim(str2);
        String ret3 = StringKit.trim(str3);
        String ret4 = StringKit.trim(str4);
        assertNull(ret);
        assertEquals(ret2, "");
        assertEquals(ret3, "");
        assertEquals(ret4, "abc");
    }

    @Test
    public void testStartWith() {
        // 是否以指定字符串开头,如果给定的字符串和开头字符串都为null则返回true，
        // 否则任意一个值为null返回false
        String str = "apple";
        String startStr = "a";
        String str2 = null;
        String startStr2 = null;
        // 字符串是否以给定字符开始
        assertTrue(StringKit.startWith(str, startStr));
        // 如果给定的字符串和开头字符串都为null则返回true
        assertTrue(StringKit.startWith(str2, startStr2));
        // 任意一个值为null返回false
        assertFalse(StringKit.startWith(str, startStr2));
        assertFalse(StringKit.startWith(str2, startStr));
        // 是否以指定字符串开头，忽略大小写
        assertTrue(StringKit.startWith(str, "A", true));
        //是否以指定字符串开头，忽略大小写
        assertTrue(StringKit.startWith(str, "ApP", true));

    }


    @Test
    public void startWithIgnoreCase() {
        // 是否以指定字符串开头，忽略大小写
        String str = "apple";
        String startStr = "A";
        assertTrue(StringKit.startWithIgnoreCase(str, startStr));
    }

    @Test
    public void startWithAny() {
        //  给定字符串是否以任何一个字符串开始
        //  给定字符串和数组为空都返回false
        String str = "apple";
        String[] startStrs = new String[]{"b", "c", "d", "e", "A", "a"};
        String[] startStrs1 = new String[]{"b", "c", "d", "e", "A", "p"};
        String str2 = null;
        String[] startStrs2 = null;
        String str3 = "";
        String[] startStrs3 = {};
        assertTrue(StringKit.startWithAny(str, startStrs));
        assertFalse(StringKit.startWithAny(str2, startStrs2));
        assertFalse(StringKit.startWithAny(str, startStrs1));
        assertFalse(StringKit.startWithAny(str3, startStrs3));
        assertFalse(StringKit.startWithAny(str2, startStrs3));
        assertFalse(StringKit.startWithAny(str3, startStrs2));
    }


    @Test
    public void testEndWith() {
        // 是否以指定字符串结尾
        String str = "apple";
        String endStr = "e";
        String str2 = null;
        String endStr2 = null;
        // 字符串是否以给定字符结尾
        assertTrue(StringKit.endWith(str, endStr));
        // 如果给定的字符串和开头字符串都为null则返回true
        assertTrue(StringKit.endWith(str2, endStr2));
        // 否则任意一个值为null返回false
        assertFalse(StringKit.endWith(str, endStr2));
        assertFalse(StringKit.endWith(str2, endStr));
        // 是否以指定字符串结尾
        assertFalse(StringKit.endWith(str, "E"));

    }


    @Test
    public void endWithIgnoreCase() {
        // 是否以指定字符串结尾，忽略大小写
        String str = "apple";
        String endStr = "E";
        String endStr1 = "a";
        assertTrue(StringKit.endWithIgnoreCase(str, endStr));
        assertFalse(StringKit.endWithIgnoreCase(str, endStr1));
    }

    @Test
    public void endWithAny() {
        // 给定字符串是否以任何一个字符串结尾
        // 给定字符串和数组为空都返回false
        String str = "apple";
        String[] endStrs = new String[]{"b", "c", "d", "e", "A", "a"};
        String str2 = null;
        String[] endStrs2 = null;
        String[] endStrs3 = new String[]{"b", "c", "d", "A", "a"};
        assertTrue(StringKit.endWithAny(str, endStrs));
        assertFalse(StringKit.endWithAny(str2, endStrs2));
        assertFalse(StringKit.endWithAny(str, endStrs3));

    }

    @Test
    public void contains() {
        // 指定字符是否在字符串中出现过
        String str = "apple";
        char containStr = 'p';
        char containStr1 = 'b';
        assertTrue(StringKit.contains(str, containStr));
        assertFalse(StringKit.contains(str, containStr1));
    }

    @Test
    public void containsAny() {
        // 查找指定字符串是否包含指定字符串列表中的任意一个字符串
        String str = "apple";
        String[] containStrs = new String[]{"le", "df", "qweewq"};
        assertTrue(StringKit.containsAny(str, containStrs));
    }

    @Test
    public void testContainsAny() {
        String str = "apple";
        char[] containStrs = new char[]{'b', 'c', 'd', 'e', 'A', 'a'};
        char[] containStrs1 = new char[]{'b', 'c', 'd', 'A', 'p'};
        char[] containStrs2 = new char[]{'b', 'c', 'd', 'A', 'd'};
        assertTrue(StringKit.containsAny(str, containStrs));
        assertTrue(StringKit.containsAny(str, containStrs1));
        assertFalse(StringKit.containsAny(str, containStrs2));
    }

    @Test
    public void containsBlank() {
        // 给定字符串是否包含空白符（空白符包括空格、制表符、全角空格和不间断空格
        // 如果给定字符串为null或者""，则返回false
        // 空格
        String str = " a ";
        // 全角
        String str2 = "　";
        // 制表符
        String str3 = "\t";
        String str4 = "";
        String str5 = null;
        assertTrue(StringKit.containsBlank(str));
        assertTrue(StringKit.containsBlank(str2));
        assertTrue(StringKit.containsBlank(str3));
        assertFalse(StringKit.containsBlank(str4));
        assertFalse(StringKit.containsBlank(str5));

    }

    @Test
    public void getContainsStr() {
        // 查找指定字符串是否包含指定字符串列表中的任意一个字符串，如果包含返回找到的第一个字符串
        //如果不包含返回null
        String str = "apple";
        String[] containStrs = new String[]{"b", "c", "d", "e", "A", "a"};
        String[] containStrs1 = new String[]{"b", "c", "d", "le", "A", "a"};
        String[] containStrs2 = new String[]{"b", "c", "d", "A"};
        String ret = StringKit.getContainsStr(str, containStrs);
        String ret1 = StringKit.getContainsStr(str, containStrs1);
        String ret2 = StringKit.getContainsStr(str, containStrs2);
        assertEquals(ret, "e");
        assertEquals(ret1, "le");
        assertNull(ret2);
    }

    @Test
    public void containsIgnoreCase() {
        // 是否包含特定字符，忽略大小写，如果给定两个参数都为null，返回true
        String str = "apple";
        String containStr = "P";
        String containStr1 = "PpLe";
        String str2 = null;
        String containStr2 = null;
        assertTrue(StringKit.containsIgnoreCase(str, containStr));
        assertTrue(StringKit.containsIgnoreCase(str, containStr1));
        assertTrue(StringKit.containsIgnoreCase(str2, containStr2));
    }

    @Test
    public void containsAnyIgnoreCase() {
        // 查找指定字符串是否包含指定字符串列表中的任意一个字符串
        // 忽略大小写
        String str = "apple";
        String[] containStrs = new String[]{"b", "d", "A"};
        String[] containStrs1 = new String[]{"b", "c", "PP"};
        String[] containStrs2 = new String[]{"b", "c", "AE", "d"};
        assertTrue(StringKit.containsAnyIgnoreCase(str, containStrs));
        assertTrue(StringKit.containsAnyIgnoreCase(str, containStrs1));
        assertFalse(StringKit.containsAnyIgnoreCase(str, containStrs2));
    }

    @Test
    public void getContainsStrIgnoreCase() {
        // 查找指定字符串是否包含指定字符串列表中的任意一个字符串，如果包含返回找到的第一个字符串
        //没有则返回null，如果字符串和字符串列表有一个是null也返回null
        // 忽略大小写
        String str = "apple";
        String[] containStrs = new String[]{"b", "c", "Le", "e", "A", "a"};
        String ret = StringKit.getContainsStrIgnoreCase(str, containStrs);
        assertEquals(ret, "Le");

        String str2 = null;
        String[] containsStrs2 = null;
        String ret2 = StringKit.getContainsStrIgnoreCase(str2, containsStrs2);
        assertNull(ret2);
    }

    @Test
    public void getGeneralField() {
        // 如果是set或get方法名，返回field， 否则null
        String str = "getContainsStr";
        String str1 = "setContainsStr";
        String str2 = "ContainsStr";
        String ret = StringKit.getGeneralField(str);
        String ret1 = StringKit.getGeneralField(str1);
        String ret2 = StringKit.getGeneralField(str2);
        assertEquals(ret, "containsStr");
        assertEquals(ret1, "containsStr");
        assertEquals(ret2, null);
    }

    @Test
    public void genSetter() {
        // 生成set方法名
        String str = "name";
        String ret = StringKit.genSetter(str);
        assertEquals(ret, "setName");
    }

    @Test
    public void genGetter() {
        // 生成get方法名
        String str = "name";
        String ret = StringKit.genGetter(str);
        assertEquals(ret, "getName");
    }


    @Test
    public void testRemoveAll() {
        // 移除字符串中所有给定字符串
        String str = "2020-09-22";
        String ret = StringKit.removeAll(str, "-");
        assertEquals(ret, "20200922");

        // 去除字符串中指定的多个字符，如有多个则全部去除
        String str1 = "aaabccc";
        char[] strings = new char[]{'a', 'c'};
        String ret1 = StringKit.removeAll(str1, strings);
        assertEquals(ret1, "b");

        // 当字符串或数字给空时
        String str2 = null;
        String str3 = "";
        char[] strings2 = null;
        String ret2 = StringKit.removeAll(str2, "-");
        String ret3 = StringKit.removeAll(str3, "-");
        String ret4 = StringKit.removeAll(str1, strings2);
        assertNull(ret2);
        assertEquals(ret3, "");
        assertEquals(ret4, "aaabccc");


    }

    @Test
    public void removeAllLineBreaks() {
        // 去除所有换行符
        String str = "a\rb\nc";
        String ret = StringKit.removeAllLineBreaks(str);
        System.out.println(ret);
        assertEquals(ret, "abc");
    }

    @Test
    public void testRemovePreAndLowerFirst() {
        // 去掉首部指定长度的字符串并将剩余字符串首字母小写
        String str = "dataNew";
        String ret = StringKit.removePreAndLowerFirst(str, 3);
        assertEquals(ret, "aNew");

        // 去掉首部指定的字符串前缀并将剩余字符串首字母小写
        String ret1 = StringKit.removePreAndLowerFirst(str, "data");
        assertEquals(ret1, "new");
    }

    @Test
    public void isUpperCase() {
        // 给定字符串中的字母是否全部为大写
        //大写字母包括A-Z
        //其它非字母的Unicode符都算作大写
        String str = "APPLE";
        String str2 = "aPPle";
        String str3 = "3";
        String str4 = null;
        assertTrue(StringKit.isUpperCase(str));
        assertFalse(StringKit.isUpperCase(str2));
        assertTrue(StringKit.isUpperCase(str3));
        assertFalse(StringKit.isUpperCase(str4));

    }

    @Test
    public void isLowerCase() {
        // 给定字符串中的字母是否全部为小写
        //小写字母包括a-z
        //其它非字母的Unicode符都算作小写
        String str = "apple";
        String str2 = "aPPle";
        String str3 = "3";
        String str4 = null;
        assertTrue(StringKit.isLowerCase(str));
        assertFalse(StringKit.isLowerCase(str2));
        assertTrue(StringKit.isLowerCase(str3));
        assertFalse(StringKit.isLowerCase(str4));

    }

    @Test
    public void upperFirstAndAddPre() {
        // 原字符串首字母大写并在其首部添加指定字符串
        String str = "new";
        String ret = StringKit.upperFirstAndAddPre(str, "data");
        assertEquals(ret, "dataNew");
    }

    @Test
    public void upperFirst() {
        // 大写首字母
        String str = "dataNew";
        String ret = StringKit.upperFirst(str);
        assertEquals(ret, "DataNew");
    }

    @Test
    public void lowerFirst() {
        // 小写首字母
        String str = "DataNew";
        String ret = StringKit.lowerFirst(str);
        System.out.println(ret);
        assertEquals(ret, "dataNew");
    }

    @Test
    public void removePrefix() {
        // 去掉指定前缀，若前缀不是preffix，返回原字符串
        String str = "bigDataNew";
        String ret = StringKit.removePrefix(str, "big");
        String ret1 = StringKit.removePrefix(str, "Big");
        assertEquals(ret, "DataNew");
        assertEquals(ret1, "bigDataNew");
    }

    @Test
    public void removePrefixIgnoreCase() {
        // 忽略大小写去掉指定前缀，若前缀不是preffix，返回原字符串
        String str = "BIGDataNew";
        String ret = StringKit.removePrefixIgnoreCase(str, "big");
        String ret1 = StringKit.removePrefixIgnoreCase(str, "bIg");
        assertEquals(ret, "DataNew");
        assertEquals(ret1, "DataNew");
    }

    @Test
    public void removeSuffix() {
        // 去掉指定后缀，若后缀不是suffix，返回原字符串
        String str = "DataNewCompany";
        String ret = StringKit.removeSuffix(str, "Company");
        String ret1 = StringKit.removeSuffix(str, "company");
        assertEquals(ret, "DataNew");
        assertEquals(ret1, "DataNewCompany");
    }

    @Test
    public void removeSufAndLowerFirst() {
        // 去掉指定后缀，并小写首字母，若后缀不是 suffix， 返回原字符串
        String str = "DataNewCompany";
        String ret = StringKit.removeSufAndLowerFirst(str, "Company");
        String ret1 = StringKit.removeSufAndLowerFirst(str, "company");
        assertEquals(ret, "dataNew");
        assertEquals(ret1, "dataNewCompany");
    }

    @Test
    public void removeSuffixIgnoreCase() {
        // 忽略大小写去掉指定后缀
        String str = "DataNewCompany";
        String ret = StringKit.removeSuffixIgnoreCase(str, "company");
        String ret1 = StringKit.removeSuffixIgnoreCase(str, "anewcompany");
        assertEquals(ret, "DataNew");
        assertEquals(ret1, "Dat");
    }

    @Test
    public void testStrip() {
        // 去除两边的指定字符串
        String str = "BigDataNewCompany";
        String ret = StringKit.strip(str, "Big", "Company");
        assertEquals(ret, "DataNew");

        String ret2 = StringKit.strip(str, str);
        assertEquals(ret2, StringKit.EMPTY);

        String str2 = "hahabilibilihaha";
        String ret3 = StringKit.strip(str2, "haha");
        assertEquals(ret3, "bilibili");

        String str3 = "1hahabilibilihaha";
        String ret4 = StringKit.strip(str3, "1haha");
        assertEquals(ret4, "bilibilihaha");
    }

    @Test
    public void testStripIgnoreCase() {
        // 去除两边的指定字符串，忽略大小写
        String str = "BigDataNewCompany";
        String ret = StringKit.stripIgnoreCase(str, "big", "company");
        System.out.println(ret);
        assertEquals(ret, "DataNew");

        String str2 = "hahabilibilihaha";
        String ret3 = StringKit.stripIgnoreCase(str2, "HAHA");
        System.out.println(ret3);
        assertEquals(ret3, "bilibili");

    }

    @Test
    public void addPrefixIfNot() {
        // 如果给定字符串不是以prefix开头的，在开头补充 prefix
        //如果给定字符串或者prefix为空，返回对象的toString方法，null会返回null
        String str = "NewCompany";
        String str2 = null;
        String prefix = "Data";
        String prefix2 = "";
        String ret = StringKit.addPrefixIfNot(str, prefix);
        String ret2 = StringKit.addPrefixIfNot(str2, prefix);
        String ret3 = StringKit.addPrefixIfNot(str, prefix2);
        assertEquals(ret, "DataNewCompany");
        assertNull(ret2);
        assertEquals(ret3, str);

    }

    @Test
    public void addSuffixIfNot() {
        // 如果给定字符串不是以suffix结尾的，在尾部补充 suffix
        //如果给定字符串或者suffix为空，返回对象的toString方法，null会返回null
        String str = "DataNew";
        String str2 = null;
        String suffix = "Company";
        String suffix2 = "";
        String ret = StringKit.addSuffixIfNot(str, suffix);
        String ret2 = StringKit.addSuffixIfNot(str2, suffix);
        String ret3 = StringKit.addSuffixIfNot(str, suffix2);
        assertEquals(ret, "DataNewCompany");
        assertNull(ret2);
        assertEquals(ret3, str);
    }

    @Test
    public void cleanBlank() {
        // 清理空白字符，如果字符串为null则返回null
        String str = null;
        String str2 = " Da ta Ne w ";
        String ret = StringKit.cleanBlank(str);
        String ret2 = StringKit.cleanBlank(str2);
        assertNull(ret);
        assertEquals(ret2, "DataNew");
    }

    @Test
    public void cleanCenter() {
        // 清理字符串中间的空格,两边忽略，如果字符串是null，依然返回null。
        String str = null;
        String str2 = " Data New ";
        String str3 = " D a t a N e w ";
        String ret = StringKit.cleanCenter(str);
        String ret2 = StringKit.cleanCenter(str2);
        String ret3 = StringKit.cleanCenter(str3);
        assertNull(ret);
        assertEquals(ret2, " DataNew ");
        assertEquals(ret3, " DataNew ");
    }


    @Test
    public void testSplitToLong() {
        // 切分字符串为long数组
        String str = "1L,2L,3L,4L,5L";
        long[] strings = StringKit.splitToLong(str, ',');
        long[] strings1 = new long[]{1L, 2L, 3L, 4L, 5L};
        assertArrayEquals(strings, strings1);

        String str2 = "1L-2L-3L-4L-5L";
        long[] strings2 = StringKit.splitToLong(str2, "-");
        long[] strings3 = new long[]{1L, 2L, 3L, 4L, 5L};
        assertArrayEquals(strings2, strings3);


    }


    @Test
    public void testSplitToInt() {
        // 切分字符串为int数组
        String str = "1,2,3,4,5";
        int[] strings = StringKit.splitToInt(str, ',');
        int[] strings1 = new int[]{1, 2, 3, 4, 5};
        assertArrayEquals(strings, strings1);

        String str2 = "1#2#3#4#5";
        int[] strings2 = StringKit.splitToInt(str2, '#');
        int[] strings3 = new int[]{1, 2, 3, 4, 5};
        assertArrayEquals(strings2, strings3);


    }

    @Test
    public void testSplitToArray() {
        // 切分字符串
        String str = "a,b,c,d,e,f";
        String[] strings = StringKit.splitToArray(str, ',');
        String[] strings1 = new String[]{"a", "b", "c", "d", "e", "f"};
        assertArrayEquals(strings, strings1);

        String str2 = "a,b,c,d,e,f";
        String[] strings2 = StringKit.splitToArray(str2, ',', 3);
        String[] strings3 = new String[]{"a", "b", "c,d,e,f"};
        assertArrayEquals(strings2, strings3);

        String str3 = "a,b,c,d,e,f";
        String[] strings4 = StringKit.splitToArray(str3, ',', 4);
        String[] strings5 = new String[]{"a", "b", "c", "d,e,f"};
        assertArrayEquals(strings4, strings5);


    }

    @Test
    public void testSplit() {
        // 切分字符串
        String str = "a#b#c";
        List<String> ret;
        ret = StringKit.split(str, '#');
        List<String> trueList = Arrays.asList("a", "b", "c");
        assertEquals(ret, trueList);

        String str2 = " a # b # c ";
        List<String> ret2;
        ret2 = StringKit.split(str2, '#', 2, false, false);
        List<String> trueList2 = Arrays.asList(" a ", " b # c ");
        assertEquals(ret2, trueList2);

        String str3 = " a ## b # c ";
        List<String> ret3;
        ret3 = StringKit.split(str3, '#', -1, true, true);
        List<String> trueList3 = Arrays.asList("a", "b", "c");
        assertEquals(ret3, trueList3);

        String str4 = null;
        List<String> ret4;
        ret4 = StringKit.split(str4, '#');
        ArrayList<String> trueList4 = new ArrayList<String>();
        assertEquals(ret4, trueList4);

        List<String> ret5;
        ret5 = StringKit.split(str3, null, -1, true, true);
        List<String> trueList5 = Arrays.asList("a", "##", "b", "#", "c");
        assertEquals(ret5, trueList5);

        List<String> ret6;
        ret6 = StringKit.split(str3, null, 0, false, false);
        List<String> trueList6 = Arrays.asList("a", "##", "b", "#", "c");
        assertEquals(ret6, trueList6);

        // 根据给定长度，将给定字符串截取为多个部分
        String str7 = "a#b#c";
        String[] ret7 = StringKit.split(str7, 2);
        String[] trueList7 = new String[]{"a#", "b#", "c"};
        assertArrayEquals(ret7, trueList7);

        List<String> strings = new ArrayList<String>();
        strings.add("a");
        strings.add("b");
        strings.add("c");
        strings.add("d");
        List<String> ret8 = StringKit.split(" a, b, c, d,", ',', true, true);
        assertEquals(ret8, strings);
    }

    @Test
    public void splitTrim() {
        // 切分字符串，去除切分后每个元素两边的空白符，去除空白项
        String str2 = " a # # b # c ";
        List<String> ret2;
        ret2 = StringKit.split(str2, '#', -1, true, true);
        List<String> trueList2 = Arrays.asList("a", "b", "c");
        assertEquals(ret2, trueList2);
    }


    @Test
    public void sub() {
        // fromIndex 开始的index（包括）  toIndex 结束的index（不包括）
        String str = "abcdefg";
        String ret = StringKit.sub(str, 2, 3);
        assertEquals(ret, "c");

        String str2 = null;
        String ret2 = StringKit.sub(str2, 2, 3);
        assertNull(ret2);

        String ret3 = StringKit.sub(str, -4, -2);
        assertEquals(ret3, "de");

        // 如果from或to为负数，则按照length从后向前数位置，如果绝对值大于字符串长度，则from归到0，to归到length
        String ret4 = StringKit.sub(str, -12, -10);
        assertEquals(ret4, "abcdefg");

        // 如果from和to位置一样，返回 ""
        String ret5 = StringKit.sub(str, 2, 2);
        assertEquals(ret5, "");

    }

    @Test
    public void subPreGbk() {
        // 截取部分字符串，这里一个汉字的长度认为是2,len 切割的位置,suffix 切割后加上后缀
        String str = "这是一串文字";
        String ret = StringKit.subPreGbk(str, 4, "??");
        String ret1 = StringKit.subPreGbk(str, 8, "葡萄");
        String ret2 = StringKit.subPreGbk(str, 16, "这是一串文字");
        assertEquals(ret, "这是??");
        assertEquals(ret1, "这是一串葡萄");
        assertEquals(ret2, "这是一串文字");

    }

    @Test
    public void maxLength() {
        // 限制字符串长度，如果超过指定长度，截取指定长度并在末尾加"..."
        //如果指定的长度大于或等于字符串长度，则返回字符串本身
        String str = "abcd";
        String ret = StringKit.maxLength(str, 3);
        String ret1 = StringKit.maxLength(str, 4);
        String ret2 = StringKit.maxLength(str, 5);
        assertEquals(ret, "abc...");
        assertEquals(ret1, "abcd");
        assertEquals(ret2, "abcd");

    }

    @Test
    public void subPre() {
        // 切割指定位置之前部分的字符串 toIndex 切割到的位置（不包括）
        //如果指定的长度大于或等于字符串长度，则返回字符串本身
        String str = "abcde";
        String ret = StringKit.subPre(str, 3);
        String ret1 = StringKit.subPre(str, 1);
        String ret2 = StringKit.subPre(str, 5);
        String ret3 = StringKit.subPre(str, 6);
        String ret4 = StringKit.subPre(str, -2);
        assertEquals(ret, "abc");
        assertEquals(ret1, "a");
        assertEquals(ret2, "abcde");
        assertEquals(ret3, "abcde");
        assertEquals(ret4, "abc");
    }

    @Test
    public void subSuf() {
        // 切割指定位置之后部分的字符串() 切割开始的位置（包括）
        String str = "abcde";
        String ret = StringKit.subSuf(str, 3);
        String ret1 = StringKit.subSuf(str, 5);
        String ret2 = StringKit.subSuf(str, 6);
        String ret3 = StringKit.subSuf(str, -4);
        assertEquals(ret, "de");
        assertEquals(ret1, "");
        assertEquals(ret2, "");
        assertEquals(ret3, "bcde");
    }

    @Test
    public void subSufByLength() {
        // 切割指定长度的后部分的字符串 length 切割到的位置(包括),如果切割长度<=0，则返回""
        String str = "abcdefg";
        String ret = StringKit.subSufByLength(str, -3);
        assertEquals(ret, StringKit.EMPTY);
        String ret2 = StringKit.subSufByLength(str, 3);
        assertEquals(ret2, "efg");


    }

    @Test
    public void subWithLength() {
        // 截取字符串,从指定位置开始,截取指定长度的字符串
        String str = "abcdefg";
        String ret = StringKit.subWithLength(str, -4, 2);
        String ret1 = StringKit.subWithLength(str, 4, 2);
        assertEquals(ret, "de");
        assertEquals(ret1, "ef");


    }

    @Test
    public void testSubBefore() {
        // 	 截取分隔字符串之前的字符串，不包括分隔字符串
        // 	 separator 分隔字符串（不包括） isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
        String str = "abcdefg";
        String ret = StringKit.subBefore(str, "c", false);
        assertEquals(ret, "ab");

        //	 如果分隔字符串为空串""，则返回原字符串
        String ret2 = StringKit.subBefore(str, null, false);
        assertEquals(ret2, str);

        //  如果给定的字符串为空串（null或""）或者分隔字符串为null，返回原字符串
        String str2 = "";
        String ret3 = StringKit.subBefore(str2, "c", false);
        assertEquals(ret3, "");

        // 如果分隔字符串未找到，返回原字符串
        String str3 = "abcdefg";
        String ret4 = StringKit.subBefore(str3, "h", false);
        assertEquals(ret4, "abcdefg");


        String str4 = "abcdefgeracdfere";
        String ret5 = StringKit.subBefore(str4, "a", true);
        assertEquals(ret5, "abcdefger");

        String str5 = "a,b.c'd";
        String ret6 = StringKit.subBefore(str5, '.', false);
        assertEquals(ret6, "a,b");
    }


    @Test
    public void testSubAfter() {
        //  截取分隔字符串之后的字符串
        //separator 分隔字符串（不包括） isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
        String str = "abcdefg";
        String ret = StringKit.subAfter(str, "c", false);
        assertEquals(ret, "defg");

        //	 如果分隔字符串为空串""，则返回空串
        String ret2 = StringKit.subAfter(str, null, false);
        assertEquals(ret2, "");

        //  如果给定的字符串为空串（null或""）或者分隔字符串为null，返回空串
        String str2 = "";
        String ret3 = StringKit.subAfter(str2, "c", false);
        assertEquals(ret3, "");

        // 如果分隔字符串未找到，返回原字符串
        String str3 = "abcdefg";
        String ret4 = StringKit.subAfter(str3, "h", false);
        assertEquals(ret4, "");


        String str4 = "abcdefgeracdfere";
        String ret5 = StringKit.subAfter(str4, "a", true);
        assertEquals(ret5, "cdfere");

        String str5 = "a,b.c'd";
        String ret6 = StringKit.subAfter(str5, '.', false);
        assertEquals(ret6, "c'd");


    }


    @Test
    public void testSubBetween() {
        // 截取指定字符串中间部分，不包括标识字符串
        //before 截取开始的字符串标识(相同字符串选取索引最靠近0的) after 截取到的字符串标识（相同字符串选取索引最靠近0的）
        String str = "ab[cdef]g";
        String ret = StringKit.subBetween(str, "[", "]");
        assertEquals(ret, "cdef");

        String str2 = "abcdabcd";
        String ret2 = StringKit.subBetween(str2, "a", "d");
        assertEquals(ret2, "bc");

        String ret3 = StringKit.subBetween(null, "", "");
        assertNull(ret3);

        String ret4 = StringKit.subBetween("", "", "");
        assertEquals(ret4, "");

        String str3 = "tagabcdfeftag";
        String ret5 = StringKit.subBetween(str3, "tag");
        assertEquals(ret5, "abcdfef");

        String str4 = "a1a2a3b5b6b7";
        String ret6 = StringKit.subBetween(str4, "a", "b");
        assertEquals(ret6, "1a2a3");

    }


    @Test
    public void testIsSurround() {
        // 给定字符串是否被字符包围
        String str = "[abcdefg]";
        assertTrue(StringKit.isSurround(str, "[", "]"));
        assertFalse(StringKit.isSurround(str, '[', 'g'));
        assertFalse(StringKit.isSurround("", "[", "]"));
        assertTrue(StringKit.isSurround(".abd!", '.', '!'));
    }

    @Test
    public void testRepeat() {
        // 重复某个字符 count 重复的数目，如果小于等于0则返回""
        String str = "abcd";
        String ret = StringKit.repeat(str, 2);
        assertEquals(ret, "abcdabcd");

        char str2 = '+';
        String ret2 = StringKit.repeat(str2, 3);
        assertEquals(ret2, "+++");
    }

    @Test
    public void repeatByLength() {
        // 重复某个字符串到指定长度
        String str = "abcefg";
        String ret = StringKit.repeatByLength(str, 4);
        String ret1 = StringKit.repeatByLength(str, 12);
        String ret2 = StringKit.repeatByLength(str, 14);
        assertEquals(ret, "abce");
        assertEquals(ret1, "abcefgabcefg");
        assertEquals(ret2, "abcefgabcefgab");

    }

    @Test
    public void repeatAndJoin() {
        // 重复某个字符串并通过分界符连接

        String ret = StringKit.repeatAndJoin("?", 5, ",");
        String ret2 = StringKit.repeatAndJoin("?", 5, null);
        String ret3 = StringKit.repeatAndJoin("?", 0, ",");
        String ret4 = StringKit.repeatAndJoin("XZ", 3, "/");
        assertEquals(ret, "?,?,?,?,?");
        assertEquals(ret2, "?????");
        assertEquals(ret3, "");
        assertEquals(ret4, "XZ/XZ/XZ");
    }

    @Test
    public void testEquals() {
        // 比较两个字符串（大小写敏感）
        assertTrue(StringKit.equals("haha", "haha"));
        assertFalse(StringKit.equals("haha", "haHa"));
        assertFalse(StringKit.equals(null, "haHa"));
    }

    @Test
    public void equalsIgnoreCase() {
        assertTrue(StringKit.equalsIgnoreCase("HAHA", "haha"));
        assertTrue(StringKit.equalsIgnoreCase("HAHA", "HAHA"));
    }

    @Test
    public void equalsAnyIgnoreCase() {
        String str = "abcdef";
        String[] strings = new String[]{"ac", "ABCDEF"};
        assertTrue(StringKit.equalsAnyIgnoreCase(str, strings));

    }

    @Test
    public void equalsAny() {
        //给定字符串是否与提供的字符串列表中任一字符串相同（忽略大小写），相同则返回true，没有相同的返回false
        //如果参与比对的字符串列表为空，返回false
        String str = "abcdef";
        String[] strings = new String[]{"ac", "ABCDEF"};
        assertTrue(StringKit.equalsAny(str, true, strings));

        String str2 = "abcdef";
        String[] strings2 = new String[]{"ac", "abcdef"};
        assertTrue(StringKit.equalsAny(str2, strings2));

        String str3 = "abcdef";
        String[] strings3 = new String[]{};
        assertFalse(StringKit.equalsAny(str3, strings3));

    }

    @Test
    public void format() {
        //格式化文本, {} 表示占位符
        //此方法只是简单将占位符 {} 按照顺序替换为参数

        //测试通常使用
        String template = "this is {} for {}";
        String str1 = "a";
        String str2 = "b";
        assertEquals(StringKit.format(template, str1, str2), "this is a for b");

        //测试转义{}
        String template1 = "this is \\{} for {}";
        assertEquals(StringKit.format(template1, str1, str2), "this is {} for a");

        //测试转义\
        String template2 = "this is \\\\{} for {}";
        assertEquals(StringKit.format(template2, str1, str2), "this is \\a for b");
    }

    @Test
    public void indexedFormat() {
        // 有序的格式化文本，使用{number}做为占位符
        String temp = "{0}+{1}";
        assertEquals(StringKit.indexedFormat(temp, "name", "dataNew"), "name+dataNew");

        String temp1 = "{0}{1}{4}{3}{2}";
        assertEquals(StringKit.indexedFormat(temp1, "A", "p", "e", "l", "p"), "Apple");
    }

    @Test
    public void testFormat() {
        // 格式化文本，使用 {varName} 占位
        String temp = "{name}+{age}";
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", "dataNew");
        map.put("age", "12");
        String ret = StringKit.format(temp, map);
        assertEquals(ret, "dataNew+12");

        String temp1 = "水果：{furit} 蔬菜：{vegetable}";
        HashMap<String, String> map1 = new HashMap<String, String>();
        map1.put("furit", "apple");
        map1.put("vegetable", "potato");
        String ret1 = StringKit.format(temp1, map1);
        assertEquals(ret1, "水果：apple 蔬菜：potato");

    }

    @Test
    public void utf8Bytes() {
        // 编码字符串，编码为UTF-8
        byte[] ret = StringKit.utf8Bytes("hello");
        System.out.println(Arrays.toString(ret));

    }

    @Test
    public void testBytes() {

        byte[] ret = StringKit.bytes("hello", CharsetKit.CHARSET_UTF_8);
        System.out.println(Arrays.toString(ret));

        byte[] ret2 = StringKit.bytes("hello");
        System.out.println(Arrays.toString(ret2));

    }

    @Test
    public void utf8Str() {
        //  将对象转为字符串
        //	1、Byte数组和ByteBuffer会被转换为对应字符串的数组 2、对象数组会调用Arrays.toString方法
        byte[] bytes = new byte[]{104, 101, 108, 108, 111};
        String ret = StringKit.utf8Str(bytes);
        assertEquals(ret, "hello");
    }


    @Test
    public void testStr() {

        byte[] bytes = new byte[]{104, 101, 108, 108, 111};
        String ret = StringKit.str(bytes, CharsetKit.CHARSET_UTF_8);
        assertEquals(ret, "hello");
    }


    @Test
    public void testToString() {
        // 调用对象的toString方法，null会返回“null”
        Collection<String> collection = new ArrayList<String>();
        collection.add("a");
        collection.add("b");
        collection.add("c");
        assertEquals(StringKit.toString(collection, ","), "a,b,c");
        assertEquals(StringKit.toString(collection, ",", "start", "end"), "startaend,startbend,startcend");
        assertEquals(StringKit.toString(collection), "a,b,c");

    }

    @Test
    public void byteBuffer() {
        // 字符串转换为byteBuffer
        String str = "hello";
        ByteBuffer ret = StringKit.byteBuffer(str, CharsetKit.GBK);
        System.out.println(ret);
        ByteBuffer ret1 = StringKit.byteBuffer(str, CharsetKit.UTF_8);
        System.out.println(ret1);

    }

    @Test
    public void testJoin() {
        //用分隔符将多个对象转换为字符串
        Number[] numbers = new Number[]{1, 2};

        String ret = StringKit.join(numbers, ",");
        String trueLi = "1,2";
        assertEquals(ret, trueLi);

        String[] strings = {"2", "3", "5", "4", "1"};
        String ret1 = StringKit.join(strings, ",");
        String trueLi1 = "2,3,5,4,1";
        assertEquals(ret1, trueLi1);

        Object[] objs = {1, 20L, new Double(2.5)};
        String ret2 = StringKit.join(objs, ",");
        String trueLi2 = "1,20,2.5";
        assertEquals(ret2, trueLi2);
    }

    @Test
    public void toUnderlineCase() {
        // 将驼峰式命名的字符串转换为下划线方式。如果转换前的驼峰式命名的字符串为空，则返回空字符串
        String str = "GetPhoneNumber";
        String ret = StringKit.toUnderlineCase(str);
        assertEquals(ret, "get_phone_number");
        String ret2 = StringKit.toUnderlineCase(null);
        assertNull(ret2);


    }

    @Test
    public void toSymbolCase() {
        // 将驼峰式命名的字符串转换为使用符号连接方式。如果转换前的驼峰式命名的字符串为空，则返回空字符串
        String str = "GetPhoneNumber";
        String ret = StringKit.toSymbolCase(str, '+');
        assertEquals(ret, "get+phone+number");
        String ret2 = StringKit.toSymbolCase(null, '+');
        assertNull(ret2);
    }

    @Test
    public void toCamelCase() {
        // 将下划线方式命名的字符串转换为驼峰式。如果转换前的下划线大写方式命名的字符串为空，则返回空字符串
        String str = "get_phone_number";
        String ret = StringKit.toCamelCase(str);
        assertEquals(ret, "getPhoneNumber");
        String ret2 = StringKit.toCamelCase(null);
        assertNull(ret2);
    }

    @Test
    public void testWrap() {
        //包装指定字符串
        String str = "dataNew";
        String ret = StringKit.wrap(str, "Nice");
        String ret2 = StringKit.wrap(str, "Big", "Company");
        String ret3 = StringKit.wrap(str, null);
        assertEquals(ret, "NicedataNewNice");
        assertEquals(ret2, "BigdataNewCompany");
        assertEquals(ret3, "dataNew");
    }

    @Test
    public void testWrapAll() {
        //包装多个字符串
        String[] strings = new String[]{"day", "man"};
        String[] ret = StringKit.wrapAll("Nice", strings);
        String[] trueLi = new String[]{"NicedayNice", "NicemanNice"};
        assertArrayEquals(ret, trueLi);
        String[] ret1 = StringKit.wrapAll("What a nice ", "!", strings);
        String[] trueLi1 = new String[]{"What a nice day!", "What a nice man!"};
        assertArrayEquals(ret1, trueLi1);
    }

    @Test
    public void wrapIfMissing() {
        // 包装指定字符串，如果前缀或后缀已经包含对应的字符串，则不再包装
        String string = "BigdataNew";
        String ret = StringKit.wrapIfMissing(string, "Big", "data");
        assertEquals(ret, "BigdataNewdata");

        String string1 = "AAAAAAAABAAAAAAAAAA";
        String ret1 = StringKit.wrapIfMissing(string1, "A", "B");
        assertEquals(ret1, "AAAAAAAABAAAAAAAAAAB");
    }

    @Test
    public void testWrapAllIfMissing() {
        // 包装多个字符串，如果已经包装，则不再包装
        String[] strings = new String[]{"day", "man"};
        String[] ret = StringKit.wrapAllIfMissing("Nice", strings);
        String[] trueLi = new String[]{"NicedayNice", "NicemanNice"};
        assertArrayEquals(ret, trueLi);

        String[] strings1 = new String[]{"Niceday", "Niceman"};
        String[] ret2 = StringKit.wrapAllIfMissing("Nice", "!", strings1);
        String[] trueLi2 = new String[]{"Niceday!", "Niceman!"};
        assertArrayEquals(ret2, trueLi2);
    }

    @Test
    public void testUnWrap() {
        // 去掉字符包装，如果未被包装则返回原字符串
        String string = "BigdataNewdata";
        String ret = StringKit.unWrap(string, "Big", "data");
        assertEquals(ret, "dataNew");

        String str = "-dataNew-";
        String ret2 = StringKit.unWrap(str, '-');
        assertEquals(ret2, "dataNew");

        String str2 = "dataNew";
        String ret3 = StringKit.unWrap(str2, '-');
        assertEquals(ret3, "dataNew");

        String str3 = "dataNew";
        String ret4 = StringKit.unWrap(str3, "Big", "data");
        assertEquals(ret4, "dataNew");
    }

    @Test
    public void isWrap() {

        String str = "BigDataNewBig";
        // 指定字符串是否被同一字符包装（前后都有这些字符串）
        assertTrue(StringKit.isWrap(str, "Big"));
        // 指定字符串是否被包装
        assertTrue(StringKit.isWrap("BigDataNewData", "Big", "Data"));
        assertFalse(StringKit.isWrap("BigDataNewDatA", "Big", "Data"));
        // 指定字符串是否被同一字符包装（前后都有这些字符串）
        assertTrue(StringKit.isWrap("-DataNew-", '-'));
        assertTrue(StringKit.isWrap("-DataNew+", '-', '+'));
        assertFalse(StringKit.isWrap("+DataNew-", '-', '+'));
    }

    @Test
    public void testPadPre() {
        // 补充字符串以满足最小长度 str 字符串  minLength 最小长度  padStr 补充的字符
        //填充字符在前面
        assertEquals(StringKit.padPre("ab", 3, "A"), "Aab");
        assertEquals(StringKit.padPre("1", 3, '0'), "001");
        assertEquals(StringKit.padPre("喝水", 3, "一二三四五六七八九"), "一喝水");
        assertEquals(StringKit.padPre("喝水", 8, "一二"), "一二一二一二喝水");

    }

    @Test
    public void testPadAfter() {
        // 补充字符串以满足最小长度
        //填充字符在后面
        assertEquals(StringKit.padAfter("ab", 3, "A"), "abA");
        assertEquals(StringKit.padAfter("1", 3, '0'), "100");
        assertEquals(StringKit.padAfter("喝水", 3, "一二三四五六七八九"), "喝水一");
        assertEquals(StringKit.padAfter("喝水", 8, "一二"), "喝水一二一二一二");
    }

    @Test
    public void testCenter() {
        // 居中字符串，两边补充指定字符串，如果指定长度小于字符串，则返回原字符串
        //padChar 两边补充的字符 默认为“”
        assertEquals(StringKit.center("", 4), "    ");
        assertEquals(StringKit.center("ab", -1), "ab");
        assertEquals(StringKit.center("ab", 4), " ab ");
        assertEquals(StringKit.center("abcd", 2), "abcd");
        assertEquals(StringKit.center("a", 4), " a  ");

        assertEquals(StringKit.center("a", 4, 'y'), "yayy");
        assertEquals(StringKit.center("a", 5, 'y'), "yyayy");
        assertEquals(StringKit.center("a", 6, 'y'), "yyayyy");
    }

    @Test
    public void testBuilder() {
        //  创建StringBuilder对象
        assertNotNull(StringKit.builder());
        assertNotNull(StringKit.strBuilder());
        //capacity 初始大小
        assertNotNull(StringKit.builder(2));
        //strs 初始字符串列表
        assertNotNull(StringKit.builder("day", "month", "year"));
    }

    @Test
    public void testStrBuilder() {
        // 创建StrBuilder对象
        assertNotNull(StringKit.strBuilder());
        assertNotNull(StringKit.strBuilder(2));
        assertNotNull(StringKit.strBuilder("day", "month", "year"));
    }


    @Test
    public void getReader() {
        // 获得StringReader
        assertNotNull(StringKit.getReader("dataNew"));
    }

    @Test
    public void getWriter() {
        //  获得StringWriter
        assertNotNull(StringKit.getWriter());
    }

    @Test
    public void testCount() {
        // 统计指定内容中包含指定字符串的数量
        //参数为 null或者"" 返回 0
        String str = "abcefga";
        assertEquals(StringKit.count(str, "a"), 2);
        assertEquals(StringKit.count("-abc+erfd+efda", '+'), 2);
        assertEquals(StringKit.count("AAAaaaaaBBaBBBbbbb", 'a'), 6);
        assertEquals(StringKit.count("-abc+erfd+efda", null), 0);

    }

    @Test
    public void cut() {
        // 将字符串切分为N等份 如果字符串的长度小于切分的长度或者切分长度<0，那么直接返回原字符串
        String str = "abcdefgh";
        assertArrayEquals(StringKit.cut(str, 1), new String[]{"a", "b", "c", "d", "e", "f", "g", "h"});
        assertArrayEquals(StringKit.cut(str, 3), new String[]{"abc", "def", "gh"});
        assertArrayEquals(StringKit.cut(str, 4), new String[]{"abcd", "efgh"});
        assertArrayEquals(StringKit.cut(str, 5), new String[]{"abcde", "fgh"});
        assertArrayEquals(StringKit.cut(str, 10), new String[]{"abcdefgh"});
        assertArrayEquals(StringKit.cut(str, -1), new String[]{"abcdefgh"});

    }

    @Test
    public void brief() {
        // 将给定字符串，变成 "xxx...xxx" 形式的字符串 maxLength 最大长度
        //如果字符串的长度+3小于等于maxLength或者maxLength<0，那么直接返回原字符串
        String str = "abcdefghi";
        String ret = StringKit.brief(str, 4);
        String ret1 = StringKit.brief(str, 9);
        assertEquals(ret, "ab...hi");
        assertEquals(ret1, "abcde...fghi");
    }

    @Test
    public void compare() {
        // 比较两个字符串，用于排序
        // 负数：str1 &lt;(<) str2，正数：str1 &gt;(>) str2, 0：str1 == str2
        //nullIsLess true表示null小于非空值 false则表示null大于非空值
        String str1 = "a";
        String str2 = null;

        assertEquals(StringKit.compare(str1, str2, false), -1);

        assertEquals((StringKit.compare(str1, str2, true)), 1);

        assertEquals((StringKit.compare(str2, str2, false)), 0);


    }

    @Test
    public void compareIgnoreCase() {
        // 比较两个字符串，用于排序，大小写不敏感
        assertTrue((StringKit.compareIgnoreCase("a", "A", true)) == 0);

        assertTrue((StringKit.compareIgnoreCase("abc", "ab", true)) > 0);

        assertTrue((StringKit.compareIgnoreCase(null, "A", true)) < 0);

    }

    @Test
    public void compareVersion() {
        // 比较两个版本
        // null版本排在最小
        // 负数：version1 &lt; version2，正数：version1 &gt; version2,0：version1 == version2
        assertTrue((StringKit.compareVersion("1.0.0", "1.0.2")) < 0);
        assertTrue((StringKit.compareVersion("1.0.0", null)) > 0);
        assertTrue((StringKit.compareVersion("1.0.0", "1.0.0")) == 0);
        assertTrue((StringKit.compareVersion("2.1.3", "2.3.1")) < 0);

    }

    @Test
    public void testIndexOf() {
        // 指定范围内查找指定字符 返回位置索引 没有查询到返回-1
        assertEquals(StringKit.indexOf("abcdef", 'e'), 4);
        assertEquals(StringKit.indexOf("abcdef", 'E'), -1);
        assertEquals(StringKit.indexOf("abecdef", 'e', 3), 5);
        assertEquals(StringKit.indexOf("abcedefe", 'e', 1, 5), 3);
        // 指定范围内反向查找字符串
        assertEquals(StringKit.indexOf("abcedebfe", "b", 5, true), 6);
    }

    @Test
    public void testIndexOfIgnoreCase() {
        // 指定范围内查找字符串，忽略大小写
        assertEquals(StringKit.indexOfIgnoreCase("aabaabaa", "B", 0), 2);
        assertEquals(StringKit.indexOfIgnoreCase("aabaabaa", "B", 3), 5);
        assertEquals(StringKit.indexOfIgnoreCase("", null, 0), -1);
        assertEquals(StringKit.indexOfIgnoreCase("abc", "", 9), -1);
        assertEquals(StringKit.indexOfIgnoreCase("abcb", "b"), 1);
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        // 指定范围内查找字符串，忽略大小写
        //  fromIndex 为搜索起始位置，从后往前计数
        String str = "abcabcabc";
        assertEquals(StringKit.lastIndexOfIgnoreCase(str, "b"), 7);
        assertEquals(StringKit.lastIndexOfIgnoreCase(str, "B", 5), 4);


    }

    @Test
    public void lastIndexOf() {
        //fromIndex 为搜索起始位置，从后往前计数
        String str = "abcabcabc";
        assertEquals(StringKit.lastIndexOf(str, "B", 5, false), StringKit.INDEX_NOT_FOUND);
    }

    @Test
    public void ordinalIndexOf() {
        // 返回字符串 searchStr 在字符串 str 中第 ordinal 次出现的位置。
        // 如果 str=null 或 searchStr=null 或 ordinal<=0 则返回-1
        assertEquals(StringKit.ordinalIndexOf("aabaabaa", "a", 2), 1);
        assertEquals(StringKit.ordinalIndexOf("aabaabaa", "a", 5), 6);
        assertEquals(StringKit.ordinalIndexOf(null, "a", 2), -1);
        assertEquals(StringKit.ordinalIndexOf("aabaabaa", null, 2), -1);
        assertEquals(StringKit.ordinalIndexOf("aabaabaa", "a", -1), -1);

    }

    @Test
    public void testAppendIfMissing() {
        //  如果给定字符串不是以给定的一个或多个字符串为结尾，则在尾部添加结尾字符串
        // 不忽略大小写
        String str = "abcabcdefg";
        assertEquals(StringKit.appendIfMissing(str, "hijk"), "abcabcdefghijk");
        assertEquals(StringKit.appendIfMissing("abcabcdefg", "efg"), "abcabcdefg");
        assertEquals(StringKit.appendIfMissing("abcabcdefg", "EFG", false), "abcabcdefgEFG");
        //忽略大小写
        assertEquals(StringKit.appendIfMissing("abcabcdefg", "EFG", true), "abcabcdefg");
        //suffix  需要添加到结尾的字符串  suffixes 需要额外检查的结尾字符串，如果以这些中的一个为结尾，则不再添加
        assertEquals(StringKit.appendIfMissing("abcabcdnba", "efg", "cba", "nba"), "abcabcdnba");
        assertEquals(StringKit.appendIfMissing("abcabcd", null, "cba", "nba"), "abcabcd");
    }

    @Test
    public void appendIfMissingIgnoreCase() {
        //  如果给定字符串不是以给定的一个或多个字符串为结尾，则在尾部添加结尾字符串
        //  忽略大小写
        String str = "abcabcd";
        assertEquals(StringKit.appendIfMissingIgnoreCase(str, "hijk", "cba", "nba"), "abcabcdhijk");
        assertEquals(StringKit.appendIfMissingIgnoreCase(str, "hijk", "BCD", "nba"), "abcabcd");
    }

    @Test
    public void testPrependIfMissing() {
        // 如果给定字符串不是以给定的一个或多个字符串为开头，则在首部添加起始字符串
        // 不忽略大小写
        String str = "abcabcdefg";
        assertEquals(StringKit.prependIfMissing(str, "hijk"), "hijkabcabcdefg");
        assertEquals(StringKit.prependIfMissing("abcabcdefg", "abc"), "abcabcdefg");
        assertEquals(StringKit.prependIfMissing("abcabcdefg", "ABC", false), "ABCabcabcdefg");
        assertEquals(StringKit.prependIfMissing("nbaabcabcdnba", "efg", "cba", "nba"), "nbaabcabcdnba");
        assertEquals(StringKit.prependIfMissing("abcabcd", null, "cba", "nba"), "abcabcd");
    }

    @Test
    public void prependIfMissingIgnoreCase() {
        // 如果给定字符串不是以给定的一个或多个字符串为开头，则在首部添加起始字符串
        // 忽略大小写
        String str = "ABCabcabcd";
        assertEquals(StringKit.prependIfMissingIgnoreCase(str, "abc", "cba", "nba"), "ABCabcabcd");
    }

    @Test
    public void reverse() {
        // 反转字符串
        assertEquals(StringKit.reverse("abcd"), "dcba");
        assertEquals(StringKit.reverse("123456"), "654321");
        assertEquals(StringKit.reverse(""), "");

    }

    @Test
    public void fillBefore() {
        // 将已有字符串填充为规定长度，如果已有字符串超过这个长度则返回这个字符串
        // 字符填充于字符串前
        assertEquals(StringKit.fillBefore("abcd", 'x', 6), "xxabcd");
        assertEquals(StringKit.fillBefore("abcd", 'x', 3), "abcd");
    }

    @Test
    public void fillAfter() {
        // 将已有字符串填充为规定长度，如果已有字符串超过这个长度则返回这个字符串<br>
        // 字符填充于字符串后
        assertEquals(StringKit.fillAfter("abcd", 'x', 6), "abcdxx");
        assertEquals(StringKit.fillAfter("abcd", 'x', 3), "abcd");

    }

    @Test
    public void fill() {
        // 将已有字符串填充为规定长度，如果已有字符串超过这个长度则返回这个字符串
        //str 被填充的字符串  filledChar 填充的字符  len 填充长度   isPre 是否填充在前
        assertEquals(StringKit.fill("abcd", 'x', 6, true), "xxabcd");
        assertEquals(StringKit.fill("abcd", 'x', 3, true), "abcd");
    }

    @Test
    public void isSubEquals() {
        // 截取两个字符串的不同部分（长度一致），判断截取的子串是否相同
        // 任意一个字符串为null返回false
        //str1 第一个字符串 start1 第一个字符串开始的位置 str2 第二个字符串 start2 第二个字符串开始的位置 length 截取长度 ignoreCase 是否忽略大小写
        String str = "abcdefg";
        String str2 = "efgabcd";
        assertTrue(StringKit.isSubEquals(str, 0, str2, 3, 4, false));
        assertTrue(StringKit.isSubEquals("ABCDefg", 0, "efgAbCd", 3, 4, true));
        assertFalse(StringKit.isSubEquals(null, 0, "efgAbCd", 3, 4, true));
    }

    @Test
    public void isAllCharMatch() {
        // 字符串的每一个字符是否都与定义的匹配器匹配

        //判断字符串是否全部为字母组成，包括大写和小写字母和汉字
        String str = "abcd";
        assertTrue(StringKit.isAllCharMatch(str, new Matcher<Character>() {
            @Override
            public boolean match(Character str) {
                return Character.isLetter(str);
            }
        }));

        // 判断字符串是否全部为大写字母
        String str1 = "ABCDEFG";
        assertTrue(StringKit.isAllCharMatch(str1, new Matcher<Character>() {
            @Override
            public boolean match(Character str1) {
                return Character.isUpperCase(str1);
            }
        }));

        // 判断字符串是否全部为数字
        String str2 = "1235465";
        assertTrue(StringKit.isAllCharMatch(str2, new Matcher<Character>() {
            @Override
            public boolean match(Character str2) {
                return NumberKit.isNumber(str2.toString());
            }
        }));


    }

    @Test
    public void replaceIgnoreCase() {
        // 替换字符串中的指定字符串，忽略大小写
        //str 字符串  searchStr 被查找的字符串  replacement 被替换的字符串
        String str = "abcd";
        assertEquals(StringKit.replaceIgnoreCase(str, "B", "c"), "accd");
        assertEquals(StringKit.replaceIgnoreCase(str, "E", "c"), "abcd");
        assertEquals(StringKit.replaceIgnoreCase("HAHBhChDHEhFHG", "H", ""), "ABCDEFG");
        assertEquals(StringKit.replaceIgnoreCase(str, "B", null), "acd");
    }

    @Test
    public void testReplace() {
        String str = "abcdbB";
        assertEquals(StringKit.replace(str, "B", "c"), "abcdbc");
        assertEquals(StringKit.replace("abcdb", "B", "c", false), "abcdb");
        assertEquals(StringKit.replace(str, 2, "b", "e", true), "abcdee");
        // 替换指定字符串的指定区间内字符为固定字符
        //startInclude 开始位置（包含） endExclude 结束位置（不包含） replacedChar 被替换的字符
        assertEquals(StringKit.replace("abcdbBabc", 2, 5, 'c'), "abcccBabc");

        // todo 替换所有正则匹配的文本，并使用自定义函数决定如何替换
        //str 要替换的字符串 regex 用于匹配的正则式  replaceFun 决定如何替换的函数
//        StringKit.replace("12ABcd56", "^-?\\d+$", new Function<java.util.regex.Matcher, String>() {
//            @Override
//            public String call(java.util.regex.Matcher parameters) {
//                return null;
//            }
//        });
    }


    @Test
    public void hide() {
        // 替换指定字符串的指定区间内字符为"*" startInclude  开始位置（包含） endExclude 结束位置（不包含）
        assertEquals(StringKit.hide("15050506060", 3, 7), "150****6060");
    }

    @Test
    public void testReplaceChars() {
        //  替换字符字符数组中所有的字符为replacedStr
        //  提供的chars为所有需要被替换的字符，例如："\r\n"，则"\r"和"\n"都会被替换，哪怕他们单独存在
        assertEquals(StringKit.replaceChars("a\nb\tc\rd", "\r\t\n", "+"), "a+b+c+d");
        char[] li = new char[]{'\t', '\r', '\n'};
        assertEquals(StringKit.replaceChars("a\nb\tc\rd", li, "+"), "a+b+c+d");
    }

    @Test
    public void testSimilar() {
        // 计算两个字符串的相似度
        double ret = StringKit.similar("abc", "ab");
        System.out.println(ret);
        assertNotNull(ret);

        // 返回相似度百分比  scale 保留小数
        String ret1 = StringKit.similar("abcdef", "df", 2);
        System.out.println(ret1);
        assertNotNull(ret1);
    }

    @Test
    public void equalsCharAt() {
        // 字符串指定位置的字符是否与给定字符相同<br>
        // 如果字符串为null，返回false<br>
        // 如果给定的位置大于字符串长度，返回false<br>
        // 如果给定的位置小于0，返回false
        // position 位置  c 需要对比的字符

        assertTrue(StringKit.equalsCharAt("abcd", 1, 'b'));
        assertFalse(StringKit.equalsCharAt(null, 1, 'b'));
        assertFalse(StringKit.equalsCharAt("abcd", 5, 'b'));
        assertFalse(StringKit.equalsCharAt("abcd", -1, 'b'));


    }

    @Test
    public void totalLength() {
        // 给定字符串数组的总长度
        // null字符长度定义为0
        String[] strings = new String[]{"a", "b", "c", "d"};
        String[] strings1 = new String[]{null, "ab", "cde"};
        String[] strings2 = new String[]{null, "ab", "cde", "ffgg", "zzwaaz"};
        assertEquals(StringKit.totalLength(strings), 4);
        assertEquals(StringKit.totalLength(strings1), 5);
        assertEquals(StringKit.totalLength(strings2), 15);
    }

    @Test
    public void move() {
        // 循环位移指定位置的字符串为指定距离<br>
        // 起始位置（包括） endExclude  结束位置（不包括）
        // 当moveLength大于0向右位移，小于0向左位移，0不位移<br>
        // 当moveLength大于字符串长度时采取循环位移策略，既位移到头后从头（尾）位移，例如长度为10，位移13则表示位移3
        assertEquals(StringKit.move("abcdefg", 1, 4, 2), "aefbcdg");
        assertEquals(StringKit.move("abcdefg", 1, 4, -2), "bcdaefg");
        assertEquals(StringKit.move("abcdefg", 1, 4, 8), "aebcdfg");
    }

    @Test
    public void concat() {
        // 连接多个字符串为一个 isNullToEmpty 是否null转为""
        String[] strings = new String[]{"what", "is", "this", null};
        assertEquals(StringKit.concat(true, strings), "whatisthis");
        String[] strings1 = new String[]{"what", "R", "U", null, "Do", "ing"};
        assertEquals(StringKit.concat(false, strings1), "whatRUnullDoing");

    }

    @Test
    public void length() {
        // 获取字符串的长度，如果为null返回0
        assertEquals(StringKit.length("abcd"), 4);
        assertEquals(StringKit.length(null), 0);
    }

    @Test
    public void swapCase() {
        // 切换给定字符串中的大小写。大写转小写，小写转大写
        assertEquals(StringKit.swapCase("nUMBER"), "Number");
        assertEquals(StringKit.swapCase(""), "");
        assertEquals(StringKit.swapCase(null), null);
        assertEquals(StringKit.swapCase("123N124"), "123n124");
    }

    @Test
    public void parseLocaleString() {
        // 将文本转换成Local对象
        String str = "locallocal";
        System.out.println(StringKit.parseLocaleString(str));

        assertNotNull(StringKit.parseLocaleString(str));
    }

    @Test
    public void getFilename() {
        // 获取路径中的文件名称
        String path = this.getClass().getResource("/log4j.xml").getPath();
        assertEquals(StringKit.getFilename(path), "log4j.xml");
        assertNull(StringKit.getFilename(null));

    }

    @Test
    public void getFilenameExtension() {
        // 获取路径中的文件后缀
        String path = this.getClass().getResource("/log4j.xml").getPath();
        assertEquals(StringKit.getFilenameExtension(path), "xml");
        assertNull(StringKit.getFilenameExtension(null));
    }

    @Test
    public void applyRelativePath() {
        // 转换成相对路径

        String str = "file:core/io/Resource.class";
        String str1 = "/D:/DHW/LCZ/server/lczLib/HappyCore/target/test-classes/log4j.xml";
        assertEquals(StringKit.applyRelativePath(str, "../"), "file:core/io/../");
        assertEquals(StringKit.applyRelativePath(str1, "../"), "/D:/DHW/LCZ/server/lczLib/HappyCore/target/test-classes/../");
    }

    @Test
    public void cleanPath() {
        // 清空path中的相对路径
        String str = "file:core/../core/io/Resource.class";
        assertEquals(StringKit.cleanPath(str), "file:core/io/Resource.class");
    }

    @Test
    public void testDelimitedListToStringArray() {
        //把字符串转换为字符类型的数组
        String str = "a,b,c,d";
        String[] strings = StringKit.delimitedListToStringArray(str, ",");
        String[] trueRet = new String[]{"a", "b", "c", "d"};
        assertArrayEquals(strings, trueRet);

        String str1 = "ab,cd,ef,gh";
        String[] strings1 = StringKit.delimitedListToStringArray(str1, ",", "cd");
        String[] trueRet1 = new String[]{"ab", "", "ef", "gh"};
        assertArrayEquals(strings1, trueRet1);
    }

    @Test
    public void deleteAny() {
        assertEquals(StringKit.deleteAny("abcd", "bc"), "ad");
        assertEquals(StringKit.deleteAny("AcdDDDjAcdLASocOldP", "Acd"), "DDDjLSoOlP");
    }

    @Test
    public void toStringArray() {
        Collection<String> collection = new ArrayList<String>();
        collection.add("a");
        collection.add("b");
        collection.add("c");

        String[] strings = new String[]{"a", "b", "c"};
        assertArrayEquals(StringKit.toStringArray(collection), strings);
    }

    @Test
    public void commaDelimitedListToStringArray() {
        String str = "a,b,c";
        String[] strings = new String[]{"a", "b", "c"};
        String str1 = null;
        System.out.println(Arrays.toString(StringKit.commaDelimitedListToStringArray(str)));
        assertArrayEquals(StringKit.commaDelimitedListToStringArray(str), strings);
    }

    @Test
    public void arrayToCommaDelimitedString() {
        // 数组转换位a1,a2这样的字符串
        String[] strings = new String[]{"123", "321"};
        String ret = StringKit.arrayToCommaDelimitedString(strings);
        System.out.println(ret);
        assertEquals(ret, "123,321");
    }

    @Test
    public void padStart() {
        // 如果string的长度小于minLength那么在其起始位置用padChar填充
        assertEquals(StringKit.padStart("abcd", 8, 'a'), "aaaaabcd");
    }

    @Test
    public void padEnd() {
        // 如果string的长度小于minLength那么在其末尾用padChar填充
        assertEquals(StringKit.padEnd("abcd", 8, 'a'), "abcdaaaa");
    }

    @Test
    public void lastIndexOfLetter() {
        // 最后一个字符所在的index 主要是排除_的干扰
        assertEquals(StringKit.lastIndexOfLetter("abcd_"), 4);
        assertEquals(StringKit.lastIndexOfLetter("adacc_afas_ada"), 13);
    }
}