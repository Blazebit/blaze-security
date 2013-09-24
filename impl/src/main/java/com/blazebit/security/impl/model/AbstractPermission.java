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

import com.blazebit.security.Permission;
import java.io.Serializable;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class AbstractPermission<S, P extends PermissionId<S>> implements Permission, Serializable {

    private static final long serialVersionUID = 1L;
    protected P id;
    private EntityAction entityAction;
    private EntityField entityField;
    private S subject;

    public void setId(P id) {
        this.id = id;
        this.entityAction = new EntityAction(id);
        this.entityField = new EntityField(id);

        if (id != null) {
            if (subject != null) {
                id.setSubject(subject);
            }
            if (entityAction != null) {
                id.setActionName(entityAction.getActionName());
            }
            if (entityField != null) {
                id.setEntity(entityField.getEntity());
                id.setField(entityField.getField());
            }
        }
    }

    public void setEntityAction(EntityAction entityAction) {
        entityAction.attachToPermissionId(id);
        this.entityAction = entityAction;
    }

    @Transient
    @Override
    public EntityAction getAction() {
        return entityAction;
    }

    @Transient
    @Override
    public EntityField getResource() {
        return entityField;
    }

    public void setEntityField(EntityField entityField) {
        entityField.attachToPermissionId(id);
        this.entityField = entityField;

    }

    public void setSubject(S subject) {
        if (id == null) {
            this.subject = subject;
        } else {
            id.setSubject(subject);

        }
    }

    @Transient
    public S getSubject() {
        return id == null ? null : id.getSubject();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractPermission<S, P> other = (AbstractPermission<S, P>) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Permission{" + "id=" + id + "}";
    }
   
}