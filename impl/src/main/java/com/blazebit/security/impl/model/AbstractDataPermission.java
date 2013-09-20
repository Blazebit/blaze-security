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
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

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
        int hash = 3;
        hash = 41 * hash + (this.id != null ? this.id.hashCode() : 0);
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
        final AbstractDataPermission<S, P> other = (AbstractDataPermission<S, P>) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Permission{" + "id=" + id + ", entityAction=" + entityAction + ", entityObjectField=" + entityObjectField + ", subject=" + subject + '}';
    }

    @Override
    public boolean matches(Permission _permission) {
        AbstractDataPermission p = (AbstractDataPermission) _permission;
        return this.id.getEntity().equals(p.id.getEntity()) && this.id.getField().equals(p.id.getField()) && this.id.getActionName().equals(p.id.getActionName()) && this.id.getEntityId().equals(p.id.getEntityId());
    }
}