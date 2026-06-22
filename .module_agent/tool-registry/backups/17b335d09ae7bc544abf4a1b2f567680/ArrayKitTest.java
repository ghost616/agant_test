package com.datanew.core.toolkit;

import com.datanew.core.lang.Editor;
import com.datanew.core.lang.Filter;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.*;

public class ArrayKitTest {

    @Test
    public void isEmpty() {
        // 数组是否为空
        String[] strings = new String[]{};
        assertTrue(ArrayKit.isEmpty(strings));

        long[] longs = new long[]{};
        assertTrue(ArrayKit.isEmpty(longs));

        Integer[] integers = new Integer[]{};
        assertTrue(ArrayKit.isEmpty(integers));

        short[] shorts = new short[]{};
        assertTrue(ArrayKit.isEmpty(shorts));

        char[] chars = new char[]{};
        assertTrue(ArrayKit.isEmpty(chars));

        byte[] bytes = new byte[]{};
        assertTrue(ArrayKit.isEmpty(bytes));

        double[] doubles = new double[]{};
        assertTrue(ArrayKit.isEmpty(doubles));

        float[] floats = new float[]{};
        assertTrue(ArrayKit.isEmpty(floats));

        boolean[] booleans = new boolean[]{};
        assertTrue(ArrayKit.isEmpty(booleans));
    }

    @Test
    public void testIsEmpty() {
        // 数组是否为空
        // 此方法会匹配单一对象，如果此对象为null则返回true
        // 如果此对象为非数组，理解为此对象为数组的第一个元素，则返回false
        // 如果此对象为数组对象，数组长度大于0情况下返回false，否则返回true
        String[] strings = new String[]{};
        Object ret = (Object[]) strings;
        assertTrue(ArrayKit.isEmpty(ret));
        assertTrue(ArrayKit.isEmpty((Object) null));

        Person p = new Person("zhangsan",18);
        assertFalse(ArrayKit.isEmpty((Object)p));

    }

    @Test
    public void isNotEmpty() {
        // 数组是否为非空
        String[] strings = new String[]{};
        assertFalse(ArrayKit.isNotEmpty(strings));

        long[] longs = new long[]{};
        assertFalse(ArrayKit.isNotEmpty(longs));

        Integer[] integers = new Integer[]{};
        assertFalse(ArrayKit.isNotEmpty(integers));

        short[] shorts = new short[]{};
        assertFalse(ArrayKit.isNotEmpty(shorts));

        char[] chars = new char[]{};
        assertFalse(ArrayKit.isNotEmpty(chars));

        byte[] bytes = new byte[]{};
        assertFalse(ArrayKit.isNotEmpty(bytes));

        double[] doubles = new double[]{};
        assertFalse(ArrayKit.isNotEmpty(doubles));

        float[] floats = new float[]{};
        assertFalse(ArrayKit.isNotEmpty(floats));

        boolean[] booleans = new boolean[]{};
        assertFalse(ArrayKit.isNotEmpty(booleans));
    }

    @Test
    public void hasNull() {
        // 是否包含null元素
        String[] strings = new String[]{"1", null};
        assertTrue(ArrayKit.hasNull(strings));
    }

    @Test
    public void firstNonNull() {
        // 返回数组中第一个非空元素
        String[] strings = new String[]{null,"3","4","1", null};
        assertEquals("3", ArrayKit.getFirstNonNull(strings));
    }

    @Test
    public void testNewArray() {
        // 新建一个空数组
        String[] strings = ArrayKit.newArray(String.class, 3);
        assertNotNull(strings);

        Object[] strings1 = ArrayKit.newArray(1);
        assertNotNull(strings1);
    }

    @Test
    public void testGetComponentType() {
        // 获取数组对象的元素类型
        String[] strings = new String[]{"1", null};
        Integer[] integers = new Integer[]{12,324,null};
        Person[] persons = new Person[]{new Person("zhangsan",15),new Person("lisi",28)};
        // array 数组对象
        assertEquals(String.class, ArrayKit.getComponentType(strings));
        assertEquals(Integer.class, ArrayKit.getComponentType(integers));
        assertEquals(Person.class, ArrayKit.getComponentType(persons));
        // arrayClass 数组类
        assertEquals(String.class, ArrayKit.getComponentType(strings.getClass()));
        assertEquals(Integer.class, ArrayKit.getComponentType(integers.getClass()));
        assertEquals(Person.class, ArrayKit.getComponentType(persons.getClass()));
    }

    @Test
    public void getArrayType() {
        // 根据数组元素类型，获取数组的类型  componentType 数组元素类型
        assertNotNull(ArrayKit.getArrayType(String.class));
    }

