package com.datanew.core.toolkit;

import java.io.Serializable;

/**
 * @Author 姚惠栋
 * @Date 2020/9/28 14:41
 * @Description
 **/

public class Person implements Serializable {
    String userName;
    int age;

    public Person() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Person(String userName, int age) {

        this.userName = userName;
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Person{" +
                "userName='" + userName + '\'' +
                ", age=" + age +
                '}';
    }
}
