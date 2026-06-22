package com.datanew.core.toolkit;

import org.junit.Test;

import java.util.*;

public class AssertKitTest {

    @Test
    public void state() {
        // 检测状态值是否为true，不为true则抛出指定错误
        try {
            AssertKit.state(true, "IllegalStateException");
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Test
    public void isTrue() {
        // 检测expression是否为true，不为true则抛出指定错误
        try {
            AssertKit.isTrue(true, "IllegalStateException");
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Test
    public void testIsNull() {
        // 检测对象是否为空
        try {
            AssertKit.isNull(null, "IllegalStateException");
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Test
    public void testNotNull() {
        // 检测对象是否为非空
        try {
            AssertKit.notNull("a", "IllegalStateException");
        } catch (IllegalStateException e) {
            throw e;
        }
    }


    @Test
    public void hasLength() {
        // 检测文本是否有内容
        try {
            AssertKit.hasLength("abc", "IllegalStateException");
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Test
    public void hasText() {
        // 检测文本是否有内容
        try {
            AssertKit.hasText("哈哈哈哈", "IllegalStateException");
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Test
    public void testNotEmpty() {
        // 检测数组是否为空
        String[] strings = new String[]{"1"};
        try {
            AssertKit.notEmpty(strings, "strings is empty");
        } catch (IllegalStateException e) {
            throw e;
        }
        Collection<Integer> collection = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
        try {
            AssertKit.notEmpty(collection, "collection is empty");
        } catch (IllegalStateException e) {
            throw e;
        }
        Map<String, String> myMap = new HashMap<String, String>();
        myMap.put("key", "value");
        try {
            AssertKit.notEmpty(myMap, "Map is empty");
        } catch (IllegalStateException e) {
            throw e;
        }

    }

    @Test
    public void noNullElements() {
        // 检测数组是否包含空对象
        String[] strings = new String[]{"1", "2"};
        try {
            AssertKit.noNullElements(strings, "strings has null element");
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @Test
    public void testIsInstanceOf() {
        // 检测对象是否属于某个类型
        String a = "a";
        try {
            AssertKit.isInstanceOf(String.class, a, "类型不匹配");
        } catch (IllegalStateException e) {
            throw e;
        }
        try {
            AssertKit.isInstanceOf(String.class, a);
        } catch (IllegalStateException e) {
            throw e;
        }
    }


    @Test
    public void testIsAssignable() {
        // 检测两个类型是否父子关系，不是则抛出错误
        String a = "a";
        try {
            AssertKit.isAssignable(Object.class, a.getClass(), "类型不匹配");
        } catch (IllegalStateException e) {
            throw e;
        }
        try {
            AssertKit.isAssignable(String.class, a.getClass());
        } catch (IllegalStateException e) {
            throw e;
        }
    }

}