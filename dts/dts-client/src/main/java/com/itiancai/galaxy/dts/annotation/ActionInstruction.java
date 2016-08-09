package com.itiancai.galaxy.dts.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ActionInstruction {
    String value() default "";
}