    @Test
    public void cast() {
        // 强转数组类型(强制转换的前提是数组元素类型可被强制转换)
        // type 数组类型或数组元素类型  arrayObj 原数组
        // TODO 运行时报错java.lang.ArrayStoreException
        String[] strings = new String[]{"1"};
        Object[] integers = ArrayKit.cast(Integer.class, strings);
    }


    @Test
    public void append() {
        // 将新元素添加到已有数组中
        Integer[] integers = new Integer[]{1, 2};
        assertArrayEquals(new Integer[]{3, 1, 2}, ArrayKit.append(new Integer[]{3}, integers));
        // 添加新元素会生成一个新的数组，不影响原数组
        assertArrayEquals(new Integer[]{1, 2}, integers);
    }

    @Test
    public void testAppend() {
        // 将新元素添加到已有数组中
        Integer[] integers = new Integer[]{1, 2};
        Object append = ArrayKit.append((Object) integers, 3);
        Integer[] res = (Integer[]) append;
        assertArrayEquals(new Integer[]{1, 2, 3}, res);
    }

    @Test
    public void testSetOrAppend() {
        // 将元素值设置为数组的某个位置，当给定的index大于数组长度，则追加
        Integer[] integers = new Integer[]{1, 2};
        assertArrayEquals(ArrayKit.setOrAppend(integers, 3, 3), new Integer[]{1, 2, 3});
        Person[] per = new Person[]{
                new Person("（化名）张三", 23),
                new Person("（化名）李四", 24),
                new Person("（化名）王五", 25),
                new Person("帅气的‘杨木发’", 20)
        };
        ArrayKit.setOrAppend(per, 1, new Person("xxx", 1));
    }

    @Test
    public void testInsert() {
        // 将新元素插入到到已有数组中的某个位置 添加新元素会生成一个新的数组，不影响原数组
        // 如果插入位置为为负数，从原数组从后向前计数，若大于原数组长度，则空白处用null填充
        Integer[] integers = new Integer[]{1, 2};
        assertArrayEquals(ArrayKit.insert(integers, 3, 3), new Integer[]{1, 2, null, 3});
        assertArrayEquals(ArrayKit.insert(integers, 0, 3), new Integer[]{3, 1, 2});
        assertArrayEquals(ArrayKit.insert(integers, -1, 3), new Integer[]{1, 3, 2});
    }

    @Test
    public void testResize() {
        // 生成一个新的重新设置大小的数组  componentType 数组元素类型
        // 调整大小后拷贝原数组到新数组下。扩大则占位前N个位置，缩小则截断
        Integer[] integers = new Integer[]{1, 2};
        Object[] strings = ArrayKit.resize(integers, 5, Integer.class);
        assertArrayEquals(new Integer[]{1,2,null,null,null},strings);

        Integer[] integers1 = new Integer[]{1, 2,4,5,6,7};
        Object[] strings1 = ArrayKit.resize(integers1, 5, Integer.class);
        assertArrayEquals(new Integer[]{1,2,4,5,6},strings1);
    }


    @Test
    public void addAll() {
        // 将多个数组合并在一起  忽略null的数组
        String[] strings = new String[]{"1"};
        String[] strings1 = new String[]{"2"};
        String[] ret = ArrayKit.addAll(strings, strings1);
        assertArrayEquals(ret, new String[]{"1", "2"});

        String[] strings2 = new String[]{"1"};
        String[] strings3 = null;
        String[] strings4 = new String[]{"3",null,"7"};
        String[] ret1 = ArrayKit.addAll(strings2, strings3,strings4);
        assertArrayEquals(ret1, new String[]{"1", "3",null,"7"});
    }

    @Test
    public void testCopy() {
        // 数组复制，原数组和目标数组都是从位置0开始复制
        String[] strings = new String[]{"1"};

        String[] strings1 = new String[]{"2"};


        Object ret = ArrayKit.copy(strings, 0, strings1, 0, 1);
        assertArrayEquals((Object[]) ret, new String[]{"1"});

        // 原数组和目标数组都是从位置0开始复制
        Object ret1 = ArrayKit.copy(strings, strings1, 1);
        assertArrayEquals((Object[]) ret1, new String[]{"1"});
    }

    @Test
    public void testClone() {
        // 克隆数组
        String[] strings = new String[]{"1"};
        String[] ret = ArrayKit.clone(strings);
        assertNotNull(ret);

        //克隆数组，如果非数组返回null
        Object stringObj = (Object[]) strings;
        Object ret1 = ArrayKit.clone(stringObj);
        assertNotNull(ret1);
        assertNull(ArrayKit.clone(null));
    }


    @Test
    public void testRange() {
        // 生成一个从0开始的数字列表
        assertArrayEquals(new int[]{0, 1, 2, 3}, ArrayKit.range(4));
        // 生成一个数字列表  includedStart 开始的数字（包含） excludedEnd 结束的数字（不包含）
        assertArrayEquals(new int[]{1, 2, 3,4,5,6}, ArrayKit.range(1, 7));
        // step 步进
        assertArrayEquals(new int[]{1, 3, 5}, ArrayKit.range(1, 7, 2));
    }

