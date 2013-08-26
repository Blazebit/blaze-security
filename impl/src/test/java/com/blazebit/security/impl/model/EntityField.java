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

import com.blazebit.security.Resource;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author Christian
 */
@Entity
public class EntityField implements Resource {

    private Integer id;
    protected PermissionId<?> permissionId;
    private EntityConstants entity;
    private String field;

    public EntityField() {
    }

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Transient
    public PermissionId<?> getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(PermissionId<?> permissionId) {
        this.permissionId = permissionId;
        this.entity=permissionId.getEntity();
        this.field=permissionId.getField();
    }

    public <P extends PermissionId<?>> EntityField(P permissionId) {
        this.permissionId = permissionId;
    }

    <P extends PermissionId<?>> void attachToPermissionId(P permissionId) {
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
        EntityField _resource = (EntityField) resource;
        if (permissionId == null) {
            return this.getEntity().equals(_resource.getEntity()) && (this.getField() == null || (this.getField() != null && this.getField().equals(_resource.getField())));
        } else {
            return permissionId.getEntity().equals(_resource.getEntity()) && permissionId.getField().equals(_resource.getField());
        }
    }

    @Enumerated(EnumType.STRING)
    public EntityConstants getEntity() {
        return permissionId == null ? entity : permissionId.getEntity();
    }

    public void setEntity(EntityConstants entity) {
        this.entity = entity;
        if (permissionId != null) {
            permissionId.setEntity(entity);
        }
    }

    @Basic
    public String getField() {
        return permissionId == null ? field : permissionId.getField();
    }

    public void setField(String field) {
        if(permissionId == null) {
            this.field = field;
        } else {
            permissionId.setField(field);
        }
    }

  
}
