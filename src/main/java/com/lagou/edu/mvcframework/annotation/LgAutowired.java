package com.lagou.edu.mvcframework.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public @interface LgAutowired {
    String value() default "";
}
