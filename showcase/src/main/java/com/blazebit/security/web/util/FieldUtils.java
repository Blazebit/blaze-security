package com.blazebit.security.web.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.blazebit.reflection.ReflectionUtils;

public class FieldUtils {

    public static List<Field> getPrimitiveFields(Class<?> entityClass) {
        return filterFields(entityClass).get(0);
    }

    public static List<Field> getCollectionFields(Class<?> entityClass) {
        return filterFields(entityClass).get(1);
    }

    private static List<List<Field>> filterFields(Class<?> entityClass) {
        List<List<Field>> ret = new ArrayList<List<Field>>();
        ret.add(new ArrayList<Field>());
        ret.add(new ArrayList<Field>());
        Field[] all = ReflectionUtils.getInstanceFields(entityClass);
        for (Field field : all) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                ret.get(1).add(field);
            } else {
                ret.get(0).add(field);
            }
        }
        return ret;
    }

}
