package com.blazebit.security.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author Christian Beikov <c.beikov@curecomp.com>
 * @company curecomp
 * @date 05.12.2013
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface BeforeDatabaseAware {

    String unitName() default "TestPU";
    
}