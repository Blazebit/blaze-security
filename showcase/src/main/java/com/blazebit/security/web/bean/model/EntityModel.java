/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

import com.blazebit.security.impl.model.EntityField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.deltaspike.core.util.ReflectionUtils;

/**
 *
 * @author cuszk
 */
public class EntityModel implements Serializable {

    private Class<?> entityClass;
    private String name;
    private List<String> fields = new ArrayList<String>();

    public EntityModel(Class<?> entityClass, String name) {
        this.entityClass = entityClass;
        this.name = name;
        Set<Field> allFields = ReflectionUtils.getAllDeclaredFields(entityClass);
        this.fields.add(EntityField.EMPTY_FIELD);
        for (Field field : allFields) {
            this.fields.add(field.getName());
        }
    }

    public EntityModel() {
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}
