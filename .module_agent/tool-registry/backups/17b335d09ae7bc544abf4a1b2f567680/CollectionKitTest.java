package com.datanew.core.toolkit;

import com.datanew.core.lang.Editor;
import com.datanew.core.lang.Filter;
import com.datanew.core.lang.Function;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CollectionKitTest {

    @Test
    public void testUnion() {
        // 两个集合的并集
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        List<Integer> list1 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3));
        List<Integer> list2 = new ArrayList<Integer>(Arrays.asList(4, 5, 6));

        Collection<Integer> ret = CollectionKit.union(list, list1);
        Collection<Integer> trueRet = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        assertEquals(ret, trueRet);

        Collection<Integer> ret1 = CollectionKit.union(list, list1, list2);
        Collection<Integer> trueRet1 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3, 4, 5, 6));
        assertEquals(ret1, trueRet1);
    }

    @Test
    public void testIntersection() {
        // 两个集合的交集
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        List<Integer> list1 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3));
        List<Integer> list2 = new ArrayList<Integer>(Arrays.asList(4, 5, 6));

        Collection<Integer> ret = CollectionKit.intersection(list, list1);
        Collection<Integer> trueRet = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3));
        assertEquals(ret, trueRet);

        Collection<Integer> ret1 = CollectionKit.intersection(list, list1, list2);
        Collection<Integer> trueRet1 = new ArrayList<Integer>();
        assertEquals(ret1, trueRet1);
    }


    @Test
    public void disjunction() {
        // 两个集合的差集
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        List<Integer> list1 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3));
        List<Integer> list2 = new ArrayList<Integer>(Arrays.asList(4, 5, 6));
        Collection<Integer> ret = CollectionKit.disjunction(list, list1);
        Collection<Integer> trueRet = new ArrayList<Integer>(Arrays.asList(3));
        assertEquals(ret, trueRet);
        // 任意一个集合为空，返回另一个集合
        Collection<Integer> ret1 = CollectionKit.disjunction(list, null);
        assertEquals(ret1, list);
        // 两个集合无交集则返回两个集合的组合
        Collection<Integer> ret2 = CollectionKit.disjunction(list, list2);
        Collection<Integer> trueRet1 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3, 4, 5, 6));
        assertEquals(ret2, trueRet1);

    }

    @Test
    public void contains() {
        // 判断指定集合是否包含指定值，如果集合为空（null或者空），返回false，否则找到元素返回true
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        List<Integer> list1 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3));
        List<Integer> list2 = new ArrayList<Integer>(Arrays.asList(4, 5, 6));
        assertTrue(CollectionKit.contains(list, 3));
        assertFalse(CollectionKit.contains(list1, 4));
        assertFalse(CollectionKit.contains(list2, null));
        assertFalse(CollectionKit.contains(null, 1));

    }

    @Test
    public void containsAny() {
        //其中一个集合在另一个集合中是否至少包含一个元素，既是两个集合是否至少有一个共同的元素
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        List<Integer> list1 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3));
        List<Integer> list2 = new ArrayList<Integer>(Arrays.asList(4, 5, 6));
        assertTrue(CollectionKit.containsAny(list, list1));
        assertFalse(CollectionKit.containsAny(list, list2));
    }

    @Test
    public void countMap() {
        // 根据集合返回一个元素计数的Map
        // 所谓元素计数就是假如这个集合中某个元素出现了n次，那将这个元素做为key，n做为value
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        Map<Integer, Integer> ret = CollectionKit.countMap(list);
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(1, 1);
        map.put(2, 1);
        map.put(3, 3);
        assertEquals(ret, map);

    }

    @Test
    public void testJoin() {
        // 以 conjunction 为分隔符将集合转换为字符串
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 3, 3));
        String ret = CollectionKit.join(list, "+");
        assertEquals(ret, "1+2+3+3+3");

        Set set = new HashSet() {{
            add("one");
            add("two");
        }};
        String ret1 = CollectionKit.join(set, "-");
        assertEquals(ret1, "one-two");

    }


    @Test
    public void popPart() {
        // 切取部分数据
        // 切取后的栈将减少这些元素 surplusAlaDatas 原数据  partSize 每部分数据的长度
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4);
        stack.push(5);
        List<Integer> ret = CollectionKit.popPart(stack, 3);
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(5, 4,3));
        assertEquals(ret, list);
        Deque<Integer> deque = new LinkedList<Integer>();
        deque.add(1);
        deque.add(2);
        deque.add(3);
        deque.add(4);
        deque.add(5);
        deque.add(6);
        List<Integer> ret1 = CollectionKit.popPart(deque, 3);
        List<Integer> list1 = new ArrayList<Integer>(Arrays.asList(1, 2,3));
        assertEquals(ret1, list1);
    }

    @Test
    public void testNewHashMap() {
        //新建一个HashMap
        HashMap<String, String> hashMap = CollectionKit.newHashMap();
        assertNotNull(hashMap);

        //size 初始大小，由于默认负载因子0.75，传入的size会实际初始大小为size / 0.75  isOrder Map的Key是否有序
        HashMap<String, String> hashMap1 = CollectionKit.newHashMap(3, false);
        assertNotNull(hashMap1);

        HashMap<String, String> hashMap2 = CollectionKit.newHashMap(3);
        assertNotNull(hashMap2);
    }

    @Test
    public void testNewHashSet() {
        //ts  元素数组
        HashSet<String> hashSet = CollectionKit.newHashSet("1", "2", "4", "3");
        assertNotNull(hashSet);
        Set set = new HashSet() {{
            add("one");
            add("two");
        }};
        HashSet<String> hashSet1 = CollectionKit.newHashSet(set);
        System.out.println(hashSet1);
        assertNotNull(hashSet1);

        List<String> lst = new ArrayList<String>();
        lst.add("aaa");
        lst.add("bbb");
        lst.add("ccc");
        lst.add("ddd");
        lst.add("eee");
        lst.add("fff");
        Iterator<String> iterator = lst.iterator();
        HashSet<String> hashSet2 = CollectionKit.newHashSet(true, iterator);
        System.out.println(hashSet2);
        assertNotNull(hashSet2);

        Enumeration<String> days;
        Vector<String> dayNames = new Vector<String>();
        dayNames.add("Sunday");
        dayNames.add("Monday");
        dayNames.add("Tuesday");
        dayNames.add("Wednesday");
        dayNames.add("Thursday");
        dayNames.add("Friday");
        dayNames.add("Saturday");
        days = dayNames.elements();
        HashSet<String> hashSet3 = CollectionKit.newHashSet(true, days);
        System.out.println(hashSet3);
        assertNotNull(hashSet3);


    }

    @Test
    //新建一个LinkedHashSet  ts  元素数组
    public void newLinkedHashSet() {
        LinkedHashSet<String> linkedHashSet = CollectionKit.newLinkedHashSet("1", "2", "4", "3");
        System.out.println(linkedHashSet);
        assertNotNull(linkedHashSet);
    }


    @Test
    public void testList() {
        //新建一个List   isLinked 是否新建LinkedList   values 数组
        List<String> list = CollectionKit.list(true);
        assertNotNull(list);
        List<String> list1 = CollectionKit.list(true, "2", "1", "3");
        assertNotNull(list1);
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add("two");
        }};
        List<String> list2 = CollectionKit.list(true, set);
        assertNotNull(list2);
        List<String> lst = new ArrayList<String>();
        lst.add("aaa");
        lst.add("bbb");
        Iterator<String> iterator = lst.iterator();
        List<String> list3 = CollectionKit.list(true, iterator);
        assertNotNull(list3);

        Enumeration<String> days;
        Vector<String> dayNames = new Vector<String>();
        dayNames.add("Sunday");
        dayNames.add("Monday");
        dayNames.add("Tuesday");
        dayNames.add("Wednesday");
        dayNames.add("Thursday");
        dayNames.add("Friday");
        dayNames.add("Saturday");
        days = dayNames.elements();
        List<String> list4 = CollectionKit.list(true, days);
        System.out.println(list4);
        assertNotNull(list4);
    }


    @Test
    public void testNewArrayList() {
        //新建一个ArrayList  values 数组
        ArrayList<String> arrayList = CollectionKit.newArrayList("1", "2");
        assertNotNull(arrayList);
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add("two");
        }};
        ArrayList<String> arrayList1 = CollectionKit.newArrayList(set);
        ArrayList<String> ret = new ArrayList<String>();
        ret.add("one");
        ret.add("two");
        assertEquals(arrayList1, ret);

        List<String> lst = new ArrayList<String>();
        lst.add("aaa");
        lst.add("bbb");
        Iterator<String> iterator = lst.iterator();
        ArrayList<String> arrayList2 = CollectionKit.newArrayList(iterator);
        assertNotNull(arrayList2);

        Enumeration<String> days;
        Vector<String> dayNames = new Vector<String>();
        dayNames.add("Sunday");
        dayNames.add("Monday");
        dayNames.add("Tuesday");
        dayNames.add("Wednesday");
        dayNames.add("Thursday");
        dayNames.add("Friday");
        dayNames.add("Saturday");
        days = dayNames.elements();
        ArrayList<String> list4 = CollectionKit.newArrayList(days);
        System.out.println(list4);
        assertNotNull(list4);
    }

    @Test
    public void toList() {
        //数组转为ArrayList
        ArrayList<String> arrayList = CollectionKit.toList("1", "2");
        assertNotNull(arrayList);
    }

    @Test
    public void newLinkedList() {
        //新建LinkedList
        LinkedList<String> linkedList = CollectionKit.newLinkedList("1", "2");
        assertNotNull(linkedList);
    }

    @Test
    public void newCopyOnWriteArrayList() {
        //新建一个CopyOnWriteArrayList
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add("two");
        }};
        CopyOnWriteArrayList<String> copyOnWriteArrayList = CollectionKit.newCopyOnWriteArrayList(set);
        assertNotNull(copyOnWriteArrayList);
    }

    @Test
    public void newBlockingQueue() {
        // 在队列为空时，获取元素的线程会等待队列变为非空。当队列满时，存储元素的线程会等待队列可用
        BlockingQueue<String> copyOnWriteArrayList = CollectionKit.newBlockingQueue(10, true);
        assertEquals(copyOnWriteArrayList.getClass(), LinkedBlockingDeque.class);
        BlockingQueue<String> copyOnWriteArrayList1 = CollectionKit.newBlockingQueue(10, false);
        assertEquals(copyOnWriteArrayList1.getClass(), ArrayBlockingQueue.class);
    }

    @Test
    public void create() {
        // 创建新的集合对象
        Collection set = CollectionKit.create(HashSet.class);
        assertEquals(set.getClass(), HashSet.class);

        Collection list = CollectionKit.create(List.class);
        assertEquals(list.getClass(), ArrayList.class);
    }

    @Test
    public void createMap() {
        // 创建Map  mapType map类型
        Map<String, String> map = CollectionKit.createMap(HashMap.class);
        assertEquals(map.getClass(), HashMap.class);
    }

    @Test
    public void distinct() {
        // 去重集合
        ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        ArrayList<String> ret = CollectionKit.distinct(arrayList);
        assertEquals(ret, CollectionKit.toList("1", "2", "3"));
    }

    @Test
    public void testSub() {
        // 截取集合的部分  list 被截取的数组  start 开始位置（包含） end 结束位置（不包含）
        ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        List<String> ret = CollectionKit.sub(arrayList, 1, 4);
        assertEquals(ret, CollectionKit.toList("2", "2", "3"));
        // 截取集合的部分  list 被截取的数组  start 开始位置（包含） end 结束位置（不包含） step 步进
        List<String> ret1 = CollectionKit.sub(arrayList, 1, 4, 2);
        assertEquals(ret1, CollectionKit.toList("2", "3"));
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add("two");
            add("three");
            add("four");
            add("five");
        }};
        List<String> ret2 = CollectionKit.sub(set, 1, 4);
        assertEquals(ret2, CollectionKit.toList("one", "two", "three"));
        List<String> ret3 = CollectionKit.sub(set, 1, 4, 2);
        assertEquals(ret3, CollectionKit.toList("one", "three"));
    }

    @Test
    public void split() {
        // 对集合按照指定长度分段，每一个段为单独的集合，返回这个集合的列表
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add("two");
            add("three");
            add("four");
            add("five");
        }};
        List<List<String>> ret = CollectionKit.split(set, 2);
        System.out.println(ret);
        assertNotNull(ret);
    }

    @Test
    public void filter() {
        // 过滤过程通过传入的Editor实现来返回需要的元素内容
        //这个Editor实现可以实现以下功能：
        // 1、过滤出需要的对象，如果返回null表示这个元素对象抛弃 2、修改元素对象，返回集合中为修改后的对象
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add("two");
            add("three");
            add("four");
            add("five");
        }};
        //collection 集合   editor 编辑器接口
        final Collection<String> ret = CollectionKit.filter(set, new Editor<String>() {
            @Override
            public String edit(String s) {
                System.out.println(s);
                if (s.equals("one")) {
                    s = "1";
                    return s;
                }
                return s;
            }
        });
        System.out.println(ret);
        assertNotNull(ret);

        List<String> lst = new ArrayList<String>();
        lst.add("aaa");
        lst.add("bbb");
        lst.add("ccc");
        //list  集合   editor 编辑器接口
        final Collection<String> ret1 = CollectionKit.filter(lst, new Editor<String>() {
            @Override
            public String edit(String s) {
                System.out.println(s);
                if (s.equals("aaa")) {
                    return null;
                }
                return s;
            }
        });
        System.out.println(ret1);
        assertNotNull(ret);
        //collection 集合   filter 过滤器
        final Collection<String> ret2 = CollectionKit.filter(set, new Filter<String>() {
            @Override
            public boolean accept(String s) {
                if (s.equals("aaa")) {
                    return false;
                }
                return true;
            }
        });
        System.out.println(ret2);
        assertNotNull(ret2);
        //list  集合  filter 过滤器
        final Collection<String> ret3 = CollectionKit.filter(lst, new Filter<String>() {
            @Override
            public boolean accept(String s) {
                return false;
            }
        });
        System.out.println(ret3);
        assertNotNull(ret3);

        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        //map  Map   filter 编辑器接口
        Map<String, Integer> ret4 = CollectionKit.filter(hashMap, new Filter<Map.Entry<String, Integer>>() {
            @Override
            public boolean accept(Map.Entry<String, Integer> stringIntegerEntry) {
                if (stringIntegerEntry.getKey() == "a") {
                    return false;
                }
                return true;
            }
        });
        System.out.println(ret4.entrySet());
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        hashMap1.put("b", 2);
        assertEquals(ret4.entrySet(), hashMap1.entrySet());

        HashMap<String, Integer> hashMap2 = new HashMap<String, Integer>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        //map  Map    editor 编辑器接口
        Map<String, Integer> ret5 = CollectionKit.filter(hashMap2, new Editor<Map.Entry<String, Integer>>() {
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
        HashMap<String, Integer> hashMap3 = new HashMap<String, Integer>();
        hashMap1.put("b", 2);

        assertEquals(ret5.entrySet(), hashMap3.entrySet());
    }

    @Test
    public void removeNull() {
        // 去除 null 元素
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add(null);
            add("three");
            add("four");
            add("five");
        }};
        Collection<String> ret = CollectionKit.removeNull(set);
        HashSet<String> set1 = new HashSet<String>() {{
            add("one");
            add("three");
            add("four");
            add("five");
        }};
        assertEquals(ret, set1);
    }

    @Test
    public void removeAny() {
        // 去掉集合中的多个元素
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add("two");
            add("three");
            add("four");
            add("five");
        }};
        Collection<String> ret = CollectionKit.removeAny(set, "one", "two");
        HashSet<String> set1 = new HashSet<String>() {{

            add("three");
            add("four");
            add("five");
        }};
        assertEquals(ret, set1);
    }

    @Test
    public void removeEmpty() {
        //去除null或者"" 元素
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add(null);
            add("");
            add("four");
            add("five");
        }};
        Collection<String> ret = CollectionKit.removeEmpty(set);
        HashSet<String> set1 = new HashSet<String>() {{
            add("one");
            add("four");
            add("five");
        }};
        assertEquals(ret, set1);
    }

    @Test
    public void removeBlank() {
        //去除null或者""或者空白字符串 元素
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add(null);
            add("");
            add(" ");
            add("five");
        }};
        Collection<String> ret = CollectionKit.removeBlank(set);
        HashSet<String> set1 = new HashSet<String>() {{
            add("one");
            add("five");
        }};
        assertEquals(ret, set1);
    }

    @Test
    public void extract() {
        // 通过Editor抽取集合元素中的某些值返回为新列表
        ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        List<Object> ret = CollectionKit.extract(arrayList, new Editor<Object>() {
            @Override
            public Object edit(Object o) {
                if (o.equals("2")) {
                    return null;
                }
                return o;
            }

        });
        assertEquals(ret, CollectionKit.toList("1", null, null, "3", "3"));
    }

    @Test
    public void getFieldValues() {
        //  获取给定Bean列表中指定字段名对应字段值的列
        List<Person> lst = new ArrayList<Person>();
        Person person = new Person("xxx", 1);
        Person person2 = new Person("xx", 2);
        lst.add(person);
        lst.add(person2);
        List<Object> list = CollectionKit.getFieldValues(lst, "userName");
        System.out.println(list);
        assertNotNull(list);
    }

    @Test
    public void findOne() {
        // 查找第一个匹配元素对象
        final ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        String string = CollectionKit.findOne(arrayList, new Filter<String>() {
            @Override
            public boolean accept(String s) {
                return arrayList.indexOf(s) == 3;
            }
        });
        assertEquals(string, "3");
    }

    @Test
    public void findOneByField() {
        // 查找第一个匹配元素对象
        // 如果集合元素是Map，则比对键和值是否相同，相同则返回
        // 如果为普通Bean，则通过反射比对元素字段名对应的字段值是否相同，相同则返回
        // 如果给定字段值参数是null 且元素对象中的字段值也为null 则认为相同
        List<Person> lst = new ArrayList<Person>();
        Person person = new Person("xxx", 1);
        Person person2 = new Person("xx", 2);
        lst.add(person);
        lst.add(person2);
        Person ret = CollectionKit.findOneByField(lst, "userName", "xxx");
        System.out.println(ret.getUserName());
        assertEquals(ret, person);
    }

    @Test
    public void count() {
        //集合中匹配规则的数量
        // TODO Matcher的用法不清楚
        HashSet<String> set = new HashSet<String>() {{
            add("one");
            add(null);
            add("");
            add("four");
            add("five");
        }};
//        int cnt = CollectionKit.count(set,Matcher.);
    }

    @Test
    public void testIsEmpty() {
        List<Person> lst = new ArrayList<Person>();
        // 集合是否为空
        assertTrue(CollectionKit.isEmpty(lst));
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        // Map是否为空
        assertTrue(CollectionKit.isEmpty(hashMap));
        Iterator iterator = lst.iterator();
        // Iterator是否为空
        assertTrue(CollectionKit.isEmpty(iterator));
        Enumeration<String> days = null;
        assertTrue(CollectionKit.isEmpty(days));
    }

    @Test
    public void testIsNotEmpty() {
        List<Person> lst = new ArrayList<Person>();
        // 集合是否为非空
        assertFalse(CollectionKit.isNotEmpty(lst));
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        // Map是否为非空
        assertFalse(CollectionKit.isNotEmpty(hashMap));
        Iterator iterator = lst.iterator();
        // Iterator是否为空
        assertFalse(CollectionKit.isNotEmpty(iterator));
        Enumeration<String> days = null;
        assertFalse(CollectionKit.isNotEmpty(days));
    }

    @Test
    public void hasNull() {
        // 是否包含null元素
        List<Person> lst = new ArrayList<Person>();
        lst.add(null);

        assertTrue(CollectionKit.hasNull(lst));

        Set hashset = new HashSet();
        hashset.add(null);

        assertTrue(CollectionKit.hasNull(hashset));
    }

    @Test
    public void testZip() {
        // 映射键值（参考Python的zip()函数)
        // keys 键列表  values 值列表  delimiter 分隔符  isOrder 是否有序
        String keyList = "1,2,3,4,5";
        String valueList = "one,two,three,four,five";
        Map<String, String> map = CollectionKit.zip(keyList, valueList, ",", true);

        assertEquals("one", map.get("1"));
        assertEquals("five", map.get("5"));
    }

    @Test
    public void testToMap() {
        // 将数组转换为Map（HashMap） array 数组 元素类型为Map.Entry、数组、Iterable、Iterator
        Map<Object, Object> map = CollectionKit.toMap(new String[][]{
                {"RED", "#FF0000"}, {"GREEN", "#00FF00"}, {"BLUE", "#0000FF"}});
        System.out.println(map);
        assertNotNull(map);


    }

    @Test
    public void toTreeSet() {
        // 将集合转换为排序后的TreeSet
        HashSet<String> set = new HashSet<String>() {{
            add("1");
            add("2");
            add("3");
            add("4");
            add("5");
        }};
        TreeSet<String> treeSet = CollectionKit.toTreeSet(set, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {

                return o1.compareTo(o2);
            }
        });
        System.out.println(treeSet);
        assertNotNull(treeSet);
    }

    @Test
    public void asEnumeration() {
        // Iterator转换为Enumeration
        ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        Enumeration<String> ret = CollectionKit.asEnumeration(arrayList.iterator());
        System.out.println(ret);
        assertNotNull(ret);
    }

    @Test
    public void asIterator() {
        // Enumeration转换为Iterator
        Enumeration<String> days = null;
        Iterator<String> ret = CollectionKit.asIterator(days);
        System.out.println(ret);
        assertNotNull(ret);
    }

    @Test
    public void asIterable() {
        // Iterator 转为 Iterable
        ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        Iterator<String> iterator = arrayList.iterator();
        assertNotNull(CollectionKit.asIterable(iterator));
    }

    @Test
    public void toCollection() {
        //  Iterable转为Collection
        ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        assertNotNull(CollectionKit.toCollection(arrayList));
    }

    @Test
    public void toListMap() {
        // 行转列，合并相同的键，值合并为列表 将Map列表中相同key的值组成列表做为Map的value
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
        //mapList Map列表
        Map<String, List<Integer>> listMap = CollectionKit.toListMap(mapList);
        System.out.println(listMap);
    }

    @Test
    public void toMapList() {
        // 列转行。将Map中值列表分别按照其位置与key组成新的map
        HashMap<String, List<Integer>> hashMap = new HashMap<String, List<Integer>>();
        hashMap.put("a", new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4)));
        hashMap.put("b", new ArrayList<Integer>(Arrays.asList(1, 2, 3)));
        hashMap.put("c", new ArrayList<Integer>(Arrays.asList(1)));
        System.out.println(hashMap);
        List<Map<String, Integer>> mapList = CollectionKit.toMapList(hashMap);
        System.out.println(mapList);
        assertNotNull(mapList);
    }

    @Test
    public void addAll() {
        // 加入全部  collection 被加入的集合  values 要加入的内容数组
        List<String> list = new ArrayList<String>();
        Collection<String> stringCollection = CollectionKit.addAll(list, new String[]{"1", "2"});
        System.out.println(stringCollection);
        assertEquals(stringCollection, CollectionKit.toList("1", "2"));


    }

    @Test
    public void testAddAll() {
        // 将指定对象全部加入到集合中
        // 提供的对象如果为集合类型，会自动转换为目标元素类型
        // collection 被加入的集合  value 对象，可能为Iterator、Iterable、Enumeration、Array
        List<Integer> list = new ArrayList<Integer>();
        Collection<Integer> stringCollection = CollectionKit.addAll(list, 1);
        System.out.println(stringCollection);
        assertEquals(stringCollection, CollectionKit.toList(1));

        List<Object> list1 = new ArrayList<>();
        Person p = new Person("ABB",25);
        Collection<Object> stringCollection1 = CollectionKit.addAll(list1, p);
        System.out.println(stringCollection1);

    }

    @Test
    public void testAddAll1() {
        // 将指定对象全部加入到集合中  collection 被加入的集合 value 对象  elementType 元素类型，为空时，使用Object类型来接纳所有类型
        List<String> list = new ArrayList<String>();
        Collection<String> stringCollection = CollectionKit.addAll(list, "hahahha", String.class);
        System.out.println(stringCollection);
        assertEquals(stringCollection, CollectionKit.toList("hahahha"));
    }

    @Test
    public void testAddAll2() {
        // 加入全部  collection 被加入的集合  iterator 要加入的Iterator
        List<String> list = new ArrayList<String>();
        ArrayList<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        Iterator<String> iterator = arrayList.iterator();
        Collection<String> stringCollection = CollectionKit.addAll(list, iterator);
        System.out.println(stringCollection);
        assertEquals(stringCollection, CollectionKit.toList("1", "2", "2", "3", "3"));
    }

    @Test
    public void testAddAll3() {
        // 加入全部  collection 被加入的集合  iterable 要加入的内容
        List<String> list = new ArrayList<String>();
        List<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        Collection<String> stringCollection = CollectionKit.addAll(list, arrayList);
        System.out.println(stringCollection);
        assertEquals(stringCollection, CollectionKit.toList("1", "2", "2", "3", "3"));
    }

    @Test
    public void testAddAll4() {
        // 加入全部  collection  被加入的集合  enumeration 要加入的内容
        Enumeration<String> days;
        Vector<String> dayNames = new Vector<String>();
        dayNames.add("Sunday");
        dayNames.add("Monday");
        dayNames.add("Tuesday");
        dayNames.add("Wednesday");
        dayNames.add("Thursday");
        dayNames.add("Friday");
        dayNames.add("Saturday");
        days = dayNames.elements();
        List<String> list = new ArrayList<String>();
        Collection<String> stringCollection = CollectionKit.addAll(list, days);
        assertEquals(stringCollection, CollectionKit.toList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));
    }

    @Test
    public void addAllIfNotContains() {
        // 将另一个列表中的元素加入到列表中，如果列表中已经存在此元素则忽略之
        List<String> list = new ArrayList<String>();
        list.add("1");
        List<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        List<String> ret = CollectionKit.addAllIfNotContains(list, arrayList);
        assertEquals(ret, CollectionKit.toList("1", "2", "3"));
    }

    @Test
    public void get() {
        // 获取集合中指定下标的元素值，下标可以为负数，例如-1表示最后一个元素 如果元素越界，返回null
        List<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        String ret = CollectionKit.get(arrayList, 1);
        assertEquals(ret, "2");

        String ret1 = CollectionKit.get(arrayList, -1);
        assertEquals(ret1,"3");
    }

    @Test
    public void getAny() {
        // 获取集合中指定多个下标的元素值，下标可以为负数，例如-1表示最后一个元素
        List<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        List<String> ret = CollectionKit.getAny(arrayList, 1, 2, 3);
        assertEquals(ret, CollectionKit.toList("2", "2", "3"));

        List<String> ret1 = CollectionKit.getAny(arrayList, -1,2,4);
        assertEquals(ret1,CollectionKit.toList("3","2","3"));
    }

    @Test
    public void testGetFirst() {
        // 获取集合的第一个元素
        List<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        String ret = CollectionKit.getFirst(arrayList);
        assertEquals(ret, "1");

        Iterator<String> iterator = arrayList.iterator();
        String ret1 = CollectionKit.getFirst(iterator);
        assertEquals(ret1, "1");
    }

    @Test
    public void getLast() {
        // 获取集合的最后一个元素
        List<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        String ret = CollectionKit.getLast(arrayList);
        assertEquals(ret, "3");
    }

    @Test
    public void testGetElementType() {
        // 获得Iterable对象的元素类型（通过第一个非空元素判断）
        List<String> arrayList = CollectionKit.toList("1", "2", "2", "3", "3");
        Class ret = CollectionKit.getElementType(arrayList);
        assertEquals(ret, String.class);

        Iterator<String> iterator = arrayList.iterator();
        Class ret1 = CollectionKit.getElementType(iterator);
        assertEquals(ret1, String.class);
    }

    @Test
    public void testValuesOfKeys() {
        // 从Map中获取指定键列表对应的值列表
        // 如果key在map中不存在或key对应值为null，则返回值列表对应位置的值也为null
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        map.put("key1", "value1");
        ArrayList<String> arrayList = CollectionKit.valuesOfKeys(map, "key", "key1");
        assertEquals(arrayList, CollectionKit.toList("value", "value1"));

        List<String> list = CollectionKit.toList("key", "key1");
        Iterator<String> iterator = list.iterator();
        ArrayList<String> arrayLis1 = CollectionKit.valuesOfKeys(map, iterator);
        assertEquals(arrayLis1, CollectionKit.toList("value", "value1"));

        List<String> list1 = CollectionKit.toList("key", "key1");
        ArrayList<String> arrayList2 = CollectionKit.valuesOfKeys(map, list1);
        assertEquals(arrayList2, CollectionKit.toList("value", "value1"));
    }


    @Test
    public void sortPageAll() {
        // 将多个集合排序并显示不同的段落（分页）
        // TODO
    }

    @Test
    public void page() {
        // 对指定List分页取值 pageNo 页码，从1开始计数，0和1效果相同  pageSize 每页的条目数  list 列表
        ArrayList<String> arrayList1 = CollectionKit.toList("1", "2", "3", "4");
        List<String> list = CollectionKit.page(1, 2, arrayList1);
        System.out.println(list);
        assertEquals(list, CollectionKit.toList("1", "2"));
    }

    @Test
    public void testSort() {
        // 针对List排序，排序会修改原List  list 被排序的List  c Comparator
        ArrayList<String> arrayList1 = CollectionKit.toList("1", "4", "3", "2");
        List<String> list = CollectionKit.sort(arrayList1, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {

                return o1.compareTo(o2);
            }
        });
        System.out.println(list);
        assertEquals(list, CollectionKit.toList("1", "2", "3", "4"));

        HashSet<String> set = new HashSet<String>() {{
            add("3");
            add("2");
            add("1");
            add("4");
            add("5");
        }};
        // 排序集合，排序不会修改原集合  collection 集合  comparator 比较器
        List<String> list1 = CollectionKit.sort(set, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {

                return o1.compareTo(o2);
            }
        });
        System.out.println(list1);
        assertEquals(list1, CollectionKit.toList("1", "2", "3", "4", "5"));

        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("3", "2");
        hashMap.put("1", "3");
        // 排序Map  map Map  comparator Entry比较器
        TreeMap<String, String> treeMap = CollectionKit.sort(hashMap, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        System.out.println(treeMap);
        assertNotNull(treeMap);
    }

    @Test
    public void testSortByProperty() {
        // 根据Bean的属性排序  list  List  property 属性名
        Person person1 = new Person("abc", 1);
        Person person2 = new Person("ABC", 2);
        List<Person> list = new ArrayList<Person>();
        list.add(person1);
        list.add(person2);

        List<Person> list1 = CollectionKit.sortByProperty(list, "userName");
        System.out.println(list1);
        assertNotNull(list1);
    }

    @Test
    public void testSortByPinyin() {
        // 根据汉字的拼音顺序排序
        List<String> list = new ArrayList<String>();
        list.add("张三");
        list.add("李四");

        List<String> list1 = CollectionKit.sortByPinyin(list);
        System.out.println(list1);
        assertNotNull(list1);
    }

    @Test
    public void sortToMap() {
        // 通过Entry排序，可以按照键排序，也可以按照值排序，亦或者两者综合排序
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("userName", "age");
        hashMap.put("a", "b");
        // entryCollection Entry集合   comparator比较器
        LinkedHashMap<String, String> linkedHashMap = CollectionKit.sortToMap(hashMap.entrySet(), new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                String key = o1.getKey();
                String key2 = o2.getKey();
                return key.compareTo(key2);
            }
        });
        System.out.println(linkedHashMap);
        assertNotNull(linkedHashMap);
    }

    @Test
    public void sortByEntry() {
        // 通过Entry排序，可以按照键排序，也可以按照值排序，亦或者两者综合排序
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("userName", "age");
        hashMap.put("a", "b");
        // map 被排序的Map    comparator比较器
        LinkedHashMap<String, String> linkedHashMap = CollectionKit.sortByEntry(hashMap, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                String key = o1.getKey();
                String key2 = o2.getKey();
                return key.compareTo(key2);
            }
        });
        System.out.println(linkedHashMap);
        assertNotNull(linkedHashMap);
    }

    @Test
    public void sortEntryToList() {
        // 将Set排序（根据Entry的值）
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("userName", "age");
        hashMap.put("a", "b");
        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>();
        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            list.add(entry);
        }
        List<Map.Entry<String, String>> list1 = CollectionKit.sortEntryToList(list);
        System.out.println(list1);
        assertNotNull(list1);
    }

    @Test
    public void forEach() {
        // 循环遍历 iterator，使用Consumer接受遍历的每条数据，并针对每条数据做处理
        ArrayList<String> arrayList1 = CollectionKit.toList("1", "2", "3", "4");
        Iterator iterator = arrayList1.iterator();
        // iterator迭代器  consumer 遍历的每条数据处理器
        CollectionKit.forEach(iterator, new CollectionKit.Consumer<Object>() {
            @Override
            //接受并处理一个参数  value 参数值   index 参数在集合中的索引
            public void accept(Object value, int index) {
                System.out.println(value);
            }
        });
    }

    @Test
    public void testForEach() {
        // 循环遍历 Enumeration，使用Consumer接受遍历的每条数据，并针对每条数据做处理
        Enumeration<String> days;
        Vector<String> dayNames = new Vector<String>();
        dayNames.add("Sunday");
        dayNames.add("Monday");
        dayNames.add("Tuesday");
        dayNames.add("Wednesday");
        dayNames.add("Thursday");
        dayNames.add("Friday");
        dayNames.add("Saturday");
        days = dayNames.elements();
        // enumeration Enumeration   consumer 遍历的每条数据处理器
        CollectionKit.forEach(days, new CollectionKit.Consumer<String>() {
            @Override
            public void accept(String value, int index) {
                System.out.println(value);
                assertNotNull(value);
            }
        });
    }

    @Test
    public void testForEach1() {
        // 循环遍历Map，使用KVConsumer接受遍历的每条数据，并针对每条数据做处理
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("userName", "age");
        hashMap.put("a", "b");
        // map Map   kvConsumer  遍历的每条数据处理器
        CollectionKit.forEach(hashMap, new CollectionKit.KVConsumer<String, String>() {
            @Override
            public void accept(String key, String value, int index) {
                if (key.equals("userName")) {
                    value = "xxx";
                }
                System.out.println(key);
                System.out.println(value);
            }
        });

    }

    @Test
    public void transform() {
        //将K类型转变为V类型
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        List<Person> personList = CollectionKit.transform(list, new Function<String, Person>() {

            @Override
            public Person call(String parameters) {
                return new Person(parameters, 30);
            }
        });

        for (int i = 0; i <personList.size(); i++) {
            System.out.println(personList.get(i).toString());
        }
    }

    @Test
    public void group() {
        // 分组，按照Hash接口定义的hash算法，集合中的元素放入hash值对应的子列表中
        ArrayList<String> arrayList1 = CollectionKit.toList("1", "2", "2", "4", "3", "4");
        // collection 被分组的集合  hash Hash值算法，决定元素放在第几个分组的规则
        List<List<String>> lists = CollectionKit.group(arrayList1, new CollectionKit.Hash<String>() {
            @Override
            public int hash(String s) {
                return StringKit.isEmpty(s) ? 0 : Integer.valueOf(s);
            }
        });
        assertNull(lists.get(0));
        assertEquals(1, lists.get(1).size());
        assertEquals(2, lists.get(2).size());
    }

    @Test
    public void groupByField() {
        // 根据元素的指定字段名分组，非Bean都放在第一个分组中
        Person person1 = new Person("abc", 1);
        Person person2 = new Person("ABC", 2);
        List<Person> list = new ArrayList<Person>();
        list.add(person1);
        list.add(person2);
        List<List<Person>> lists = CollectionKit.groupByField(list, "userName");
        System.out.println(lists);
        assertNotNull(lists);
    }

    @Test
    public void reverse() {
        // 反序给定List，会在原List基础上直接修改
        ArrayList<String> arrayList1 = CollectionKit.toList("1", "2", "2", "4", "3", "4");
        List<String> ret = CollectionKit.reverse(arrayList1);
        System.out.println(ret);
        assertEquals(ret, arrayList1);
    }

    @Test
    public void reverseNew() {
        // 反序给定List，会创建一个新的List，原List数据不变
        ArrayList<String> arrayList1 = CollectionKit.toList("1", "2", "2", "4", "3", "4");
        List<String> ret = CollectionKit.reverseNew(arrayList1);
        System.out.println(ret);
        assertEquals(ret, CollectionKit.toList("4", "3", "4", "2", "2", "1"));
        assertEquals(arrayList1, CollectionKit.toList("1", "2", "2", "4", "3", "4"));
    }

    @Test
    public void setOrAppend() {
        // 设置或增加元素。当index小于List的长度时，替换指定位置的值，否则在尾部追加
        ArrayList<String> arrayList1 = CollectionKit.toList("1", "2", "2", "4", "3", "4");
        List<String> ret = CollectionKit.setOrAppend(arrayList1, 2, "hahha");
        List<String> ret1 = CollectionKit.setOrAppend(ret, 6, "lalala");
        assertEquals(ret1, CollectionKit.toList("1", "2", "hahha", "4", "3", "4", "lalala"));

        List<Object> collect = CollectionKit.stream(null).filter(o -> o != null).collect(Collectors.toList());
        System.out.println(collect);
    }

    @Test
    public void testCloneListByCloneable(){
        // 克隆list对象
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");
        List<String> cloneList = CollectionKit.cloneByCloneable(list);
        assertEquals(list,cloneList);
    }


    @Test
    public void testCloneSetByCloneable(){
        // 克隆Set对象
        Set<String> hashSet = new HashSet<>();
        hashSet.add("a");
        hashSet.add("b");
        hashSet.add("c");

        Set<String> cloneSet = CollectionKit.cloneByCloneable(hashSet);
        assertEquals(hashSet,cloneSet);
    }

}