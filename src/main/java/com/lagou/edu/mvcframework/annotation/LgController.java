package com.lagou.edu.mvcframework.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface LgController {
    String value() default "";
}
