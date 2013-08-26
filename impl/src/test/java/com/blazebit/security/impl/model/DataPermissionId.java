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

import javax.persistence.Basic;
import javax.persistence.Embeddable;

@Embeddable
public class DataPermissionId<S> extends PermissionId<S> {
    
    private static final long serialVersionUID = 1L;
    private String entityId;
    
    @Basic(optional = true)
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public String getActionName() {
        return super.getActionName(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EntityConstants getEntity() {
        return super.getEntity(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getField() {
        return super.getField(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public S getSubject() {
        return super.getSubject(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this.entityId != null ? this.entityId.hashCode() : 0);
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
        final DataPermissionId<S> other = (DataPermissionId<S>) obj;
        if ((this.entityId == null) ? (other.entityId != null) : !this.entityId.equals(other.entityId)) {
            return false;
        }
        return true;
    }
    
     
    
    
    
    
   
}