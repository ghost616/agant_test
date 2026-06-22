package com.datanew.core.toolkit;

import com.datanew.core.bean.BeanDesc;
import com.datanew.core.bean.CopyOptions;
import com.datanew.core.bean.ValueProvider;
import com.datanew.core.lang.Editor;
import org.junit.Test;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BeanKitTest {

    @Test
    public void isBean() {
        // 判断是否为Bean对象<br>
        // 判定方法是是否存在只有一个参数的setXXX方法
        assertTrue(BeanKit.isBean(Person.class));
    }

    @Test
    public void hasSetter() {
        // 判断是否有Setter方法
        // 判定方法是是否存在只有一个参数的setXXX方法
        assertTrue(BeanKit.hasSetter(Person.class));
    }

    @Test
    public void hasGetter() {
        // 判断是否有Getter方法
        // 判定方法是是否存在只有一个参数的getXXX方法
        assertTrue(BeanKit.hasGetter(Person.class));
    }

    @Test
    public void createDynaBean() {
        // 创建动态Bean   bean 普通Bean或Map
        assertNotNull(BeanKit.createDynaBean(new Person("xxx", 1)));
    }

    @Test
    public void findEditor() {
        // 查找类型转换器
            assertNotNull(BeanKit.findEditor(String.class));
            assertNotNull(BeanKit.findEditor(Integer.class));
    }

    @Test
    public void hasNull() {
        // 判断Bean中是否有值为null的字段
        assertFalse(BeanKit.hasNull(new Person("xxx", 1)));
        assertTrue(BeanKit.hasNull(new Person(null, 1)));
    }

    @Test
    public void getBeanDesc() {
        // 获取 Bean描述信息
        BeanDesc ret = BeanKit.getBeanDesc(Person.class);
        assertNotNull(ret.getField("userName"));
    }

    @Test
    public void getPropertyDescriptors() {
        // 获得Bean字段描述数组
        PropertyDescriptor[] ret = BeanKit.getPropertyDescriptors(Person.class);
        for (PropertyDescriptor propertyDescriptor : ret) {
            System.out.println(propertyDescriptor);
            assertNotNull(propertyDescriptor);
        }

    }

    @Test
    public void getPropertyDescriptorMap() {
        // 获得字段名和字段描述Map，获得的结果会缓存在BeanInfoCache中
        Map<String, PropertyDescriptor> map = BeanKit.getPropertyDescriptorMap(Person.class, true);
        System.out.println(map.entrySet());
        assertNotNull(map);

    }

    @Test
    public void testGetPropertyDescriptor() {
        // 获得Bean类属性描述，忽略大小写
        PropertyDescriptor ret = BeanKit.getPropertyDescriptor(Person.class, "aGe", true);
        assertEquals(ret.getName(), "age");

        // 获得Bean类属性描述，大小写敏感
        PropertyDescriptor ret2 = BeanKit.getPropertyDescriptor(Person.class, "age");
        assertEquals(ret2.getName(), "age");


    }

    @Test
    public void getFieldValue() {
        // 获得字段值，通过反射直接获得字段值，并不调用getXXX方法  fieldNameOrIndex 字段名或序号，序号支持负数
        Object ret = BeanKit.getFieldValue(new Person("xxx", 1), "age");
        assertEquals(ret, 1);
        // 对象同样支持Map类型，fieldNameOrIndex即为key
        Map hashMap = new HashMap();
        hashMap.put("zhangsan",38);
        assertEquals(BeanKit.getFieldValue(hashMap,"zhangsan"),38);
    }

    @Test
    public void setFieldValue() {
        // 设置字段值，，通过反射设置字段值，并不调用setXXX方法
        Person person = new Person("xxx", 1);
        BeanKit.setFieldValue(person, "age", 18);
        Object ret = BeanKit.getFieldValue(person, "age");
        assertEquals(ret, 18);
        // 对象同样支持Map类型，fieldNameOrIndex即为key
        Map hashMap = new HashMap();
        hashMap.put("zhangsan",38);
        BeanKit.setFieldValue(hashMap,"zhangsan",48);
        assertEquals(BeanKit.getFieldValue(hashMap,"zhangsan"),48);

    }

    @Test
    public void getProperty() {
        // 解析Bean中的属性值
        Person person = new Person("xxx", 1);
        Object ret = BeanKit.getProperty(person, "person.age");
        assertEquals(ret, 1);

        Map hashMap = new HashMap();
        hashMap.put("zhangsan",38);
        assertEquals(38,BeanKit.getProperty(hashMap, "zhangsan"));

    }

    @Test
    public void setProperty() {
        //设置Bean中的属性值
        Person person = new Person("xxx", 1);
        BeanKit.setProperty(person, "person.age", 18);
        Object ret = BeanKit.getProperty(person, "person.age");
        System.out.println(ret);
        assertEquals(ret, 18);
    }

    @Test
    public void testMapToBean() {
        // Map转换为Bean对象  isIgnoreError 是否忽略注入错误
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("userName", "xxx");
        map.put("age", 1);
        Person person = BeanKit.mapToBean(map, Person.class, false);
        assertEquals(person.getUserName(), "xxx");

        Person person1 = BeanKit.mapToBean(map, Person.class, new CopyOptions().setEditable(Serializable.class));
        assertNotNull(person1);
    }

    @Test
    public void mapToBeanIgnoreCase() {
        // Map转换为Bean对象 忽略大小写
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("USerName", "xxx");
        map.put("aGE", 1);
        Person person = BeanKit.mapToBeanIgnoreCase(map, Person.class, true);
        assertEquals(person.getUserName(), "xxx");
    }

    @Test
    public void testFillBeanWithMap() {
        // 使用Map填充Bean对象
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("userName", "xxx");
        map.put("age", 1);
        Person person = new Person();
        Person ret = BeanKit.fillBeanWithMap(map, person, false);
        assertEquals(ret.getUserName(), "xxx");

        HashMap<String, Object> map1 = new HashMap<String, Object>();
        map1.put("user_name", "username");
        map1.put("age", 1);
        Person person1 = new Person();
        // isToCamelCase 是否将下划线模式转换为驼峰模式
        Person ret1 = BeanKit.fillBeanWithMap(map1, person1, true, false);
        System.out.println(ret1.getAge());
        assertEquals(ret1.getUserName(), "username");


        Person p = new Person();
        // copyOptions 属性复制选项
        Person person2 = BeanKit.fillBeanWithMap(map, p, new CopyOptions().setEditable(Serializable.class));
        assertNotNull(person2);

        Person p1 = new Person();
        Person person3 = BeanKit.fillBeanWithMap(map, p1, true, new CopyOptions().setEditable(Serializable.class));
        assertNotNull(person3);
    }

    @Test
    public void fillBeanWithMapIgnoreCase() {
        //使用Map填充Bean对象，忽略大小写
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("uSeRnAmE", "xxx");
        map.put("AgE", 1);
        Person person = new Person();
        Person ret = BeanKit.fillBeanWithMapIgnoreCase(map, person, true);
        assertEquals(ret.getUserName(), "xxx");
    }

    @Test
    public void testToBean() {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("userName", "xxx");
        map.put("age", 1);
        // 对象或Map转Bean
        Person person = BeanKit.toBean(map, Person.class);
        assertEquals(person.getUserName(), "xxx");

        // ServletRequest 参数转Bean  valueProvider 值提供者

        Person person1 = BeanKit.toBean(Person.class, new ValueProvider<String>() {
            @Override
            public Object value(String key, Type valueType) {
                return map.get(key);
            }

            @Override
            public boolean containsKey(String key) {
                return false;
            }
        }, new CopyOptions());

        assertNotNull(person1);
    }

    @Test
    public void fillBean() {
        // 填充Bean的核心方法
        final HashMap<String, Object> map = new HashMap<String, Object>();
        Person person1 = BeanKit.toBean(Person.class, new ValueProvider<String>() {
            @Override
            public Object value(String key, Type valueType) {

                return map.get(key);
            }

            @Override
            public boolean containsKey(String key) {
                return false;
            }
        }, new CopyOptions());

        assertNotNull(person1);
    }

    @Test
    public void testBeanToMap() {
        // 对象转Map，不进行驼峰转下划线，不忽略值为空的字段
        Person person = new Person("xx x", 1);
        Map<String, Object> map = BeanKit.beanToMap(person);
        System.out.println(map);
        assertNotNull(map);

        // 对象转Map  isToUnderlineCase 是否转换为下划线模式  ignoreNullValue 是否忽略值为空的字段
        Person person1 = new Person(null, 1);
        Map<String, Object> map1 = BeanKit.beanToMap(person1, true, true);
        System.out.println(map1);
        assertNotNull(map1);

        // 对象转Map targetMap 目标的Map isToUnderlineCase 是否转换为下划线模式 ignoreNullValue 是否忽略值为空的字段
        Person person2 = new Person("userName", 1);
        final Map<String, Object> targetMap = new HashMap<String, Object>();
        Map<String, Object> map2 = BeanKit.beanToMap(person2, targetMap, true, false);
        assertNotNull(map2);

        // 对象转Map  bean bean对象 targetMap 目标的Map ignoreNullValue 是否忽略值为空的字段 keyEditor 属性字段（Map的key）编辑器，用于筛选、编辑key
        Person person3 = new Person("xxxx", 100);
        final Map<String, Object> targetMap1 = new HashMap<String, Object>();
        Map<String, Object> map3 = BeanKit.beanToMap(person3, targetMap1, false, new Editor<String>() {
            @Override
            public String edit(String s) {
                System.out.println(s);
                if (s.equals("userName")) {
                    return s;
                }
                if (s.equals("age")) {
                    return null;
                }
                return null;
            }
        });
        System.out.println(map3);
        assertNotNull(map3);
    }

    @Test
    public void testCopyProperties() {
        // 复制Bean对象属性  source 源Bean对象  target 目标Bean对象
        Man man = new Man();
        BeanKit.copyProperties(new Person("xxxx", 123), man);
        assertEquals(man.userName,"xxxx");
        assertEquals(man.age,123);

        // 复制Bean对象属性   source 源Bean对象  target 目标Bean对象 ignoreProperties 不拷贝的的属性列表
        Man man1 = new Man();
        BeanKit.copyProperties(new Person("xxxx", 123), man1, "userName");
        assertNull(man1.userName);

        // 复制Bean对象属性   source 源Bean对象  target 目标Bean对象  ignoreCase 是否忽略大小写
        Man man2 = new Man();
        BeanKit.copyProperties(new Person("xxxx", 123), man2, true);
        assertEquals(man2.getUserName(),"xxxx");
    }


    @Test
    public void isMatchName() {
        // 给定的Bean的类名是否匹配指定类名字符串
        // bean Bean  beanClassName Bean的类名  isSimple 是否只匹配类名而忽略包名，true表示忽略包名
        assertTrue(BeanKit.isMatchName(new Person(), "com.datanew.core.toolkit.Person", false));
        assertTrue(BeanKit.isMatchName(new Person(), "Person", true));

    }

    @Test
    public void trimStrFields() {
        // 把Bean里面的String属性做trim操作  ignoreFields 不需要trim的Field名称列表（不区分大小写）
        Person person = new Person(" xxx ", 1);
        assertEquals(BeanKit.trimStrFields(person, "age").getUserName(), "xxx");
    }

    @Test
    public void isEmpty() {
        // 判断Bean是否为空对象，空对象表示本身为null或者所有属性都为null
        Person person = null;
        assertTrue(BeanKit.isEmpty(person));
    }

    @Test
    public void hasNullField() {
        // 判断Bean是否包含值为null的属性
        // 对象本身为null也返回true
        Person person = new Person(null, 1);
        assertTrue(BeanKit.hasNullField(person));
    }
}