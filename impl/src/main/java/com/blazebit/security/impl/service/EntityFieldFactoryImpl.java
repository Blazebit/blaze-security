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
package com.blazebit.security.impl.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.security.Action;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.ResourceName;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;

/**
 * 
 * @author cuszk
 */
public class EntityFieldFactoryImpl implements EntityFieldFactory {

    @Override
    public EntityField createResource(Class<?> clazz, String field, Integer id) {
        ResourceName annotation = (ResourceName) AnnotationUtils.findAnnotation(clazz, ResourceName.class);
        if (annotation != null) {
            if (id != null) {
                return new EntityObjectField(annotation.name(), field, String.valueOf(id));
            } else {
                return new EntityField(annotation.name(), field);
            }
        } else {
            throw new IllegalArgumentException("Class " + clazz
                + " does not have a ResourceName annotation, therefore it cannot be a resource!");
        }
    }

    @Override
    public EntityField createResource(Class<?> clazz) {
        return createResource(clazz, EntityField.EMPTY_FIELD);

    }

    @Override
    public EntityField createResource(Class<?> clazz, String field) {
        ResourceName annotation = (ResourceName) AnnotationUtils.findAnnotation(clazz, ResourceName.class);
        if (annotation != null) {
            return new EntityField(annotation.name(), field);
        } else {
            throw new IllegalArgumentException("Class " + clazz
                + " does not have a ResourceName annotation, therefore it cannot be a resource!");
        }
    }

    @Override
    public EntityField createResource(Class<?> clazz, Integer id) {
        return createResource(clazz, EntityField.EMPTY_FIELD, id);

    }

    // TODO read automatically
    @Override
    public List<Class<?>> getEntityClasses() {
        List<Class<?>> ret = new ArrayList<Class<?>>();
        ret.add(Carrier.class);
        ret.add(Party.class);
        ret.add(Contact.class);
        ret.add(CarrierGroup.class);
        ret.add(UserPermission.class);
        ret.add(UserDataPermission.class);
        ret.add(UserGroupPermission.class);
        ret.add(UserGroupDataPermission.class);
        ret.add(User.class);
        ret.add(UserGroup.class);
        return ret;
    }

    @Override
    public EntityField createResource(Subject subject) {
        return createResource(subject.getClass(), EntityField.EMPTY_FIELD, ((IdHolder) subject).getId());

    }

    @Override
    public EntityField createResource(Role role) {
        return createResource(role.getClass(), EntityField.EMPTY_FIELD, ((IdHolder) role).getId());
    }

    @Override
    public EntityField createResource(Action action) {
        return new EntityField(action.getClass().getSimpleName(), ((EntityAction) action).getActionName());
    }

}
