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

import javax.persistence.Basic;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class PermissionId<S> implements Serializable {

    private static final long serialVersionUID = 1L;
    private String actionName;
    private String entity;
    private String field;
    private S subject;

    @Basic(optional = false)
    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    @Basic(optional = false)
    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    @Basic
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
        if (this.field == null) {
            this.field = "";
        }
    }

    @ManyToOne
    @JoinColumn(name = "subject", nullable = false)
    public S getSubject() {
        return subject;
    }

    public void setSubject(S subject) {
        this.subject = subject;
    }

    @Override
    public int hashCode() {
        int hash = 3;
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
        final PermissionId<?> other = (PermissionId<?>) obj;
        if ((this.actionName == null) ? (other.actionName != null) : !this.actionName.equals(other.actionName)) {
            return false;
        }
        if ((this.entity == null) ? (other.entity != null) : !this.entity.equals(other.entity)) {
            return false;
        }
        if ((this.field == null) ? (other.field != null) : !this.field.equals(other.field)) {
            return false;
        }
        if (this.subject != other.subject && (this.subject == null || !this.subject.equals(other.subject))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PermissionId{" + "actionName=" + actionName + ", entity=" + entity + ", field=" + field + ", subject=" + subject + '}';
    }

}