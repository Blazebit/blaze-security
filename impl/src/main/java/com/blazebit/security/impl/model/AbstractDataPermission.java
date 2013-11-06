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

import java.io.Serializable;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.blazebit.security.Permission;

@MappedSuperclass
public abstract class AbstractDataPermission<S, P extends DataPermissionId<S>> implements Permission, Serializable {

    private static final long serialVersionUID = 1L;
    protected P id;
    private EntityAction entityAction;
    private EntityObjectField entityObjectField;
    private S subject;

    public void setId(P id) {
        this.id = id;
        this.entityAction = new EntityAction(id);
        this.entityObjectField = new EntityObjectField(id);

        if (id != null) {
            if (subject != null) {
                id.setSubject(subject);
            }
            if (entityAction != null) {
                id.setActionName(entityAction.getActionName());
            }
            if (entityObjectField != null) {
                id.setEntity(entityObjectField.getEntity());
                id.setField(entityObjectField.getField());
                id.setEntityId(entityObjectField.getEntityId());
            }
        }
    }

    @Transient
    @Override
    public EntityAction getAction() {
        return entityAction;
    }

    public void setEntityAction(EntityAction entityAction) {
        entityAction.attachToPermissionId(id);
        this.entityAction = entityAction;
    }

    @Transient
    @Override
    public EntityObjectField getResource() {
        return entityObjectField;
    }

    public void setEntityObjectField(EntityObjectField entityObjectField) {
        entityObjectField.attachToPermissionId(id);
        this.entityObjectField = entityObjectField;
    }

    @Transient
    public S getSubject() {
        return id == null ? null : id.getSubject();
    }

    public void setSubject(S subject) {
        this.subject = subject;
        id.setSubject(subject);
    }

 

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityAction == null) ? 0 : entityAction.hashCode());
        result = prime * result + ((entityObjectField == null) ? 0 : entityObjectField.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((subject == null) ? 0 : subject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractDataPermission other = (AbstractDataPermission) obj;
        if (entityAction == null) {
            if (other.entityAction != null)
                return false;
        } else if (!entityAction.equals(other.entityAction))
            return false;
        if (entityObjectField == null) {
            if (other.entityObjectField != null)
                return false;
        } else if (!entityObjectField.equals(other.entityObjectField))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (subject == null) {
            if (other.subject != null)
                return false;
        } else if (!subject.equals(other.subject))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Permission{" + "id=" + id + "}";
    }

}