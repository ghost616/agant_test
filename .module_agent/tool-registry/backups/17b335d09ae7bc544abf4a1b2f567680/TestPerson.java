package com.datanew.core.toolkit;

import java.util.Comparator;


/**
 * @Author 姚惠栋
 * @Date 2020/9/29 9:30
 * @Description
 **/

public class TestPerson implements Comparator<TestPerson> {
    private String name;
    private Integer age;

    public TestPerson(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public TestPerson() {

    }

    public String getName() {
        return this.name;
    }

    public int getAge() {
        return this.age;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TestPerson) {
            TestPerson p = (TestPerson) o;
            return this.name.equals(p.name) && this.age == p.age;
        } else
            return false;
    }

    @Override
    public String toString() {
        return name + "(" + age.toString() + ")";
    }

    @Override
    public int compare(TestPerson p1, TestPerson p2) {

        return p1.getName().compareTo(p2.getName());
    }

}
