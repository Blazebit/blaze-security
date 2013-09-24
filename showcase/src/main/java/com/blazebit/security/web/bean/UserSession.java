/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 *
 * @author Christian
 */
@Named
@SessionScoped
public class UserSession implements Serializable {

    private User user;
    private User selectedUser;
    private UserGroup selectedUserGroup;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(User selectedUser) {
        this.selectedUser = selectedUser;
    }

    public UserGroup getSelectedUserGroup() {
        return selectedUserGroup;
    }

    public void setSelectedUserGroup(UserGroup selectedUserGroup) {
        this.selectedUserGroup = selectedUserGroup;
    }
    
    
}
