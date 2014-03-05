/**
 * 
 */
package com.blazebit.security.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.ejb.EJBException;

import com.blazebit.exception.ExceptionUtils;
import com.googlecode.catchexception.throwable.internal.ThrowableProcessingInterceptor;

/**
 * @author Christian Beikov <c.beikov@curecomp.com>
 * @company curecomp
 * @date 05.12.2013
 */
public class EjbAwareThrowableProcessingInterceptor<E extends Throwable> extends ThrowableProcessingInterceptor<E> {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Throwable>[] UNWRAPS = (Class<? extends Throwable>[]) new Class<?>[]{InvocationTargetException.class, EJBException.class};
    
    /**
     * @param target
     * @param clazz
     * @param assertThrowable
     */
    public EjbAwareThrowableProcessingInterceptor(Object target, Class<E> clazz) {
        super(target, clazz, true);
    }

    @Override
    protected Object afterInvocationThrowsThrowable(Throwable e, Method method) throws Error, Throwable {
        return super.afterInvocationThrowsThrowable(ExceptionUtils.unwrap(e, UNWRAPS), method);
    }

}
