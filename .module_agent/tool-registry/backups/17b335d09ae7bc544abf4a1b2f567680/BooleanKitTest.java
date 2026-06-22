package com.datanew.core.toolkit;

import org.junit.Test;

import static org.junit.Assert.*;

public class BooleanKitTest {

    @Test
    public void testNegate() {
        // 取�8反值
        assertFalse(BooleanKit.negate(Boolean.TRUE));
        assertTrue(BooleanKit.negate(Boolean.FALSE));
        assertFalse(BooleanKit.negate(true));
        assertTrue(BooleanKit.negate(false));
    }

    @Test
    public void isTrue() {
        // 检查 Boolean�<是否为true
        assertTrue(BooleanKit.isTrue(Boolean.TRUE));
        assertFalse(BooleanKit.isTrue(Boolean.FALSE));
        assertFalse(BooleanKit.isTrue(null));
    }

    @Test
    public void isFalse() {
        // // 检查 Boolean值是e��为false
        assertFalse(BooleanKit.isFalse(Boolean.TRUE));
        assertTrue(BooleanKit.isFalse(Boolean.FALSE));
        assertFalse(BooleanKit.isFalse(null));
    }

    @Test
    public void toBoolean() {
        //  转换字符串为boolean值
        assertTrue(BooleanKit.toBoolean("真 "));
        assertTrue(BooleanKit.toBoolean("true"));
        assertTrue(BooleanKit.toBoolean(" on "));
        assertTrue(BooleanKit.toBoolean("是"));
        assertTrue(BooleanKit.toBoolean("yes"));
        assertTrue(BooleanKit.toBoolean("Y"));
        assertFalse(BooleanKit.toBoolean("No"));
        assertFalse(BooleanKit.toBoolean("XXX"));
        assertTrue(BooleanKit.toBoolean("对"));
        assertFalse(BooleanKit.toBoolean("对的"));
        assertFalse(BooleanKit.toBoolean("错误"));
    }

    @Test
    public void toInt() {
        // boolean值�,为int
        assertEquals(BooleanKit.toInt(true), 1);
        assertEquals(BooleanKit.toInt(Boolean.TRUE), 1);
        assertEquals(BooleanKit.toInt(false), 0);
        assertEquals(BooleanKit.toInt(Boolean.FALSE), 0);
    }

    @Test
    public void toInteger() {
        // boolean值转为Integer
        assertEquals(BooleanKit.toInteger(true), new Integer(1));
        assertEquals(BooleanKit.toInteger(Boolean.TRUE), new Integer(1));
        assertEquals(BooleanKit.toInteger(false), new Integer(0));
        assertEquals(BooleanKit.toInteger(Boolean.FALSE), new Integer(0));
    }

    @Test
    public void toChar() {
        // boolean值转d��char
        assertEquals(BooleanKit.toChar(true), 1);
        assertEquals(BooleanKit.toChar(Boolean.TRUE), 1);
        assertEquals(BooleanKit.toChar(false), 0);
        assertEquals(BooleanKit.toChar(Boolean.FALSE), 0);
    }

    @Test
    public void toCharacter() {
        // boolean值转为Character
        assertEquals(BooleanKit.toCharacter(true), new Character('\u0001'));
        assertEquals(BooleanKit.toCharacter(Boolean.TRUE), new Character('\u0001'));
        assertEquals(BooleanKit.toCharacter(false), new Character('\u0000'));
        assertEquals(BooleanKit.toCharacter(Boolean.FALSE), new Character('\u0000'));
    }

    @Test
    public void toByte() {
        // boolean值转为byte
        assertEquals(BooleanKit.toByte(true), 1);
        assertEquals(BooleanKit.toByte(Boolean.TRUE), 1);
        assertEquals(BooleanKit.toByte(false), 0);
        assertEquals(BooleanKit.toByte(Boolean.FALSE), 0);
    }

    @Test
    public void toByteObj() {
        // booleane��转为Byte
        assertEquals(BooleanKit.toByteObj(true), new Byte("1"));
        assertEquals(BooleanKit.toByteObj(Boolean.TRUE), new Byte("1"));
        assertEquals(BooleanKit.toByteObj(false), new Byte("0"));
        assertEquals(BooleanKit.toByteObj(Boolean.FALSE), new Byte("0"));
    }

