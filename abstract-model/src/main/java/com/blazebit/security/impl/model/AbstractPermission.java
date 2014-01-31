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

import com.blazebit.security.model.Permission;

@MappedSuperclass
public abstract class AbstractPermission<S, P extends AbstractPermissionId<S>, A extends AbstractEntityAction, F extends AbstractEntityField> implements Permission, Serializable {

    private static final long serialVersionUID = 1L;
    protected P id;
    private A entityAction;
    private F entityField;
    private S subject;
    
	protected abstract A createEntityAction(P id);
	
	protected abstract F createEntityField(P id);
    
    public void setId(P id) {
        this.id = id;
		this.entityAction = createEntityAction(id);
		this.entityField = createEntityField(id);

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

    public void setEntityAction(A entityAction) {
        entityAction.attachToPermissionId(id);
        this.entityAction = entityAction;
    }

    @Transient
    @Override
    public A getAction() {
        return entityAction;
    }

    @Transient
    @Override
    public F getResource() {
        return entityField;
    }

    public void setEntityField(F entityField) {
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityAction == null) ? 0 : entityAction.hashCode());
        result = prime * result + ((entityField == null) ? 0 : entityField.hashCode());
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
        AbstractPermission<?, ?, ?, ?> other = (AbstractPermission<?, ?, ?, ?>) obj;
        if (entityAction == null) {
            if (other.entityAction != null)
                return false;
        } else if (!entityAction.equals(other.entityAction))
            return false;
        if (entityField == null) {
            if (other.entityField != null)
                return false;
        } else if (!entityField.equals(other.entityField))
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