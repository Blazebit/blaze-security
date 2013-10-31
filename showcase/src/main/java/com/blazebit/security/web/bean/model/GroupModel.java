/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

import com.blazebit.security.impl.model.UserGroup;

/**
 * 
 * @author cuszk
 */
public class GroupModel {

    private UserGroup userGroup;
    private boolean selected;
    private boolean marked;

    public GroupModel(UserGroup userGroup, boolean marked, boolean selected) {
        this.userGroup = userGroup;
        this.marked = marked;
        this.selected = selected;

    }

    public UserGroup getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

}
