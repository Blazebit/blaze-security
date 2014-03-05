/*
 * Copyright 2013 Blazebit.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.blazebit.security.integration.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.lang.StringUtils;
import com.blazebit.security.entity.EntityIdAccessor;
import com.blazebit.security.entity.EntityIdConverter;
import com.blazebit.security.entity.EntityResource;
import com.blazebit.security.entity.EntityResourceDefinition;
import com.blazebit.security.entity.EntityResourceFactory;
import com.blazebit.security.entity.EntityResourceMetamodel;
import com.blazebit.security.entity.EntityResourceType;
import com.blazebit.security.integration.util.ELUtils;
import com.blazebit.security.model.EntityField;
import com.blazebit.security.model.EntityObjectField;
import com.blazebit.security.model.Resource;

/**
 * 
 * @author cuszk
 */
public class EntityResourceFactoryImpl implements EntityResourceFactory {
    
    @Inject
    private EntityIdConverter idConverter;
    @Inject
    private EntityIdAccessor idAccessor;
    @Inject
    private EntityResourceMetamodel resourceMetamodel;
    
    private Resource createResource(String entity, String field, Serializable id, boolean idRequired) {
        if (entity == null) {
            throw new NullPointerException("entity");
        }
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (idRequired && id == null) {
            throw new NullPointerException("id");
        }
        if (entity.isEmpty()) {
            throw new IllegalArgumentException("Invalid entity name");
        }
        
        if(idRequired) {
            return new EntityObjectField(entity, field, idConverter.getIdAsString(id));
        } else {
            return new EntityField(entity, field);
        }
    }
    
    private String getResourceName(Class<?> clazz) {
        EntityResourceType annotation = (EntityResourceType) AnnotationUtils
            .findAnnotation(clazz, EntityResourceType.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class " + clazz + " does not have a ResourceName annotation, therefore it cannot be a resource!");
        }
        return annotation.name();
    }

	@Override
	public Resource createResource(String entity) {
		return createResource(entity, EntityResource.EMPTY_FIELD, null, false);
	}

	@Override
	public Resource createResource(String entity, String field) {
        return createResource(entity, field, null, false);
	}

	@Override
	public Resource createResource(String entity, Serializable id) {
        return createResource(entity, EntityResource.EMPTY_FIELD, id, true);
	}

	@Override
	public Resource createResource(String entity, String field, Serializable id) {
        return createResource(entity, field, id, true);
	}

	@Override
	public Resource createResource(Class<?> clazz, String field, Serializable id) {
		return createResource(getResourceName(clazz), field, id, true);
	}

	@Override
	public Resource createResource(Class<?> clazz) {
		return createResource(clazz, EntityResource.EMPTY_FIELD);

	}

	@Override
	public Resource createResource(Class<?> clazz, String field) {
		return createResource(getResourceName(clazz), field, null, false);
	}

	@Override
	public Resource createResource(Class<?> clazz, Serializable id) {
		return createResource(clazz, EntityResource.EMPTY_FIELD, id);

	}

    @Override
    public Resource createResource(Object entityObject, String field) {
        List<EntityResourceDefinition> resourceDefinitions = resourceMetamodel.getEntityResources().get(entityObject.getClass().getName());
        Map<String, Object> variableMap = new HashMap<String, Object>();

        variableMap.put("object", entityObject);

        if (resourceDefinitions.size() == 1) {
            EntityResourceDefinition def = resourceDefinitions.get(0);

            if (StringUtils.isEmpty(def.getTestExpression())) {
                return createResource(def.getResourceName(), field, idAccessor.getId(entityObject));
            }
        }

        for (EntityResourceDefinition def : resourceDefinitions) {
            if (Boolean.TRUE.equals(ELUtils.getValue(def.getTestExpression(), Boolean.class, variableMap))) {
                return createResource(def.getResourceName(), field, idAccessor.getId(entityObject));
            }
        }

        throw new RuntimeException("No resource definition found!!");
    }

    @Override
    public Resource createResource(Object entityObject, String field, boolean withId) {
        List<EntityResourceDefinition> resourceDefinitions = resourceMetamodel.getEntityResources().get(entityObject.getClass().getName());
        Map<String, Object> variableMap = new HashMap<String, Object>();

        variableMap.put("object", entityObject);

        if (resourceDefinitions.size() == 1) {
            EntityResourceDefinition def = resourceDefinitions.get(0);

            if (StringUtils.isEmpty(def.getTestExpression())) {
                return createResource(def.getResourceName(), field, withId ? idAccessor.getId(entityObject) : null, withId);
            }
        }

        for (EntityResourceDefinition def : resourceDefinitions) {
            if (Boolean.TRUE.equals(ELUtils.getValue(def.getTestExpression(), Boolean.class, variableMap))) {
                return createResource(def.getResourceName(), field, withId ? idAccessor.getId(entityObject) : null, withId);
            }
        }

        throw new RuntimeException("No resource definition found!!");
    }

    @Override
    public Resource createResource(Object entity) {
        return createResource(entity, EntityResource.EMPTY_FIELD);
    }

    @Override
    public Resource createResource(Object entity, boolean withId) {
        return createResource(entity, EntityResource.EMPTY_FIELD, withId);
    }

}
