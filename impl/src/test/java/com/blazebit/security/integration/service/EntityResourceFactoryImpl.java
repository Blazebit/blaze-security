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
package com.blazebit.security.integration.service;

import java.io.Serializable;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.security.annotation.ResourceName;
import com.blazebit.security.factory.EntityResourceFactory;
import com.blazebit.security.impl.model.AbstractEntityField;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.model.BaseEntity;
import com.blazebit.security.model.Resource;
import com.google.gson.Gson;

/**
 * 
 * @author cuszk
 */
public class EntityResourceFactoryImpl implements EntityResourceFactory {

	@Override
	public Resource createResource(String entity) {
		return new EntityField(entity, EntityField.EMPTY_FIELD);
	}

	@Override
	public Resource createResource(String entity, String field) {
		return new EntityField(entity, field);
	}

	@Override
	public Resource createResource(String entity, Serializable id) {
		return createResource(entity, EntityField.EMPTY_FIELD, id);
	}

	@Override
	public Resource createResource(String entity, String field, Serializable id) {
		return new EntityObjectField(entity, field, new Gson().toJson(id));
	}

	@Override
	public Resource createResource(Class<?> clazz, String field, Serializable id) {
		ResourceName annotation = (ResourceName) AnnotationUtils
				.findAnnotation(clazz, ResourceName.class);
		if (annotation != null) {
			if (id != null) {
				return new EntityObjectField(annotation.name(), field,
						new Gson().toJson(id));
			} else {
				return new EntityField(annotation.name(), field);
			}
		} else {
			throw new IllegalArgumentException(
					"Class "
							+ clazz
							+ " does not have a ResourceName annotation, therefore it cannot be a resource!");
		}
	}

	@Override
	public Resource createResource(Class<?> clazz) {
		return createResource(clazz, EntityField.EMPTY_FIELD);

	}

	@Override
	public Resource createResource(Class<?> clazz, String field) {
		ResourceName annotation = (ResourceName) AnnotationUtils
				.findAnnotation(clazz, ResourceName.class);
		if (annotation != null) {
			return new EntityField(annotation.name(), field);
		} else {
			throw new IllegalArgumentException(
					"Class "
							+ clazz
							+ " does not have a ResourceName annotation, therefore it cannot be a resource!");
		}
	}

	@Override
	public Resource createResource(Class<?> clazz, Serializable id) {
		return (AbstractEntityField) createResource(clazz,
				EntityField.EMPTY_FIELD, id);

	}

	@Override
	public Resource createResource(BaseEntity<? extends Serializable> entityObject) {
		return createResource(entityObject.getClass(), EntityField.EMPTY_FIELD,
				entityObject.getId());
	}

	@Override
	public Resource createResource(BaseEntity<? extends Serializable> entityObject, String field) {
		return createResource(entityObject.getClass(), field,
				entityObject.getId());
	}

}
