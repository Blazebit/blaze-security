/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.user;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.GroupHandlerBaseBean;
import com.blazebit.security.web.bean.GroupView;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "userBean")
@ViewScoped
public class UserBean extends GroupHandlerBaseBean implements GroupView, PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;

    @Inject
    private PermissionManager permissionManager;
    private List<User> users = new ArrayList<User>();
    private User selectedUser;
    private TreeNode permissionRoot;
    private TreeNode groupRoot;
    private User newUser = new User();

    // redirects to start page
    public void backToIndex() throws IOException {
        userSession.setUser(null);
        ((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false)).invalidate();
        FacesContext.getCurrentInstance().getExternalContext().redirect("../index.xhtml");
        FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    // loads all users, excludes logged in user
    @PostConstruct
    public void init() {
        users = userService.findUsers(userSession.getSelectedCompany());
        users.remove(userSession.getUser());
        if (getSelectedUser() != null) {
            selectUser(getSelectedUser());
        }
    }

    // first tab: select user -> display groups and permissions of selected user
    public void selectUser(User user) {
        this.selectedUser = user;
        this.userSession.setSelectedUser(selectedUser);

        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);

        this.permissionRoot = new DefaultTreeNode("root", null);
        permissionRoot = getPermissionTree(userPermissions, userDataPermissions);

        List<UserGroup> groups = userGroupService.getGroupsForUser(selectedUser);
        this.groupRoot = buildGroupTree(groups);
    }

    // saves new user
    public void saveUser() {
        userService.createUser(userSession.getSelectedCompany(), newUser.getUsername());
        users = userService.findUsers(userSession.getSelectedCompany());
        users.remove(userSession.getUser());
    }

    public void saveUser(User user) {
        if (user.equals(getSelectedUser())) {
            userSession.setUser(null);
        }
    }

    public void deleteUser(User user) {
        if (user.equals(userSession.getSelectedUser())) {
            userSession.setSelectedUser(null);
        }
        userService.delete(user);
        users = userService.findUsers(userSession.getSelectedCompany());
        users.remove(userSession.getUser());
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(User selectedUser) {
        this.selectedUser = selectedUser;
    }

    public List<User> getUsers() {
        return users;
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionRoot;
    }

    @Override
    public TreeNode getGroupRoot() {
        return groupRoot;
    }

    public User getNewUser() {
        return newUser;
    }

    public void setNewUser(User newUser) {
        this.newUser = newUser;
    }

}
