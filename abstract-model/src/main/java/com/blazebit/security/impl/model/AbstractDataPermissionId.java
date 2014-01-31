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

import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractDataPermissionId<S> extends AbstractPermissionId<S> {

    private static final long serialVersionUID = 1L;
    private String entityId;

    @Basic(optional = false)
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.entityId != null ? this.entityId.hashCode() : 0);
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
        final AbstractDataPermissionId<?> other = (AbstractDataPermissionId<?>) obj;
        if ((this.entityId == null) ? (other.entityId != null) : !this.entityId.equals(other.entityId)) {
            return false;
        }
        if ((this.getActionName() == null) ? (other.getActionName() != null) : !this.getActionName().equals(other.getActionName())) {
            return false;
        }
        if ((this.getEntity() == null) ? (other.getEntity() != null) : !this.getEntity().equals(other.getEntity())) {
            return false;
        }
        if ((this.getField() == null) ? (other.getField() != null) : !this.getField().equals(other.getField())) {
            return false;
        }
        if (this.getSubject() != other.getSubject() && (this.getSubject() == null || !this.getSubject().equals(other.getSubject()))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "ID: " + entityId + "-" + super.toString();
    }
}