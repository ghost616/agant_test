package com.datanew.core.toolkit;

import java.lang.annotation.*;

@Target(value={ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubAnnotation {
}
