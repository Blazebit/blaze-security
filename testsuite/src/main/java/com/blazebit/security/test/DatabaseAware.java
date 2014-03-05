package com.blazebit.security.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If annotated on class level, every test method will get a fresh database.
 * On method level this annotation will only affect the respective test method.
 * The behavior on class level can be overridden by using {@link DatabaseUnaware} on specific methods.
 * The test class must use the {@link ContainerRunner} to be able to make use of this feature.
 * 
 * @author Christian Beikov <c.beikov@curecomp.com>
 * @company curecomp
 * @date 05.12.2013
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DatabaseAware {

    String unitName() default "TestPU";
    
}