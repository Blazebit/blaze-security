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

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractPermissionId<S> implements Serializable {

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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "subject", nullable = false)
    public S getSubject() {
        return subject;
    }

    public void setSubject(S subject) {
        this.subject = subject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actionName == null) ? 0 : actionName.hashCode());
        result = prime * result + ((entity == null) ? 0 : entity.hashCode());
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((subject == null) ? 0 : subject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractPermissionId)) {
            return false;
        }
        AbstractPermissionId<?> other = (AbstractPermissionId<?>) obj;
        if (actionName == null) {
            if (other.actionName != null) {
                return false;
            }
        } else if (!actionName.equals(other.actionName)) {
            return false;
        }
        if (entity == null) {
            if (other.entity != null) {
                return false;
            }
        } else if (!entity.equals(other.entity)) {
            return false;
        }
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else if (!field.equals(other.field)) {
            return false;
        }
        if (subject == null) {
            if (other.subject != null) {
                return false;
            }
        } else if (!subject.equals(other.subject)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return actionName + ", "+ entity + ", " + field + ", subject: " + subject;
    }
}