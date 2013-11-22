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

import com.blazebit.security.Action;
import com.blazebit.security.constants.ActionConstants;

/**
 * 
 * @author Christian
 */
public class EntityAction implements Action, Comparable<EntityAction> {

    private String actionName;
    private PermissionId<?> permissionId;

    public EntityAction() {
    }

    public EntityAction(ActionConstants constant) {
        this.actionName = constant.name();
    }

    public String getActionName() {
        return permissionId == null ? this.actionName : permissionId.getActionName();
    }

    public void setActionName(String actionName) {
        if (permissionId == null) {
            this.actionName = actionName;
        } else {
            permissionId.setActionName(actionName);
        }
    }

    public PermissionId<?> getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(PermissionId<?> permissionId) {
        this.permissionId = permissionId;
    }

    public <P extends PermissionId<?>> EntityAction(P id) {
        this.permissionId = id;
        this.actionName = permissionId.getActionName();
    }

    <P extends PermissionId<?>> void attachToPermissionId(P permissionId) {
        this.permissionId = permissionId;

        if (actionName != null) {
            permissionId.setActionName(actionName);
            actionName = null;
        }
    }

    @Override
    public boolean implies(Action action) {
        if (this.actionName != null) {
            return ((EntityAction) action).getActionName().equals(this.actionName);
        } else {
            return ((EntityAction) action).getActionName().equals(this.permissionId.getActionName());
        }
    }

    @Override
    public String toString() {
        return "EntityAction{" + "actionName=" + getActionName() + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.actionName != null ? this.actionName.hashCode() : 0);
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
        final EntityAction other = (EntityAction) obj;
        if ((this.actionName == null) ? (other.actionName != null) : !this.actionName.equals(other.actionName)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(EntityAction o) {
        return o.getActionName().compareToIgnoreCase(actionName);
    }

}
