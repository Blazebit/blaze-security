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
package com.blazebit.security.model;

import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;

import com.blazebit.security.model.AbstractPermissionId;

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
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof AbstractDataPermissionId)) {
            return false;
        }
        AbstractDataPermissionId<?> other = (AbstractDataPermissionId<?>) obj;
        if (entityId == null) {
            if (other.entityId != null) {
                return false;
            }
        } else if (!entityId.equals(other.entityId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ID: " + entityId + "-" + super.toString();
    }
}