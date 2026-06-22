package com.datanew.core.toolkit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @Author 姚惠栋
 * @Date 2020/9/29 8:43
 * @Description
 **/

public class SerializeDemo
{
    public static void main(String [] args)
    {
        Person e = new Person();
        e.userName = "Reyan Ali";
        e.age = 12;
        try
        {
            FileOutputStream fileOut =
                    new FileOutputStream("D:\\javafile\\person.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(e);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in D:\\javafile\\person.ser");
        }catch(IOException i)
        {
            i.printStackTrace();
        }
    }
}