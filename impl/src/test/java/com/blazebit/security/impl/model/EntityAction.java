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

import com.blazebit.security.Action;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author Christian
 */
@Entity
@Table(name = "Entity_Action", schema = "USERROLES")
public class EntityAction implements Action {

    private Integer id;
    private PermissionId<?> permissionId;
    private String actionName;

    public EntityAction() {
    }

    @Transient
    public PermissionId<?> getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(PermissionId<?> permissionId) {
        this.permissionId = permissionId;
    }

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public <P extends PermissionId<?>> EntityAction(P id) {
        this.permissionId = id;
        this.actionName=permissionId.getActionName();
    }

    <P extends PermissionId<?>> void attachToPermissionId(P permissionId) {
        this.permissionId = permissionId;

        if (actionName != null) {
            permissionId.setActionName(actionName);
            actionName = null;
        }
    }

    @Override
    public boolean matches(Action action) {
        if (this.actionName != null) {
            return ((EntityAction) action).getActionName().equals(this.actionName);
        } else {
            return ((EntityAction) action).getActionName().equals(this.permissionId.getActionName());
        }
    }

    @Basic
    public String getActionName() {
        return permissionId == null ? this.actionName : permissionId.getActionName();
    }

   public void setActionName(String actionName) {
        if(permissionId == null) {
            this.actionName = actionName;
        } else {
            permissionId.setActionName(actionName);
        }
    }
}
