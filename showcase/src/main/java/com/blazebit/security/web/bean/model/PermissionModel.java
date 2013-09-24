/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

import com.blazebit.security.Permission;

/**
 *
 * @author cuszk
 */
public class PermissionModel {

    private Permission permission;
    private boolean selected;

    public PermissionModel(Permission permission, boolean selected) {
        this.permission = permission;
        this.selected = selected;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
