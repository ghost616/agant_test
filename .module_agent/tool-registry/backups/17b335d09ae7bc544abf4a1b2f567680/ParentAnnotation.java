package com.datanew.core.toolkit;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE,ElementType.METHOD})
@Documented
@Inherited  //可以继承
public  @interface ParentAnnotation {
}



