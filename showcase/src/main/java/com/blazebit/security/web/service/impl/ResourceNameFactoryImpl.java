package com.blazebit.security.web.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.EntityResource;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Resource;
import com.blazebit.security.ResourceDefinition;
import com.blazebit.security.ResourceNameExtension;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.ResourceName;
import com.blazebit.security.web.service.api.ResourceNameFactory;

public class ResourceNameFactoryImpl implements ResourceNameFactory {

    @Inject
    private EntityResourceFactory entityResourceFactory;

    @Inject
    private ResourceNameExtension resourceNameExtension;

    @Override
    public Resource createResource(IdHolder entityObject) {
        return createResource(entityObject, EntityField.EMPTY_FIELD);
    }

    @Override
    public Resource createResource(IdHolder entityObject, String field) {
        List<ResourceDefinition> resourceDefinitions = resourceNameExtension.getResources().get(new EntityResource(entityObject.getClass().getName()));
        ResourceName annotation = (ResourceName) AnnotationUtils.findAnnotation(entityObject.getClass(), ResourceName.class);
        if (annotation != null) {
            if (!StringUtils.isEmpty(annotation.test())) {
                Method getter = ReflectionUtils.getGetter(entityObject.getClass(), annotation.test());
                Object value;
                try {
                    value = getter.invoke(entityObject);
                    for (ResourceDefinition rd : resourceDefinitions) {
                        if (rd.getTestExpression().equals(value)) {
                            return entityResourceFactory.createResource(rd.getResourceName(), field, entityObject.getId());
                        }
                    }

                } catch (IllegalAccessException e) {
                } catch (IllegalArgumentException e) {
                } catch (InvocationTargetException e) {
                }
            }
        }
        return entityResourceFactory.createResource(entityObject.getClass(), field, entityObject.getId());
    }
    
    
}
