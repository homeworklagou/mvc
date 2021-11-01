package com.lagou.edu.mvcframework.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE,ElementType.METHOD})
public @interface LgRequestMapping {
    String value() default "";
}
