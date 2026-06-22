package com.datanew.core.toolkit;


import com.datanew.core.annotation.Toolkit;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.*;


public class AnnotationKitTest {

    @Test
    public void testGetAnnotation(){

        // 获取指定类�的annotation对象
        AnnotatedElement myAnnotatedElement = Sub.class;
        Class<SubAnnotation> annotationType = SubAnnotation.class;
        Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
        SubAnnotation annotation = AnnotationKit.getAnnotation(myAnnotatedElement, annotationType);
        ParentAnnotation annotation1 = AnnotationKit.getAnnotation(myAnnotatedElement, annotationType1);
        assertNotNull(annotation);
        assertNotNull(annotation1);
    }

    @Test
    public void testGetAnnotation1(){

        // �;�指定方法获取指定annotation,为空或者出错则返回null
        try {
            Method method = Sub.class.getMethod("test",null);
            Class<SubAnnotation> annotationType = SubAnnotation.class;
            Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
            SubAnnotation annotation = AnnotationKit.getAnnotation(method, annotationType);
            ParentAnnotation annotation1 = AnnotationKit.getAnnotation(method, annotationType1);
            assertNotNull(annotation);
            assertNull(annotation1);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetAnnotations(){

        // 通过AnnotatedElement获取注解列�!�
        AnnotatedElement myAnnotatedElement = Sub.class;
        Annotation[] annotations = AnnotationKit.getAnnotations(myAnnotatedElement);
        if (annotations == null){
            System.out.println("Annotation is null");
        }
        for (Annotation annotation : annotations) {
            System.out.println(annotation);
        }

    }

    @Test
    public void testGetAnnotations1() {

        // 获取指定�9法上的注解
        Method[] methods = Sub.class.getMethods();
        for (Method method : methods) {
            if(method.getName().equals("test")){
                Annotation[] annotations = AnnotationKit.getAnnotations(method);
                if (annotations == null){
                    System.out.println("Annotation is null");
                }
                for (Annotation annotation : annotations) {
                    System.out.println(annotation);
                }
            }
        }

    }



    @Test
    public void findAnnotation() {

        // 在Element上根据element对象,注�#类型找到指定类型的注�'�对象
        AnnotatedElement myAnnotatedElement = Sub.class;
        Class<SubAnnotation> annotationType = SubAnnotation.class;
        Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
        SubAnnotation annotation = AnnotationKit.findAnnotation(myAnnotatedElement, annotationType);
        ParentAnnotation annotation1 = AnnotationKit.findAnnotation(myAnnotatedElement, annotationType1);
        assertNotNull(annotation);
        assertNull(annotation1);
    }

    @Test
    public void findAnnotation1() {

       // 获取方法中�'行Annotation，如果找不到o��会尝试查找接口上的Annotation
        try {
            Method method = Sub.class.getMethod("test",null);
            Class<SubAnnotation> annotationType = SubAnnotation.class;
            Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
            SubAnnotation annotation = AnnotationKit.findAnnotation(method, annotationType);
            ParentAnnotation annotation1 = AnnotationKit.findAnnotation(method, annotationType1);
            assertNotNull(annotation);
            assertNotNull(annotation1);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void isInterfaceWithAnnotatedMethods() {

        // 判定类�方法里面是否带有注解
        boolean b = AnnotationKit.isInterfaceWithAnnotatedMethods(Sub.class);
        assertTrue(b);
    }

    @Test
    public void findAnnotation2() {

        // 查找Class上指�.�类型的Annotation对象
        Class<Sub> subClass = Sub.class;
        Class<SubAnnotation> annotationType = SubAnnotation.class;
        Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
        SubAnnotation annotation = AnnotationKit.findAnnotation(subClass, annotationType);
        ParentAnnotation annotation1 = AnnotationKit.findAnnotation(subClass, annotationType1);
        assertNotNull(annotation);
        assertNotNull(annotation1);
    }

    @Test
    public void findAnnotationDeclaringClass() {

        // 通过Class类型e��Annotation类型查找到Annotation标记所在的真实Class
        Class<SubAnnotation> annotationType = SubAnnotation.class;
        Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
        Class<Sub> subClass = Sub.class;
        Class<?> annotationDeclaringClass = AnnotationKit.findAnnotationDeclaringClass(annotationType, subClass);
        Class<?> annotationDeclaringClass1 = AnnotationKit.findAnnotationDeclaringClass(annotationType1, subClass);
        assertNotNull(annotationDeclaringClass);
        assertNotNull(annotationDeclaringClass1);
    }

    @Test
    public void findAnnotationDeclaringClassForTypes() {

        // 通过Class类型和多个Annotation类型查找到Annotationf��记所在的真实Class
        // 只要有任意一个Annotation类型符合,则立刻返��
        List<Class<? extends Annotation>> annotationTypes = new ArrayList<>();
        annotationTypes.add(SubAnnotation.class);
        annotationTypes.add(ParentAnnotation.class);
        annotationTypes.add(Toolkit.class);
        Class<Sub> subClass = Sub.class;
        Class<?> annotationDeclaringClass = AnnotationKit.findAnnotationDeclaringClassForTypes(annotationTypes, subClass);
        assertNotNull(annotationDeclaringClass);
    }

    @Test
    public void isAnnotationDeclaredLocally() {

        // 在Class上��否包含指定的Annotation�;型
        Class<SubAnnotation> annotationType = SubAnnotation.class;
        Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
        Class<Sub> subClass = Sub.class;
        assertTrue(AnnotationKit.isAnnotationDeclaredLocally(annotationType,subClass));
        assertFalse(AnnotationKit.isAnnotationDeclaredLocally(annotationType1,subClass));
    }

    @Test
    public void isAnnotationInherited() {

        // ��定的AnnotationType是否是��含在指定Class中（不是��接打在当前class，可能��打在父类上）
        Class<SubAnnotation> annotationType = SubAnnotation.class;
        Class<ParentAnnotation> annotationType1 = ParentAnnotation.class;
        Class<Sub> subClass = Sub.class;
        assertFalse(AnnotationKit.isAnnotationInherited(annotationType, subClass));
        assertTrue(AnnotationKit.isAnnotationInherited(annotationType1, subClass));
    }

    @Test
    public void isAnnotationMetaPresent() {
        // todo
        // 在指定��Annotation中是否包含指�的MetaAnnotationType

    }

    @Test
    public void isInJavaLangAnnotationPackage() {

        // 当前Annotation�/否是java系统的Annotation  annotation 注解类型
        Annotation a = new Annotation() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Retention.class;
            }
        };

        Annotation a1 = new Annotation() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ParentAnnotation.class;
            }
        };

       assertFalse(AnnotationKit.isInJavaLangAnnotationPackage(a1));

    }

    @Test
    public void isInJavaLangAnnotationPackage1() {

        // 当前Annotation是否是java系统的Annotation  annotationType 注解类�的Class
        assertTrue(AnnotationKit.isInJavaLangAnnotationPackage(Retention.class));
        assertFalse(AnnotationKit.isInJavaLangAnnotationPackage(ParentAnnotation.class));

    }

    @Test
    public void isInJavaLangAnnotationPackage2() {

        // 当前Annotationf��否是java系统的Annotation   annotationType 注解类名�0
        String annotationType ="java.lang.annotation.Retention";
        assertTrue(AnnotationKit.isInJavaLangAnnotationPackage(annotationType));

    }
}