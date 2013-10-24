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
package com.blazebit.security.impl.model;

import com.blazebit.lang.StringUtils;
import com.blazebit.security.Resource;

/**
 * Class that represents an entity object with an id. Field attribute is inherited from EntityField and it is optional. If not
 * specified it refers to all the entities with the given id.
 * 
 * @author Christian
 */
public class EntityObjectField extends EntityField {

    private DataPermissionId<?> permissionId;
    private String entityId;

    public EntityObjectField() {
    }

    public EntityObjectField(String entity, String field, String id) {
        super(entity, field);
        this.entityId = id;
    }

    public EntityObjectField(String entity, String id) {
        super(entity);
        this.entityId = id;
    }

    @Override
    public DataPermissionId<?> getPermissionId() {
        return permissionId;
    }

    public <P extends DataPermissionId<?>> EntityObjectField(P permissionId) {
        this.permissionId = permissionId;
        this.entityId = permissionId.getEntityId();
        this.entity = permissionId.getEntity();
        this.field = permissionId.getField();
    }

    @Override
    public boolean implies(Resource resource) {
        if (resource instanceof EntityObjectField) {
            EntityObjectField _resource = (EntityObjectField) resource;
            if (permissionId == null) {
                return super.implies(resource) && this.getEntityId().equals(_resource.getEntityId());
            } else {
                return permissionId.getEntity().equals(_resource.getEntity()) && permissionId.getEntityId().equals(_resource.getEntityId())
                    && (StringUtils.isEmpty(permissionId.getField()) || permissionId.getField().equals(_resource.getField()));
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isReplaceableBy(Resource resource) {
        if (resource instanceof EntityObjectField) {
            EntityObjectField _resource = (EntityObjectField) resource;
            if (permissionId == null) {
                return this.getEntity().equals(_resource.getEntity()) && this.getEntityId().equals(_resource.getEntityId()) && (_resource.isEmptyField() && !this.isEmptyField());
            } else {
                return permissionId.getEntity().equals(_resource.getEntity()) && permissionId.getEntityId().equals(_resource.getEntityId())
                    && (_resource.isEmptyField() && !permissionId.getField().equals(EMPTY_FIELD));
            }
        } else {
            if (resource instanceof EntityField) {
                EntityField _resource = (EntityField) resource;
                if (permissionId == null) {
                    return this.getEntity().equals(_resource.getEntity()) && ((_resource.isEmptyField() && !this.isEmptyField()) || this.getField().equals(_resource.getField()));
                } else {
                    return permissionId.getEntity().equals(_resource.getEntity())
                        && (((_resource.isEmptyField() && !permissionId.getField().equals(EMPTY_FIELD)) || permissionId.getField().equals(_resource.getField())));
                }
            } else {
                throw new IllegalArgumentException("Not supported resource type");
            }
        }

    }

    <P extends DataPermissionId<?>> void attachToPermissionId(P permissionId) {
        this.permissionId = permissionId;

        if (entity != null) {
            permissionId.setEntity(super.entity);
            entity = null;
        }
        if (field != null) {
            permissionId.setField(field);
            field = null;
        }
        if (entityId != null) {
            permissionId.setEntityId(entityId);
            entityId = null;
        }
    }

    @Override
    public String getEntity() {
        return permissionId == null ? entity : permissionId.getEntity();
    }

    @Override
    public void setEntity(String entity) {
        if (permissionId == null) {
            this.entity = entity;
        } else {
            permissionId.setEntity(entity);
        }
    }

    @Override
    public String getField() {
        return permissionId == null ? field : permissionId.getField();
    }

    @Override
    public void setField(String field) {
        if (permissionId == null) {
            this.field = field;
        } else {
            permissionId.setField(field);
        }
    }

    public String getEntityId() {
        return permissionId == null ? entityId : permissionId.getEntityId();
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return "EntityObjectField{" + "entity=" + getEntity() + ", field=" + getField() + ", entityId=" + getEntityId() + '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityObjectField other = (EntityObjectField) obj;
        if ((this.entityId == null) ? (other.entityId != null) : !this.entityId.equals(other.entityId)) {
            return false;
        }

        return true;
    }

}
