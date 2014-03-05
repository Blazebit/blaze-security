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

/**
 * 
 * @author Christian
 */
public class AbstractEntityAction implements Action, Comparable<Action> {

    private String actionName;
    private AbstractPermissionId<?> permissionId;

    public AbstractEntityAction() {
    }

    public AbstractEntityAction(String action) {
        this.actionName = action;
    }

    @Override
    public String getName() {
        return permissionId == null ? this.actionName : permissionId.getActionName();
    }

    public void setName(String actionName) {
        if (permissionId == null) {
            this.actionName = actionName;
        } else {
            permissionId.setActionName(actionName);
        }
    }

    public AbstractPermissionId<?> getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(AbstractPermissionId<?> permissionId) {
        this.permissionId = permissionId;
    }

    public <P extends AbstractPermissionId<?>> AbstractEntityAction(P id) {
        this.permissionId = id;
        this.actionName = permissionId.getActionName();
    }

    <P extends AbstractPermissionId<?>> void attachToPermissionId(P permissionId) {
        this.permissionId = permissionId;

        if (actionName != null) {
            permissionId.setActionName(actionName);
            actionName = null;
        }
    }

    @Override
    public String toString() {
        return "EntityAction{" + "actionName=" + getName() + '}';
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
        if (!(obj instanceof Action)) {
            return false;
        }
        final Action other = (Action) obj;
        final String name = getName();
        if ((name == null) ? (other.getName() != null) : !name.equals(other.getName())) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Action o) {
        return o.getName().compareToIgnoreCase(getName());
    }

}
