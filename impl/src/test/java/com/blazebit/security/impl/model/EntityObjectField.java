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
import javax.persistence.Embeddable;

/**
 *
 * @author Christian
 */
public class EntityObjectField implements Resource {

    protected DataPermissionId<?> permissionId;
    private EntityConstants entity;
    private String field;
    private String entityId;

    public <P extends DataPermissionId<?>> EntityObjectField(P permissionId) {
        this.permissionId = permissionId;
    }
    
    @Override
    public boolean matches(Resource resource) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    <P extends DataPermissionId<?>> void attachToPermissionId(P permissionId) {
        this.permissionId = permissionId;
        
        if(entity != null) {
            permissionId.setEntity(entity);
            entity = null;
        }
        if(field != null) {
            permissionId.setField(field);
            field = null;
        }
        if(entityId != null) {
            permissionId.setEntityId(entityId);
            entityId = null;
        }
    }

    public EntityConstants getEntity() {
        return permissionId == null ? entity : permissionId.getEntity();
    }

    public void setEntity(EntityConstants entity) {
        if(permissionId == null) {
            this.entity = entity;
        } else {
            permissionId.setEntity(entity);
        }
    }

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

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
}
