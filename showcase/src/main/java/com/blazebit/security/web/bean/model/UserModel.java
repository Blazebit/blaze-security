/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

import com.blazebit.security.impl.model.User;

/**
 * 
 * @author cuszk
 */
public class UserModel {

    private User user;
    private boolean selected;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public UserModel(User user, boolean selected) {
        this.user = user;
        this.selected = selected;
    }
}