    @Test
    public void toLong() {
        // boolean值转为long
        assertEquals(BooleanKit.toLong(true), 1L);
        assertEquals(BooleanKit.toLong(Boolean.TRUE), 1L);
        assertEquals(BooleanKit.toLong(false), 0L);
        assertEquals(BooleanKit.toLong(Boolean.FALSE), 0L);
    }

    @Test
    public void toLongObj() {
        // boolean值转为Long
        assertEquals(BooleanKit.toLongObj(true), new Long("1"));
        assertEquals(BooleanKit.toLongObj(Boolean.TRUE), new Long("1"));
        assertEquals(BooleanKit.toLongObj(false), new Long("0"));
        assertEquals(BooleanKit.toLongObj(Boolean.FALSE), new Long("0"));
    }

    @Test
    public void toShort() {
        // boolean值转为short
        assertEquals(BooleanKit.toShort(true), 1);
        assertEquals(BooleanKit.toShort(Boolean.TRUE), 1);
        assertEquals(BooleanKit.toShort(false), 0);
        assertEquals(BooleanKit.toShort(Boolean.FALSE), 0);
    }

    @Test
    public void toShortObj() {
        // boolean值转为Short
        assertEquals(BooleanKit.toShortObj(true), new Short("1"));
        assertEquals(BooleanKit.toShortObj(Boolean.TRUE), new Short("1"));
        assertEquals(BooleanKit.toShortObj(false), new Short("0"));
        assertEquals(BooleanKit.toShortObj(Boolean.FALSE), new Short("0"));
    }

    @Test
    public void toFloat() {
        // boolean值转d��float
        assertEquals(BooleanKit.toFloat(true), 1f, 0);
        assertEquals(BooleanKit.toFloat(Boolean.TRUE), 1f, 0);
        assertEquals(BooleanKit.toFloat(false), 0f, 0);
        assertEquals(BooleanKit.toFloat(Boolean.FALSE), 0f, 0);
    }

    @Test
    public void toFloatObj() {
        // boolean值转为Float
        assertEquals(BooleanKit.toFloatObj(true), new Float("1"));
        assertEquals(BooleanKit.toFloatObj(Boolean.TRUE), new Float("1"));
        assertEquals(BooleanKit.toFloatObj(false), new Float("0"));
        assertEquals(BooleanKit.toFloatObj(Boolean.FALSE), new Float("0"));
    }

    @Test
    public void toDouble() {
        // boolean值转�:double
        assertEquals(BooleanKit.toDouble(true), 1, 0);
        assertEquals(BooleanKit.toDouble(Boolean.TRUE), 1, 0);
        assertEquals(BooleanKit.toDouble(false), 0, 0);
        assertEquals(BooleanKit.toDouble(Boolean.FALSE), 0, 0);
    }

    @Test
    public void toDoubleObj() {
        // boolean值转为Double
        assertEquals(BooleanKit.toDoubleObj(true), new Double("1"));
        assertEquals(BooleanKit.toDoubleObj(Boolean.TRUE), new Double("1"));
        assertEquals(BooleanKit.toDoubleObj(false), new Double("0"));
        assertEquals(BooleanKit.toDoubleObj(Boolean.FALSE), new Double("0"));
    }

    @Test
    public void toStringTrueFalse() {
        // 将boolean转换为字符串'true' 或者 'false'.
        assertEquals(BooleanKit.toStringTrueFalse(true), "true");
        assertEquals(BooleanKit.toStringTrueFalse(Boolean.TRUE), "true");
        assertEquals(BooleanKit.toStringTrueFalse(false), "false");
        assertEquals(BooleanKit.toStringTrueFalse(Boolean.FALSE), "false");
    }

    @Test
    public void toStringOnOff() {
        // 将boolean转换为字符串 'on' 或� 'off'
        assertEquals(BooleanKit.toStringOnOff(true), "on");
        assertEquals(BooleanKit.toStringOnOff(Boolean.TRUE), "on");
        assertEquals(BooleanKit.toStringOnOff(false), "off");
        assertEquals(BooleanKit.toStringOnOff(Boolean.FALSE), "off");
    }

