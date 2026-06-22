package com.datanew.core.toolkit;

import com.datanew.core.lang.Editor;
import com.datanew.core.lang.Filter;
import com.datanew.core.map.MapBuilder;
import com.datanew.core.map.MapProxy;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MapKitTest {

    @Test
    public void isEmpty() {
        // Map是否为空
        Map<String, String> map = new HashMap<String, String>();
        assertTrue(MapKit.isEmpty(map));
        map.put("1", "2");
        assertFalse(MapKit.isEmpty(map));
    }

    @Test
    public void isNotEmpty() {
        // Map是否为非空
        Map<String, String> map = new HashMap<String, String>();
        assertFalse(MapKit.isNotEmpty(map));
        map.put("key", "value");
        assertTrue(MapKit.isNotEmpty(map));
    }

    @Test
    public void testNewHashMap() {
        // 新建一个HashMap
        HashMap<String, String> map = MapKit.newHashMap();
        assertEquals(HashMap.class, map.getClass());

        // size 初始大小 isOrder Map的Key是否有序，有序返回LinkedHashMap，否则返回HashMap
        HashMap<String, String> map1 = MapKit.newHashMap(3, true);
        assertEquals(LinkedHashMap.class, map1.getClass());

        HashMap<String, String> map2 = MapKit.newHashMap(3);
        assertEquals(HashMap.class, map2.getClass());

        HashMap<String, String> map3 = MapKit.newHashMap(true);
        assertEquals(LinkedHashMap.class, map3.getClass());

        HashMap<String, String> map4 = MapKit.newHashMap(false);
        assertEquals(HashMap.class, map4.getClass());
    }

    @Test
    public void testNewTreeMap() {
        // 新建一个TreeMap
        TreeMap<TestPerson, String> treeMap = MapKit.newTreeMap(new TestPerson());
        assertEquals(TreeMap.class, treeMap.getClass());

        HashMap<TestPerson, String> map = MapKit.newHashMap();
        TreeMap<TestPerson, String> treeMap2 = MapKit.newTreeMap(map, new TestPerson());
        assertEquals(TreeMap.class, treeMap2.getClass());
    }

    @Test
    public void createMap() {
        // 创建Map mapType map类型
        Map<Object, Object> treeMap = MapKit.createMap(TreeMap.class);
        assertNotNull(treeMap);
    }

    @Test
    public void testOf() {
        // 将单一键值对转换为Map
        HashMap<String, String> hashMap = MapKit.of("key", "value");
        System.out.println(hashMap);
        assertEquals(HashMap.class, hashMap.getClass());

        // 将单一键值对转换为Map  isOrder 是否有序
        HashMap<String, String> hashMap1 = MapKit.of("key1", "value1", true);
        System.out.println(hashMap1);
        assertEquals(LinkedHashMap.class, hashMap1.getClass());

        // 将数组转换为Map（HashMap），支持数组元素类型为：
        // array 数组。元素类型为Map.Entry、数组、Iterable、Iterator
        HashMap<Object, Object> hashMap2 = MapKit.of(new String[][]{
                {"RED", "#FF0000"},
                {"GREEN", "#00FF00"},
                {"BLUE", "#0000FF"}});
        System.out.println(hashMap2);
        assertEquals(HashMap.class, hashMap2.getClass());
    }

    @Test
    public void toListMap() {
        // 行转列，合并相同的键，值合并为列表
        // 将Map列表中相同key的值组成列表做为Map的value
        List<HashMap<String, Integer>> mapList = new ArrayList<HashMap<String, Integer>>() {
        };
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();
        HashMap<String, Integer> hashMap3 = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 1);
        hashMap.put("c", 1);
        hashMap1.put("a", 2);
        hashMap1.put("b", 2);
        hashMap2.put("a", 3);
        hashMap2.put("b", 3);
        hashMap3.put("a", 4);
        mapList.add(hashMap);
        mapList.add(hashMap1);
        mapList.add(hashMap2);
        mapList.add(hashMap3);
        System.out.println(mapList);
        Map<String, List<Integer>> listMap = MapKit.toListMap(mapList);
        System.out.println(listMap);
        assertNotNull(listMap);

        Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();
        map.put("a", Arrays.asList(1, 2, 3, 4));
        map.put("b", Arrays.asList(1, 2, 3));
        map.put("c", Arrays.asList(1));
        assertEquals(map, listMap);
    }

    @Test
    public void toMapList() {
        // 列转行。将Map中值列表分别按照其位置与key组成新的map
        HashMap<String, List<Integer>> hashMap = new HashMap<String, List<Integer>>();
        hashMap.put("a", new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4)));
        hashMap.put("b", new ArrayList<Integer>(Arrays.asList(1, 2, 3)));
        hashMap.put("c", new ArrayList<Integer>(Arrays.asList(1)));
        List<Map<String, Integer>> mapList = MapKit.toMapList(hashMap);
        System.out.println(mapList);
        assertNotNull(mapList);

        List<HashMap<String, Integer>> mapList1 = new ArrayList<HashMap<String, Integer>>() {
        };
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();
        HashMap<String, Integer> hashMap3 = new HashMap<String, Integer>();
        HashMap<String, Integer> hashMap4 = new HashMap<String, Integer>();
        hashMap1.put("a", 1);
        hashMap1.put("b", 1);
        hashMap1.put("c", 1);
        hashMap2.put("a", 2);
        hashMap2.put("b", 2);
        hashMap3.put("a", 3);
        hashMap3.put("b", 3);
        hashMap4.put("a", 4);
        mapList1.add(hashMap1);
        mapList1.add(hashMap2);
        mapList1.add(hashMap3);
        mapList1.add(hashMap4);
        assertEquals(mapList1, mapList);
    }

    @Test
    public void toCamelCaseMap() {
        // 将已知Map转换为key为驼峰风格的Map
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("user_name", "xxx");
        Map<String, String> hashMap1 = MapKit.toCamelCaseMap(hashMap);
        System.out.println(hashMap1.keySet());
        for (String key : hashMap1.keySet()) {
            assertEquals(key, "userName");
        }
    }

    @Test
    public void toObjectArray() {
        // 将键值对转换为二维数组，第一维是key，第二纬是value
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        hashMap.put("c", 3);
        Object[][] objects = MapKit.toObjectArray(hashMap);
        assertNotNull(objects);
    }

    @Test
    public void join() {
        // 将map转成字符串  separator entry之间的连接符  keyValueSeparator kv之间的连接符
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        hashMap.put("c", 3);
        String ret = MapKit.join(hashMap, ",", "=");
        assertEquals(ret, "a=1,b=2,c=3");
        String ret1 = MapKit.join(hashMap, "|", ":");
        assertEquals(ret1, "a:1|b:2|c:3");
    }

    @Test
    public void joinIgnoreNull() {
        // 将map转成字符串，忽略null的键和值
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        hashMap.put("c", null);
        String ret = MapKit.joinIgnoreNull(hashMap, ",", "=");
        assertEquals(ret, "a=1,b=2");
    }

    @Test
    public void testJoin() {
        // 将map转成字符串  isIgnoreNull 是否忽略null的键和值
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        hashMap.put("c", null);
        String ret = MapKit.join(hashMap, ",", "=", true);
        assertEquals(ret, "a=1,b=2");
        String ret1 = MapKit.join(hashMap, ",", "=", false);
        assertEquals(ret1, "a=1,b=2,c=null");
    }

    @Test
    public void filter() {
        // 过滤
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        Map<String, Integer> ret = MapKit.filter(hashMap, new Editor<Map.Entry<String, Integer>>() {
            @Override
            public Map.Entry<String, Integer> edit(Map.Entry<String, Integer> stringIntegerEntry) {
                String key = stringIntegerEntry.getKey();
                Integer value = stringIntegerEntry.getValue();

                if (value == 1) {
                    return null;
                }
                return stringIntegerEntry;
            }
        });
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        hashMap1.put("b", 2);

        assertEquals(ret.entrySet(), hashMap1.entrySet());

    }

    @Test
    public void testFilter() {
        // 过滤
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        Map<String, Integer> ret = MapKit.filter(hashMap, new Filter<Map.Entry<String, Integer>>() {
            @Override
            public boolean accept(Map.Entry<String, Integer> stringIntegerEntry) {
                if (stringIntegerEntry.getKey() == "a") {
                    return false;
                }
                return true;
            }
        });
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        hashMap1.put("b", 2);

        assertEquals(ret.entrySet(), hashMap1.entrySet());

    }

    @Test
    public void testFilter1() {
        // 过滤Map保留指定键值对，如果键不存在跳过
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        Map<String, Integer> ret = MapKit.filter(hashMap, "b", "c");
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        hashMap1.put("b", 2);

        assertEquals(ret.entrySet(), hashMap1.entrySet());

    }

    @Test
    public void reverse() {
        // Map的键和值互换
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        hashMap.put(4, 1);
        hashMap.put(5, 2);
        Map<Integer, Integer> ret = MapKit.reverse(hashMap);
        HashMap<Integer, Integer> hashMap1 = new HashMap<Integer, Integer>();
        hashMap1.put(1, 4);
        hashMap1.put(2, 5);
        assertEquals(ret, hashMap1);
    }

    @Test
    public void sort() {
        // 排序已有Map，Key有序的Map，使用默认Key排序方式（字母顺序）
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        hashMap.put(5, 2);
        hashMap.put(4, 1);

        Map<Integer, Integer> ret = MapKit.sort(hashMap);
        HashMap<Integer, Integer> hashMap1 = new HashMap<Integer, Integer>();
        hashMap1.put(4, 1);
        hashMap1.put(5, 2);
        assertEquals(ret, hashMap1);

        HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();
        hashMap2.put("e", 1);
        hashMap2.put("b", 2);
        hashMap2.put("a", 3);
        hashMap2.put("c", 4);
        hashMap2.put("d", 5);
        TreeMap<String, Integer> ret2 = MapKit.sort(hashMap2);

        HashMap<String, Integer> hashMap3 = new HashMap<String, Integer>();
        hashMap3.put("a", 3);
        hashMap3.put("b", 2);
        hashMap3.put("c", 4);
        hashMap3.put("d", 5);
        hashMap3.put("e", 1);
        assertEquals(ret2, hashMap3);

    }

    @Test
    public void testSort() {
        // 排序已有Map，Key有序的Map  comparator Key比较器
        TreeMap<Integer, Integer> treeMap = new TreeMap<Integer, Integer>();
        treeMap.put(4, 6);
        treeMap.put(5, 2);
        treeMap.put(3, 8);
        final Map<Integer, Integer> ret = MapKit.sort(treeMap, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (o1 < o2) {
                    return 1;
                }
                return 0;
            }
        });

        TreeMap<Integer, Integer> treeMap1 = new TreeMap<Integer, Integer>();
        treeMap1.put(4, 6);
        treeMap1.put(3, 8);
        treeMap1.put(5, 2);
        assertEquals(ret, treeMap1);

    }

    @Test
    public void createProxy() {
        // 创建代理Map
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        hashMap.put(5, 2);
        hashMap.put(4, 1);
        MapProxy ret = MapKit.createProxy(hashMap);
        assertNotNull(ret);
    }

    @Test
    public void testBuilder() {
        // 创建链接调用map
        MapBuilder<String, Integer> ret = MapKit.builder();

        assertEquals(ret.build().getClass(), HashMap.class);
        // map 实际使用的map
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        MapBuilder<Integer, Integer> ret1 = MapKit.builder(hashMap);
        assertEquals(ret1.build().getClass(), HashMap.class);
        // k  key   v  value
        MapBuilder<String, Integer> ret2 = MapKit.builder("key", 1);
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        hashMap1.put("key", 1);
        assertEquals(hashMap1.entrySet(), ret2.build().entrySet());

    }

    @Test
    public void getAny() {
        // 获取Map的部分key生成新的Map
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        Map<String, Integer> ret = MapKit.getAny(hashMap, "a", "c");
        HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();
        hashMap2.put("a", 1);
        assertEquals(ret.entrySet(), hashMap2.entrySet());
    }

    @Test
    public void getStr() {
        // 获取Map指定key的值，并转换为字符串
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        hashMap.put(5, 2);
        String ret = MapKit.getStr(hashMap, 5);
        assertEquals(ret, "2");
    }

    @Test
    public void getInt() {
        // 获取Map指定key的值，并转换为Integer
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        hashMap.put(5, "2");
        int ret = MapKit.getInt(hashMap, 5);
        assertEquals(ret, 2);
    }

    @Test
    public void getDouble() {
        // 获取Map指定key的值，并转换为Double
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        hashMap.put(5, "2.5");
        double ret = MapKit.getDouble(hashMap, 5);
        assertEquals(ret, 2.5, 1);
    }

    @Test
    public void getFloat() {
        // 获取Map指定key的值，并转换为Float
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        hashMap.put(5, "2.5");
        float ret = MapKit.getFloat(hashMap, 5);
        assertEquals(ret, 2.5f, 1);
    }

    @Test
    public void getShort() {
        // 获取Map指定key的值，并转换为Short
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        hashMap.put(5, "2");
        short ret = MapKit.getShort(hashMap, 5);
        assertEquals(ret, 2);
    }

    @Test
    public void getBool() {
        // 获取Map指定key的值，并转换为Bool
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        hashMap.put(5, 1);
        boolean ret = MapKit.getBool(hashMap, 5);
        assertTrue(ret);
    }

    @Test
    public void getChar() {
        // 获取Map指定key的值，并转换为Character
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        hashMap.put(5, 1);
        char ret = MapKit.getChar(hashMap, 5);
        assertEquals(ret, '1');
    }

    @Test
    public void getLong() {
        // 获取Map指定key的值，并转换为Long
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        hashMap.put(5, 1);
        long ret = MapKit.getLong(hashMap, 5);
        assertEquals(ret, 1L);
    }

    @Test
    public void getDate() {
        // 获取Map指定key的值，并转换为Date
        HashMap<Integer, String> hashMap = new HashMap<Integer, String>();
        hashMap.put(5, "2020-09-29");
        Date ret = MapKit.getDate(hashMap, 5);
        Date date = DateKit.parse("2020-09-29");
        assertEquals(ret, date);
    }

}