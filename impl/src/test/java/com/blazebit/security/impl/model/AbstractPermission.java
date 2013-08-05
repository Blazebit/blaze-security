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

import com.blazebit.security.Permission;
import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Transient;

public abstract class AbstractPermission<S, P extends PermissionId<S>> implements Permission<EntityField>, Serializable {

    private static final long serialVersionUID = 1L;
    private P id;
    private EntityAction entityAction;
    private EntityField entityField;
    private S subject;

    @EmbeddedId
    public P getId() {
        return id;
    }

    public void setId(P id) {
        this.id = id;
        this.entityAction = new EntityAction(id);
        this.entityField = new EntityField(id);
        
        if(id != null) {
            if(subject != null) {
                id.setSubject(subject);
            }
            if(entityAction != null) {
                id.setActionName(entityAction.getActionName());
            }
            if(entityField != null) {
                id.setEntity(entityField.getEntity());
                id.setField(entityField.getField());
            }
        }
    }

    @Override
    @Transient
    public EntityAction getAction() {
        return entityAction;
    }

    public void setAction(EntityAction entityAction) {
        entityAction.attachToPermissionId(id);
        this.entityAction = entityAction;
    }
    
    @Override
    @Transient
    public EntityField getResource() {
        return entityField;
    }

    @Transient
    public EntityField getEntityField() {
        return entityField;
    }

    public void setEntityField(EntityField entityField) {
        entityField.attachToPermissionId(id);
        this.entityField = entityField;
    }

    @Transient
    public S getSubject() {
        return id == null ? null : id.getSubject();
    }

    public void setSubject(S subject) {
        if(id == null) {
            this.subject = subject;
        } else {
            id.setSubject(subject);
        }
    }
}