    @Test
    public void toStringYesNo() {
        // 将boolean转f��为字符串 'yes' 或者 'no'
        assertEquals(BooleanKit.toStringYesNo(true), "yes");
        assertEquals(BooleanKit.toStringYesNo(Boolean.TRUE), "yes");
        assertEquals(BooleanKit.toStringYesNo(false), "no");
        assertEquals(BooleanKit.toStringYesNo(Boolean.FALSE), "no");
    }

    @Test
    public void testToString() {
        // 将boolean转换为�-�符串
        assertEquals(BooleanKit.toString(true, "yes", "no"), "yes");
        assertEquals(BooleanKit.toString(Boolean.TRUE, "true", "false"), "true");
        assertEquals(BooleanKit.toString(false, "True", "False"), "False");
        assertEquals(BooleanKit.toString(Boolean.FALSE, "1", "0"), "0");
    }

    @Test
    public void testAnd() {
        // 对Booleanf��组取与 &

        assertTrue(BooleanKit.and(new boolean[]{true, true}));
        assertFalse(BooleanKit.and(new boolean[]{false, false}));
        assertFalse(BooleanKit.and(new boolean[]{true, false}));
        assertFalse(BooleanKit.and(new boolean[]{true, true, false}));
        assertTrue(BooleanKit.and(new boolean[]{true, true, true}));
        assertTrue(BooleanKit.and(new boolean[]{Boolean.TRUE, Boolean.TRUE}));
        assertFalse(BooleanKit.and(new boolean[]{Boolean.FALSE, Boolean.FALSE}));
        assertFalse(BooleanKit.and(new boolean[]{Boolean.TRUE, Boolean.FALSE}));
        assertTrue(BooleanKit.and(new boolean[]{Boolean.TRUE, Boolean.TRUE, Boolean.TRUE}));
        assertFalse(BooleanKit.and(new boolean[]{Boolean.FALSE, Boolean.FALSE, Boolean.TRUE}));
        assertFalse(BooleanKit.and(new boolean[]{Boolean.TRUE, Boolean.FALSE, Boolean.TRUE}));

    }

    @Test
    public void testOr() {
        // 对Booleanf��组取或 ||
        assertTrue(BooleanKit.or(new boolean[]{true, true}));
        assertFalse(BooleanKit.or(new boolean[]{false, false}));
        assertTrue(BooleanKit.or(new boolean[]{true, false}));
        assertTrue(BooleanKit.or(new boolean[]{true, true, false}));
        assertTrue(BooleanKit.or(new boolean[]{true, true, true}));
        assertTrue(BooleanKit.or(new boolean[]{Boolean.TRUE, Boolean.TRUE}));
        assertFalse(BooleanKit.or(new boolean[]{Boolean.FALSE, Boolean.FALSE}));
        assertTrue(BooleanKit.or(new boolean[]{Boolean.TRUE, Boolean.FALSE}));
        assertTrue(BooleanKit.or(new boolean[]{Boolean.TRUE, Boolean.TRUE, Boolean.TRUE}));
        assertTrue(BooleanKit.or(new boolean[]{Boolean.FALSE, Boolean.FALSE, Boolean.TRUE}));
        assertTrue(BooleanKit.or(new boolean[]{Boolean.TRUE, Boolean.FALSE, Boolean.TRUE}));
    }


    @Test
    public void testXor() {
        //  对Boolean数组取异或
        assertFalse(BooleanKit.xor(new boolean[]{true, true}));
        assertFalse(BooleanKit.xor(new boolean[]{false, false}));
        assertTrue(BooleanKit.xor(new boolean[]{true, false}));
        assertFalse(BooleanKit.xor(new boolean[]{Boolean.TRUE, Boolean.TRUE}));
        assertFalse(BooleanKit.xor(new boolean[]{Boolean.FALSE, Boolean.FALSE}));
        assertTrue(BooleanKit.xor(new boolean[]{Boolean.TRUE, Boolean.FALSE}));

    }

}