    @Test
    public void split() {
        // 拆分byte数组为几个等份（最后一份如果长度不足len 用0填充）  len 每个小节的长度
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        byte[][] ret = ArrayKit.split(bytes, 2);
        System.out.println(Arrays.deepToString(ret));
        byte[][] trueRet = new byte[][]{{1, 2}, {3, 4}, {5, 0}};
        assertArrayEquals(trueRet, ret);

    }

    @Test
    public void filter() {
        // 过滤
        // 过滤过程通过传入的Filter实现来过滤返回需要的元素内容，这个Filter实现可以实现以下功能：
        // 过滤出需要的对象，Filter中的accept(Object)方法返回true的对象将被加入结果集合中
        String[] strings = new String[]{"1", null};
        final String[] ret = ArrayKit.filter(strings, new Editor<String>() {
            @Override
            public String edit(String s) {
                if (s == null) {
                    s = "0";
                }
                return s;
            }
        });
        System.out.println(Arrays.toString(ret));
        assertArrayEquals(new String[]{"1", "0"}, ret);

        String[] strings1 = new String[]{"1", null};
        String[] ret1 = ArrayKit.filter(strings1, new Filter<String>() {
            @Override
            public boolean accept(String s) {
                if (s == null) {
                    return false;
                }
                return true;
            }
        });
        System.out.println(Arrays.toString(ret1));
        assertArrayEquals(new String[]{"1"}, ret1);
    }

    @Test
    public void removeNull() {
        // 去除null元素
        String[] strings1 = new String[]{"1", null};
        assertArrayEquals(new String[]{"1"}, ArrayKit.removeNull(strings1));
    }

    @Test
    public void removeEmpty() {
        // 去除null或者""元素
        String[] strings1 = new String[]{"1", null, ""};
        assertArrayEquals(new String[]{"1"}, ArrayKit.removeEmpty(strings1));
    }

    @Test
    public void removeBlank() {
        // 去除null或者""或者空白字符串元素
        String[] strings1 = new String[]{"1", " ", ""};
        assertArrayEquals(new String[]{"1"}, ArrayKit.removeBlank(strings1));
    }

    @Test
    public void nullToEmpty() {
        // 数组元素中的null转换为""
        String[] strings1 = new String[]{"1", null, ""};
        assertArrayEquals(new String[]{"1", "", ""}, ArrayKit.nullToEmpty(strings1));
    }

    @Test
    public void zip() {
        // 映射键值（参考Python的zip()函数）
        // <K> Key类型 <V> Value类型 keys 键列表 values 值列表 isOrder 是否有序
        String[] keyList = new String[]{"1", "2", "3", "4", "5"};
        String[] valueList = new String[]{"one", "two", "three", "four", "five"};
        Map<String, String> map = ArrayKit.zip(keyList, valueList, true);
        System.out.println(map);
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("1", "one");
        hashMap.put("2", "two");
        hashMap.put("3", "three");
        hashMap.put("4", "four");
        hashMap.put("5", "five");

        assertEquals(hashMap, map);
    }

    @Test
    public void testIndexOf() {
        // 返回数组中指定元素所在位置，未找到返回INDEX_NOT_FOUND(-1)
        // 相同元素返回靠前的元素的位置
        String[] strings = new String[]{"1", "2", "3", "4", "5"};
        assertEquals(2, ArrayKit.indexOf(strings, "3"));
        assertEquals(-1, ArrayKit.indexOf(strings, "6"));

        long[] longs = new long[]{1, 2, 3, 4};
        assertEquals(2, ArrayKit.indexOf(longs, 3));

        int[] ints = new int[]{1, 2, 3, 4};
        assertEquals(2, ArrayKit.indexOf(ints, 3));

        short[] shorts = new short[]{1, 2, 3, 4};
        assertEquals(2, ArrayKit.indexOf(shorts, (short) 3));

        char[] chars = new char[]{'1', '2', '3', '4'};
        assertEquals(2, ArrayKit.indexOf(chars, '3'));

        byte[] bytes = new byte[]{1, 2, 3, 4};
        assertEquals(2, ArrayKit.indexOf(bytes, (byte) 3));

        double[] doubles = new double[]{1.1, 2.2, 3.3, 4.4};
        assertEquals(2, ArrayKit.indexOf(doubles, 3.3));

        float[] floats = new float[]{1.1f, 2.2f, 3.3f, 4.4f};
        assertEquals(2, ArrayKit.indexOf(floats, 3.3f));

        boolean[] booleans = new boolean[]{true, false};
        assertEquals(0, ArrayKit.indexOf(booleans, true));
    }

