package com.blazebit.security.integration.util;

import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 * @author Christian Beikov
 */
public class ELUtils {

    private ELUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValueSimple(String expression, Class<T> expectedType, Map<String, Object> variables) {
        ExpressionFactory ef = ExpressionFactory.newInstance();
        ELContext elContext = new SimpleContext(variables, ef, new SimpleELResolver());
        return (T) ef.createValueExpression(elContext, "#{" + expression + "}", expectedType).getValue(elContext);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValue(String expression, Class<T> expectedType, Map<String, Object> variables) {
        ExpressionFactory ef = ExpressionFactory.newInstance();
        ELContext elContext = new SimpleContext(variables, ef, new FullELResolver());
        return (T) ef.createValueExpression(elContext, "#{" + expression + "}", expectedType).getValue(elContext);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValue(String expression, Class<T> expectedType, Map<String, Object> variables, ELResolver elResolver) {
        ExpressionFactory ef = ExpressionFactory.newInstance();
        ELContext elContext = new SimpleContext(variables, ef, elResolver);
        return (T) ef.createValueExpression(elContext, "#{" + expression + "}", expectedType).getValue(elContext);
    }

    private static class SimpleContext extends ELContext {

        private final VariableMapper variables = new VariableMapper() {

            Map<String, ValueExpression> map = new HashMap<String, ValueExpression>();

            @Override
            public ValueExpression resolveVariable(String variable) {
                return map.get(variable);
            }

            @Override
            public ValueExpression setVariable(String variable, ValueExpression expression) {
                return map.put(variable, expression);
            }
        };
        private final ELResolver resolver;

        public SimpleContext(Map<String, Object> map, ExpressionFactory ef, ELResolver elResolver) {
            this.resolver = elResolver;

            if (map != null && !map.isEmpty()) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof ValueExpression) {
                        variables.setVariable(entry.getKey(), (ValueExpression) entry.getValue());
                    } else {
                        variables.setVariable(entry.getKey(), ef.createValueExpression(entry.getValue(), entry.getValue().getClass()));
                    }
                }
            }
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return null;
        }

        @Override
        public VariableMapper getVariableMapper() {
            return variables;
        }

        @Override
        public ELResolver getELResolver() {
            return resolver;
        }
    }

    private static class SimpleELResolver extends ELResolver {

        private boolean isResolvable(Object base) {
            return base == null;
        }

        private boolean resolve(ELContext context, Object base, Object property) {
            context.setPropertyResolved(isResolvable(base) && property instanceof String);
            return context.isPropertyResolved();
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return isResolvable(context) ? String.class : null;
        }

        @Override
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
            return null;
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property) {
            return resolve(context, base, property) ? Object.class : null;
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property) {
            return null;
        }

        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return resolve(context, base, property) ? true : false;
        }

        @Override
        public void setValue(ELContext context, Object base, Object property, Object value) throws PropertyNotWritableException {
            throw new PropertyNotWritableException("Resolver is read only!");
        }

        @Override
        public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
            if (resolve(context, base, method)) {
                throw new NullPointerException("Cannot invoke method " + method + " on null");
            }
            return null;
        }
    }

    private static class FullELResolver extends ELResolver {

        private final CompositeELResolver delegate;

        public FullELResolver() {
            delegate = new CompositeELResolver();
            delegate.add(new ArrayELResolver(true));
            delegate.add(new ListELResolver(true));
            delegate.add(new MapELResolver(true));
            delegate.add(new BeanELResolver(true));
        }

        public FullELResolver(ELResolver resolver) {
            delegate = new CompositeELResolver();
            delegate.add(new ArrayELResolver(true));
            delegate.add(new ListELResolver(true));
            delegate.add(new MapELResolver(true));
            delegate.add(new BeanELResolver(true));
            delegate.add(resolver);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property) throws NullPointerException, PropertyNotFoundException, ELException {
            return delegate.getValue(context, base, property);
        }

        @Override
        public void setValue(ELContext context, Object base, Object property, Object value) throws NullPointerException, PropertyNotFoundException, PropertyNotWritableException,
            ELException {
            delegate.setValue(context, base, property, value);
        }

        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property) throws NullPointerException, PropertyNotFoundException, ELException {
            return delegate.isReadOnly(context, base, property);
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
            return delegate.getFeatureDescriptors(context, base);
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return delegate.getCommonPropertyType(context, base);
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property) throws NullPointerException, PropertyNotFoundException, ELException {
            return delegate.getType(context, base, property);
        }

        @Override
        public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
            return delegate.invoke(context, base, method, paramTypes, params);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
