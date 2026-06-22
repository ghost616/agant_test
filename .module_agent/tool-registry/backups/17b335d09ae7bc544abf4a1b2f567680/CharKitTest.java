package com.datanew.core.toolkit;

import org.junit.Test;

import static org.junit.Assert.*;

public class CharKitTest {

    @Test
    public void isAscii() {
        // 是否为ASCII字符，ASCII字符位于0~127之间
        assertTrue(CharKit.isAscii('b'));

    }

    @Test
    public void isAsciiPrintable() {
        // 是否为可见ASCII字符，可见字符位于32~126之间
        assertTrue(CharKit.isAsciiPrintable('b'));
        assertFalse(CharKit.isAsciiPrintable('\n'));
    }

    @Test
    public void isExpressionCode() {
        // 是否表达式码
        assertFalse(CharKit.isExpressionCode('\n'));
        assertFalse(CharKit.isExpressionCode('+'));
    }

    @Test
    public void isAsciiControl() {
        // 是否为ASCII控制符（不可见字符），控制符位于0~31和127
        assertFalse(CharKit.isAsciiControl('a'));
        assertFalse(CharKit.isAsciiControl('+'));
        assertTrue(CharKit.isAsciiControl('\n'));
    }

    @Test
    public void isLetter() {
        // 判断是否为字母（包括大写字母和小写字母）
        assertTrue(CharKit.isLetter('a'));
        assertTrue(CharKit.isLetter('A'));
        assertFalse(CharKit.isLetter('+'));
        assertFalse(CharKit.isLetter('0'));

    }

    @Test
    public void isLetterUpper() {
        // 判断是否为大写字母，大写字母包括A~Z
        assertTrue(CharKit.isLetterUpper('A'));
        assertFalse(CharKit.isLetterUpper('b'));
    }

    @Test
    public void isLetterLower() {
        // 检查字符是否为小写字母，小写字母指a~z
        assertFalse(CharKit.isLetterLower('A'));
        assertTrue(CharKit.isLetterLower('b'));
    }

    @Test
    public void isNumber() {
        // 检查是否为数字字符，数字字符指0~9
        assertFalse(CharKit.isNumber('A'));
        assertFalse(CharKit.isNumber('+'));
        assertFalse(CharKit.isNumber('\n'));
        assertTrue(CharKit.isNumber('2'));
    }

    @Test
    public void isHexChar() {
        // 是否为16进制规范的字符，判断是否为如下字符
        // 1. 0~9
        // 2. a~f
        // 4. A~F
        assertTrue(CharKit.isHexChar('2'));
        assertTrue(CharKit.isHexChar('a'));
        assertTrue(CharKit.isHexChar('A'));
        assertFalse(CharKit.isHexChar('+'));
    }

    @Test
    public void isLetterOrNumber() {
        // 是否为字符或数字，包括A~Z、a~z、0~9
        assertTrue(CharKit.isLetterOrNumber('a'));
        assertTrue(CharKit.isLetterOrNumber('2'));
        assertTrue(CharKit.isLetterOrNumber('A'));
        assertFalse(CharKit.isLetterOrNumber('+'));
    }

    @Test
    public void testToString() {
        // 字符转为字符串
        // 如果为ASCII字符，使用缓存
        String ret = CharKit.toString('b');

        assertEquals(ret, "b");

    }

    @Test
    public void isCharClass() {
        //  给定类名是否为字符类，字符类包括：
        //  Character.class
        //  char.class
        assertFalse(CharKit.isCharClass(String.class));
        assertTrue(CharKit.isCharClass(Character.class));

    }

    @Test
    public void isChar() {
        // 给定对象对应的类是否为字符类，字符类包括：
        //  Character.class
        //  char.class
        assertFalse(CharKit.isChar("abc"));
        assertTrue(CharKit.isChar('a'));
    }

    @Test
    public void testIsBlankChar() {
        //  是否空白符 空白符包括空格、制表符、全角空格和不间断空格
        assertTrue(CharKit.isBlankChar('\t'));
        assertTrue(CharKit.isBlankChar(' '));
        assertTrue(CharKit.isBlankChar('　'));
    }

    @Test
    public void isEmoji() {
        // TODO 未测试
        // 判断是否为emoji表情符
        assertFalse(CharKit.isEmoji('a'));

    }

    @Test
    public void isFileSeparator() {
        // 是否为Windows或者Linux（Unix）文件分隔符
        assertTrue(CharKit.isFileSeparator('\\'));
        assertTrue(CharKit.isFileSeparator('/'));
        assertFalse(CharKit.isFileSeparator('+'));
    }

    @Test
    public void testEquals() {
        // 比较两个字符是否相同
        assertTrue(CharKit.equals('a','A',true));
        assertFalse(CharKit.equals('b','B',false));
    }
}