    @Test
    public void indexOfIgnoreCase() {
        // 返回数组中指定元素所在位置，忽略大小写，未找到返回INDEX_NOT_FOUND(-1)
        // 相同元素返回靠前的元素的位置
        String[] strings = new String[]{"A", "b", "C", "d", "E"};
        assertEquals(2, ArrayKit.indexOfIgnoreCase(strings, "c"));

        String[] strings1 = new String[]{"A", "b", "C", "d", "E","B","c","a","e"};
        assertEquals(4, ArrayKit.indexOfIgnoreCase(strings1, "e"));
    }

    @Test
    public void testLastIndexOf() {
        // 返回数组中指定元素所在最后的位置，未找到返回INDEX_NOT_FOUND(-1)
        String[] strings = new String[]{"A", "b", "C", "d", "E", "A"};
        assertEquals(5, ArrayKit.lastIndexOf(strings, "A"));

        long[] longs = new long[]{1, 2, 3, 4, 3};
        assertEquals(4, ArrayKit.lastIndexOf(longs, 3));

        int[] ints = new int[]{1, 2, 3, 4, 3};
        assertEquals(4, ArrayKit.lastIndexOf(ints, 3));

        short[] shorts = new short[]{1, 2, 3, 4, 3};
        assertEquals(4, ArrayKit.lastIndexOf(shorts, (short) 3));

        char[] chars = new char[]{'1', '2', '3', '4', '3'};
        assertEquals(4, ArrayKit.lastIndexOf(chars, '3'));

        byte[] bytes = new byte[]{1, 2, 3, 4, 3};
        assertEquals(4, ArrayKit.lastIndexOf(bytes, (byte) 3));

        double[] doubles = new double[]{1.1, 2.2, 3.3, 4.4, 3.3};
        assertEquals(4, ArrayKit.lastIndexOf(doubles, 3.3));

        float[] floats = new float[]{1.1f, 2.2f, 3.3f, 4.4f, 3.3f};
        assertEquals(4, ArrayKit.lastIndexOf(floats, 3.3f));

        boolean[] booleans = new boolean[]{true, false, true};
        assertEquals(2, ArrayKit.lastIndexOf(booleans, true));
    }

    @Test
    public void contains() {
        // 数组中是否包含元素
        String[] strings = new String[]{"A", "b", "C", "d", "E", "A"};
        assertTrue(ArrayKit.contains(strings, "C"));

        long[] longs = new long[]{1, 2, 3, 4, 3};
        assertTrue(ArrayKit.contains(longs, 3));

        int[] ints = new int[]{1, 2, 3, 4, 3};
        assertTrue(ArrayKit.contains(ints, 3));

        short[] shorts = new short[]{1, 2, 3, 4, 3};
        assertTrue(ArrayKit.contains(shorts, (short) 3));

        char[] chars = new char[]{'1', '2', '3', '4', '3'};
        assertTrue(ArrayKit.contains(chars, '3'));

        byte[] bytes = new byte[]{1, 2, 3, 4, 3};
        assertTrue(ArrayKit.contains(bytes, (byte) 3));

        double[] doubles = new double[]{1.1, 2.2, 3.3, 4.4, 3.3};
        assertTrue(ArrayKit.contains(doubles, 3.3));

        float[] floats = new float[]{1.1f, 2.2f, 3.3f, 4.4f, 3.3f};
        assertTrue(ArrayKit.contains(floats, 3.3f));

        boolean[] booleans = new boolean[]{true, false, true};
        assertTrue(ArrayKit.contains(booleans, true));

    }

    @Test
    public void containsAny() {
        // 数组中是否包含指定元素中的任意一个
        String[] strings = new String[]{"A", "b", "C", "d", "E", "A"};
        assertTrue(ArrayKit.containsAny(strings, "e", "f", "A"));
    }

    @Test
    public void containsIgnoreCase() {
        // 数组中是否包含元素，忽略大小写
        String[] strings = new String[]{"A", "b", "C", "d", "E", "A"};
        assertTrue(ArrayKit.containsIgnoreCase(strings, "c"));
    }

    @Test
    public void testWrap() {
        // 将原始类型数组包装为包装类型
        Integer[] ret = ArrayKit.wrap(1, 2, 3, 4, 5);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, ret);

        Long[] ret1 = ArrayKit.wrap(1L, 2L, 3L, 4L, 5L);
        assertArrayEquals(new Long[]{1L, 2L, 3L, 4L, 5L}, ret1);

        Character[] ret2 = ArrayKit.wrap('1', '2', '3', '4', '5');
        assertArrayEquals(new Character[]{'1', '2', '3', '4', '5'}, ret2);

