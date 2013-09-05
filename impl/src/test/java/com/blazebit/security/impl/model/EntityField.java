/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl.model;

import com.blazebit.security.EntityResource;
import com.blazebit.security.Resource;
import org.apache.commons.lang.StringUtils;

/**
 * Class representing an Entity resource with an optional field attribute. If
 * field attribute is empty the resource refers to all the entities defined by
 * the entity name attribute.
 *
 * @author Christian
 */
public class EntityField implements Resource, EntityResource {

    protected String entity;
    protected String field;
    protected PermissionId<?> permissionId;

    public EntityField() {
    }

    public EntityField(String entity, String field) {
        this.entity = entity;
        this.field = field;
    }

    public EntityField(String entity) {
        this.entity = entity;
        this.field = "";
    }

    public PermissionId<?> getPermissionId() {
        return permissionId;
    }

    public <P extends PermissionId<?>> EntityField(P permissionId) {
        this.permissionId = permissionId;
    }

    public <P extends PermissionId<?>> void attachToPermissionId(P permissionId) {
        this.permissionId = permissionId;

        if (entity != null) {
            permissionId.setEntity(entity);
            entity = null;
        }
        if (field != null) {
            permissionId.setField(field);
            field = null;
        }
    }

    @Override
    public boolean matches(Resource resource) {
        if (resource instanceof EntityResource) {
            EntityResource _resource = (EntityResource) resource;
            if (permissionId == null) {
                return this.getEntity().equals(_resource.getEntity()) && (StringUtils.isEmpty(this.getField()) || this.getField().equals(_resource.getField()));
            } else {
                return permissionId.getEntity().equals(_resource.getEntity()) && (StringUtils.isEmpty(permissionId.getField()) || permissionId.getField().equals(_resource.getField()));
            }
        } else {
            throw new IllegalArgumentException("Not supported resource type");
        }
    }

    @Override
    public String getEntity() {
        return permissionId == null ? entity : permissionId.getEntity();
    }

    public void setEntity(String entity) {
        this.entity = entity;
        if (permissionId != null) {
            permissionId.setEntity(entity);
        }
    }

    @Override
    public String getField() {
        return permissionId == null ? field : permissionId.getField();
    }

    public void setField(String field) {
        this.field = field;
        if (permissionId != null) {
            permissionId.setField(field);
        }
    }

    @Override
    public String toString() {
        return "EntityField{" + "entity=" + getEntity() + ", field=" + getField() + '}';
    }
}
