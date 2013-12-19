package com.blazebit.security.auth.impl;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Target({ METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface LoginModuleName {

}