        Byte[] ret3 = ArrayKit.wrap((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new Byte[]{1, 2, 3, 4, 5}, ret3);

        Short[] ret4 = ArrayKit.wrap((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        assertArrayEquals(new Short[]{1, 2, 3, 4, 5}, ret4);

        Float[] ret5 = ArrayKit.wrap(1F, 2, 3F, 4F, 5F);
        assertArrayEquals(new Float[]{1F, 2F, 3F, 4F, 5F}, ret5);

        Double[] ret6 = ArrayKit.wrap(1.1, 2.2, 3.3, 4.4, 5.5);
        assertArrayEquals(new Double[]{1.1, 2.2, 3.3, 4.4, 5.5}, ret6);

        Boolean[] ret7 = ArrayKit.wrap(true, false);
        assertArrayEquals(new Boolean[]{true, false}, ret7);
    }

    @Test
    public void testUnWrap() {
        // 包装类数组转为原始类型数组
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, ArrayKit.unWrap(integers));

        Long[] longs = new Long[]{1L, 2L, 3L, 4L, 5L};
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L, 5L}, ArrayKit.unWrap(longs));

        Character[] characters = new Character[]{'1', '2', '3', '4', '5'};
        assertArrayEquals(new char[]{'1', '2', '3', '4', '5'}, ArrayKit.unWrap(characters));

