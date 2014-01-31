package com.blazebit.security.impl.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.blazebit.security.factory.EntityResourceFactory;
import com.blazebit.security.factory.ResourceNameFactory;
import com.blazebit.security.impl.el.utils.ELUtils;
import com.blazebit.security.impl.model.AbstractEntityField;
import com.blazebit.security.metamodel.ResourceMetamodel;
import com.blazebit.security.model.IdHolder;
import com.blazebit.security.model.Resource;
import com.blazebit.security.spi.EntityResource;
import com.blazebit.security.spi.ResourceDefinition;

public class ResourceNameFactoryImpl implements ResourceNameFactory {

    @Inject
    private EntityResourceFactory entityResourceFactory;

    @Inject
    private ResourceMetamodel resourceMetamodel;

    @Override
    public Resource createResource(IdHolder entityObject, String field) {
        List<ResourceDefinition> resourceDefinitions = resourceMetamodel.getEntityResources().get(new EntityResource(entityObject.getClass().getName()));
        Map<String, Object> variableMap = new HashMap<String, Object>();

        variableMap.put("object", entityObject);

        if (resourceDefinitions.size() == 1) {
            ResourceDefinition def = resourceDefinitions.get(0);

            if (com.blazebit.lang.StringUtils.isEmpty(def.getTestExpression())) {
                return entityResourceFactory.createResource(def.getResourceName(), field, entityObject.getId());
            }
        }

        for (ResourceDefinition def : resourceDefinitions) {
            if (Boolean.TRUE.equals(ELUtils.getValue(def.getTestExpression(), Boolean.class, variableMap))) {
                return entityResourceFactory.createResource(def.getResourceName(), field, entityObject.getId());
            }
        }

        throw new RuntimeException("No resource definition found!!");
    }

    @Override
    public Resource createResource(IdHolder entity) {
        return createResource(entity, AbstractEntityField.EMPTY_FIELD);
    }

}