        Byte[] bytes = new Byte[]{1, 2, 3, 4, 5};
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, ArrayKit.unWrap(bytes));

        Short[] shorts = new Short[]{1, 2, 3, 4, 5};
        assertArrayEquals(new short[]{1, 2, 3, 4, 5}, ArrayKit.unWrap(shorts));

        Float[] floats = new Float[]{1f, 2f, 3f, 4f, 5f};

        assertEquals(Arrays.toString(new float[]{1f, 2f, 3f, 4f, 5f}), Arrays.toString(ArrayKit.unWrap(floats)));

        Double[] doubles = new Double[]{1.1, 2.2, 3.3, 4.4, 5.5};
        assertEquals(Arrays.toString(new double[]{1.1, 2.2, 3.3, 4.4, 5.5}), Arrays.toString(ArrayKit.unWrap(doubles)));

        Boolean[] booleans = new Boolean[]{true, false};
        assertEquals(Arrays.toString(new boolean[]{true, false}), Arrays.toString(ArrayKit.unWrap(booleans)));
    }


    @Test
    public void isArray() {
        // 对象是否为数组对象  如果为null 返回false
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        assertTrue(ArrayKit.isArray(integers));
    }

    @Test
    public void get() {
        // 获取数组对象中指定index的值，支持负数，例如-1表示倒数第一个值
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        assertEquals((Integer) 5, ArrayKit.get(integers, -1));
        assertEquals((Integer) 4, ArrayKit.get(integers, 3));
        assertEquals((Integer) 3, ArrayKit.get(integers, -3));
    }

    @Test
    public void getAny() {
        // 获取数组中指定多个下标元素值，组成新数组
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        assertArrayEquals(new Integer[]{1, 2}, ArrayKit.getAny(integers, 0, 1));
    }

    @Test
    public void testSub() {
        // 获取子数组  start 开始位置（包括） end 结束位置（不包括）
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        Integer[] ret = ArrayKit.sub(integers, 1, 4);
        assertArrayEquals(new Integer[]{2, 3, 4}, ret);

        Object integerObj = (Object[]) integers;
        Object[] ret1 = ArrayKit.sub(integerObj, 1, 4);
        assertArrayEquals(new Object[]{2, 3, 4}, ret1);

        // step  步进
        Object[] ret2 = ArrayKit.sub(integerObj, 0, 4, 2);
        assertArrayEquals(new Object[]{1, 3}, ret2);

    }

    @Test
    public void testToString() {
        // 数组或集合转String
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        String[] strings = new String[]{"1", "2", "3", "4", "5"};
        assertEquals(Arrays.toString(strings), ArrayKit.toString(integers));
    }

    @Test
    public void length() {
        // 获取数组长度
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        assertEquals(5, ArrayKit.length(integers));
    }

    @Test
    public void testJoin() {
        // 以 conjunction 为分隔符将数组转换为字符串
        Integer[] integers = new Integer[]{1, 2, 3, 4, 5};
        assertEquals("1+2+3+4+5", ArrayKit.join(integers, "+"));

        // prefix 每个元素添加的前缀，null表示不添加  suffix 每个元素添加的后缀，null表示不添加
        assertEquals("a1a+a2a+a3a+a4a+a5a", ArrayKit.join(integers, "+", "a", "a"));
        long[] longs = new long[]{1L, 2L, 3L};
        assertEquals("1+2+3", ArrayKit.join(longs, "+"));

        int[] ints = new int[]{1, 2, 3};
        assertEquals("1+2+3", ArrayKit.join(ints, "+"));

        char[] chars = new char[]{'1', '2', '3'};
        assertEquals("1+2+3", ArrayKit.join(chars, "+"));

        short[] shorts = new short[]{1, 2, 3};
        assertEquals("1+2+3", ArrayKit.join(shorts, "+"));

        byte[] bytes = new byte[]{1, 2, 3};
        assertEquals("1+2+3", ArrayKit.join(bytes, "+"));

        boolean[] booleans = new boolean[]{true, false};
        assertEquals("true+false", ArrayKit.join(booleans, "+"));

        float[] floats = new float[]{1f, 2f, 3f};
        assertEquals("1.0+2.0+3.0", ArrayKit.join(floats, "+"));

        double[] doubles = new double[]{1, 2, 3};
        assertEquals("1.0+2.0+3.0", ArrayKit.join(doubles, "+"));

    }


    @Test
    public void testToArray() {
        // ByteBuffer 转byte数组
        ByteBuffer buffer = ByteBuffer.allocate(2);
        byte[] ret = ArrayKit.toArray(buffer);
        System.out.println(Arrays.toString(ret));
        assertNotNull(ret);

        List<String> strings = new ArrayList<String>();
        Iterator iterator = strings.iterator();
        // 将集合转为数组  componentType 集合元素类型
        assertNotNull(ArrayKit.toArray(iterator, String.class));
        assertNotNull(ArrayKit.toArray(strings, String.class));
    }


    @Test
    public void testRemove() {
        // 移除数组中对应位置的元素
        String[] strings = new String[]{"1", "2"};
        assertArrayEquals(new String[]{"1"}, ArrayKit.remove(strings, 1));

        long[] longs = new long[]{1L, 2L};
        assertArrayEquals(new long[]{1}, ArrayKit.remove(longs, 1));

        int[] ints = new int[]{1, 2};
        assertArrayEquals(new int[]{1}, ArrayKit.remove(ints, 1));

        short[] shorts = new short[]{1, 2};
        assertArrayEquals(new short[]{1}, ArrayKit.remove(shorts, 1));

        char[] chars = new char[]{'1', '2'};
        assertArrayEquals(new char[]{'1'}, ArrayKit.remove(chars, 1));

        byte[] bytes = new byte[]{1, 2};
        assertArrayEquals(new byte[]{1}, ArrayKit.remove(bytes, 1));

        double[] doubles = new double[]{1, 2};
        assertEquals(Arrays.toString(new double[]{1}), Arrays.toString(ArrayKit.remove(doubles, 1)));

        float[] floats = new float[]{1f, 2f};
        assertEquals(Arrays.toString(new float[]{1f}), Arrays.toString(ArrayKit.remove(floats, 1)));

        boolean[] booleans = new boolean[]{true, false};
        assertEquals(Arrays.toString(new boolean[]{true}), Arrays.toString(ArrayKit.remove(booleans, 1)));


    }


    @Test
    public void testRemoveEle() {
        // 移除数组中指定的元素  只会移除匹配到的第一个元素
        String[] strings = new String[]{"1", "2", "1"};
        assertArrayEquals(new String[]{"2", "1"}, ArrayKit.removeEle(strings, "1"));

        long[] longs = new long[]{1L, 2L, 1L};
        assertArrayEquals(new long[]{2L, 1L}, ArrayKit.removeEle(longs, 1L));

        int[] ints = new int[]{1, 2, 1};
        assertArrayEquals(new int[]{2, 1}, ArrayKit.removeEle(ints, 1));

        short[] shorts = new short[]{1, 2, 1};
        assertArrayEquals(new short[]{2, 1}, ArrayKit.removeEle(shorts, (short) 1));

        char[] chars = new char[]{'1', '2', '1'};
        assertArrayEquals(new char[]{'2', '1'}, ArrayKit.removeEle(chars, '1'));

        byte[] bytes = new byte[]{1, 2, 1};
        assertArrayEquals(new byte[]{2, 1}, ArrayKit.removeEle(bytes, (byte) 1));

        double[] doubles = new double[]{1, 2, 1};
        assertEquals(Arrays.toString(new double[]{2, 1}), Arrays.toString(ArrayKit.removeEle(doubles, 1)));

        float[] floats = new float[]{1f, 2f, 1f};
        assertEquals(Arrays.toString(new float[]{2f, 1f}), Arrays.toString(ArrayKit.removeEle(floats, 1f)));

        boolean[] booleans = new boolean[]{true, false, true};
        assertEquals(Arrays.toString(new boolean[]{false, true}), Arrays.toString(ArrayKit.removeEle(booleans, true)));

    }

    @Test
    public void testReverse() {
        // 反转数组，会变更原数组  startIndexInclusive 起始位置（包含） endIndexExclusive 结束位置（不包含）
        int[] ints = new int[]{1, 2, 3, 4};
        int[] ret = ArrayKit.reverse(ints, 1, 3);
        System.out.println(Arrays.toString(ret));
        assertEquals(Arrays.toString(new int[]{1, 3, 2, 4}), Arrays.toString(ret));

        final int[] ints1 = new int[]{2, 3, 4, 5};
        int[] ret1 = ArrayKit.reverse(ints1);
        assertEquals(Arrays.toString(new int[]{5, 4, 3, 2}), Arrays.toString(ret1));

        final long[] longs = new long[]{2L, 3L, 4L, 5L};
        long[] ret2 = ArrayKit.reverse(longs);
        assertEquals(Arrays.toString(new long[]{5L, 4L, 3L, 2L}), Arrays.toString(ret2));

        final long[] longs1 = new long[]{1L, 2L, 3L, 4L};
        long[] ret3 = ArrayKit.reverse(longs1, 1, 3);
        assertEquals(Arrays.toString(new int[]{1, 3, 2, 4}), Arrays.toString(ret3));

        final short[] shorts = new short[]{2, 3, 4, 5};
        short[] ret4 = ArrayKit.reverse(shorts);
        assertEquals(Arrays.toString(new short[]{5, 4, 3, 2}), Arrays.toString(ret4));

        final short[] shorts1 = new short[]{1, 2, 3, 4};
        short[] ret5 = ArrayKit.reverse(shorts1, 1, 3);
        assertEquals(Arrays.toString(new short[]{1, 3, 2, 4}), Arrays.toString(ret5));

        final char[] chars = new char[]{'2', '3', '4', '5'};
        char[] ret6 = ArrayKit.reverse(chars);
        assertEquals(Arrays.toString(new char[]{'5', '4', '3', '2'}), Arrays.toString(ret6));

        final char[] chars1 = new char[]{'1', '2', '3', '4'};
        char[] ret7 = ArrayKit.reverse(chars1, 1, 3);
        assertEquals(Arrays.toString(new char[]{'1', '3', '2', '4'}), Arrays.toString(ret7));

        final byte[] bytes = new byte[]{2, 3, 4, 5};
        byte[] ret8 = ArrayKit.reverse(bytes);
        assertEquals(Arrays.toString(new byte[]{5, 4, 3, 2}), Arrays.toString(ret8));

        final byte[] bytes1 = new byte[]{1, 2, 3, 4};
        byte[] ret9 = ArrayKit.reverse(bytes1, 1, 3);
        assertEquals(Arrays.toString(new byte[]{1, 3, 2, 4}), Arrays.toString(ret9));

        final double[] doubles = new double[]{2, 3, 4, 5};
        double[] ret10 = ArrayKit.reverse(doubles);
        assertEquals(Arrays.toString(new double[]{5, 4, 3, 2}), Arrays.toString(ret10));

        final double[] doubles1 = new double[]{1, 2, 3, 4};
        double[] ret11 = ArrayKit.reverse(doubles1, 1, 3);
        assertEquals(Arrays.toString(new double[]{1, 3, 2, 4}), Arrays.toString(ret11));

        final float[] floats = new float[]{2, 3, 4, 5};
        float[] ret12 = ArrayKit.reverse(floats);
        assertEquals(Arrays.toString(new float[]{5, 4, 3, 2}), Arrays.toString(ret12));

        final float[] floats1 = new float[]{1, 2, 3, 4};
        float[] ret13 = ArrayKit.reverse(floats1, 1, 3);
        assertEquals(Arrays.toString(new float[]{1, 3, 2, 4}), Arrays.toString(ret13));


        final boolean[] booleans = new boolean[]{true, false};
        boolean[] ret14 = ArrayKit.reverse(booleans);
        assertEquals(Arrays.toString(new boolean[]{false, true}), Arrays.toString(ret14));

        final boolean[] booleans1 = new boolean[]{true, false, true, false};
        boolean[] ret15 = ArrayKit.reverse(booleans1, 1, 3);
        assertEquals(Arrays.toString(new boolean[]{true, true, false, false}), Arrays.toString(ret15));

    }


    @Test
    public void testMin() {
        // 取最小值
        int[] ints = new int[]{1, 2, 3, 4};
        int ret = ArrayKit.min(ints);
        assertEquals(1, ret);

        long[] longs = new long[]{1L, 2L, 3L, 4L};
        long ret1 = ArrayKit.min(longs);
        assertEquals(1L, ret1);

        short[] shorts = new short[]{1, 2, 3, 4};
        short ret2 = ArrayKit.min(shorts);
        assertEquals(1, ret2);

        char[] chars = new char[]{'1', '2', '3', '4'};
        char ret4 = ArrayKit.min(chars);
        assertEquals('1', ret4);

        byte[] bytes = new byte[]{1, 2, 3, 4};
        byte ret5 = ArrayKit.min(bytes);
        assertEquals(1, ret5);

        double[] doubles = new double[]{1.0, 2.0, 3.0, 4.0};
        double ret6 = ArrayKit.min(doubles);
        assertEquals(1.0, ret6, 0);

        float[] floats = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        float ret7 = ArrayKit.min(floats);
        assertEquals(1.0, ret7, 0);
    }


    @Test
    public void testMax() {
        // 取最大值
        int[] ints = new int[]{1, 2, 3, 4};
        int ret = ArrayKit.max(ints);
        assertEquals(4, ret);

        long[] longs = new long[]{1L, 2L, 3L, 4L};
        long ret1 = ArrayKit.max(longs);
        assertEquals(4L, ret1);

        short[] shorts = new short[]{1, 2, 3, 4};
        short ret2 = ArrayKit.max(shorts);
        assertEquals(4, ret2);

        char[] chars = new char[]{'1', '2', '3', '4'};
        char ret4 = ArrayKit.max(chars);
        assertEquals('4', ret4);

        byte[] bytes = new byte[]{1, 2, 3, 4};
        byte ret5 = ArrayKit.max(bytes);
        assertEquals(4, ret5);

        double[] doubles = new double[]{1.0, 2.0, 3.0, 4.0};
        double ret6 = ArrayKit.max(doubles);
        assertEquals(4.0, ret6, 0);

        float[] floats = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        float ret7 = ArrayKit.max(floats);
        assertEquals(4.0f, ret7, 0);
    }

    @Test
    public void testSwap() {
        // 交换数组中两个位置的值
        int[] ints = new int[]{1, 2, 3, 4};
        assertEquals(Arrays.toString(new int[]{1, 3, 2, 4}), Arrays.toString(ArrayKit.swap(ints, 1, 2)));
        long[] longs = new long[]{1L, 2L, 3L, 4L};
        assertEquals(Arrays.toString(new long[]{1L, 3L, 2L, 4L}), Arrays.toString(ArrayKit.swap(longs, 1, 2)));
        double[] doubles = new double[]{1.0, 2.0, 3.0, 4.0};
        assertEquals(Arrays.toString(new double[]{1.0, 3.0, 2.0, 4.0}), Arrays.toString(ArrayKit.swap(doubles, 1, 2)));
        float[] floats = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        assertEquals(Arrays.toString(new float[]{1.0f, 3.0f, 2.0f, 4.0f}), Arrays.toString(ArrayKit.swap(floats, 1, 2)));
        boolean[] booleans = new boolean[]{true, false, false, true};
        assertEquals(Arrays.toString(new boolean[]{true, false, true, false}), Arrays.toString(ArrayKit.swap(booleans, 2, 3)));
        byte[] bytes = new byte[]{1, 2, 3, 4};
        assertEquals(Arrays.toString(new byte[]{1, 3, 2, 4}), Arrays.toString(ArrayKit.swap(bytes, 1, 2)));
        short[] shorts = new short[]{1, 2, 3, 4};
        assertEquals(Arrays.toString(new short[]{1, 3, 2, 4}), Arrays.toString(ArrayKit.swap(shorts, 1, 2)));

    }

    @Test
    public void testFindOne(){
        //查找第一个匹配元素对象  返回满足过滤条件的第一个元素
        String[] strings = new String[]{"4", "2", "3"};
        String result = ArrayKit.findOne(strings, new Filter<String>() {
            public boolean accept(String s) {
                if (s == null || !"3".equals(s)) {
                    return false;
                }
                return true;
            }
        });
        assertEquals("3",result);

        Integer[] integers = new Integer[]{5, 55, 555,5555};
        Integer result1 = ArrayKit.findOne(integers, new Filter<Integer>() {
            public boolean accept(Integer i) {
                if ( i < 500) {
                    return false;
                }
                return true;
            }
        });
        assertEquals((Integer) 555,result1);

    }

    @Test
    public void testFind(){
        //查找第一个匹配元素对象  返回满足过滤条件的第一个元素 返回类型为Optional<T>
        String[] strings = new String[]{"4", "2", "3"};
        Optional result = ArrayKit.find(strings, new Filter<String>() {
            public boolean accept(String s) {
                if (s == null || !"3".equals(s)) {
                    return false;
                }
                return true;
            }
        });
        assertEquals("3",result.get());

        Integer[] integers = new Integer[]{5, 55, 555,5555};
        Optional result1 = ArrayKit.find(integers, new Filter<Integer>() {
            public boolean accept(Integer i) {
                if ( i < 500) {
                    return false;
                }
                return true;
            }
        });
        assertEquals((Integer) 555,result1.get());

    }


}