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
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.GroupView;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.PermissionViewUtils;
import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.bean.model.GroupModel;
import com.blazebit.security.web.bean.model.PermissionModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "userBean")
@ViewScoped
public class UserBean extends PermissionViewUtils implements GroupView, PermissionView, Serializable {

    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private RoleService roleService;
    @Inject
    private PermissionService securityService;
    @Inject
    private UserContext userContext;
    @Inject
    private UserSession userSession;
    @Inject
    private PermissionDataAccess permissionDataAccess;

    @Inject
    private PermissionManager permissionManager;
    private List<User> users = new ArrayList<User>();
    private User selectedUser;
    private List<PermissionModel> userPermissions = new ArrayList<PermissionModel>();
    private List<GroupModel> userGroups = new ArrayList<GroupModel>();
    private TreeNode permissionRoot;
    private TreeNode groupRoot;
    private boolean groupTreeView;
    private boolean permissionTreeView;
    private String newUserName = "new_user";

    public void backToIndex() throws IOException {
        userSession.setUser(null);
        ((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false)).invalidate();
        FacesContext.getCurrentInstance().getExternalContext().redirect("/SecurityWebProject/index.xhtml");
        FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    @PostConstruct
    public void init() {
        users = userService.findUsers();
        if (getSelectedUser() != null) {
            selectUser(getSelectedUser());
        }
    }

    private void initPermissionList(List<Permission> permissions) {
        this.userPermissions.clear();
        for (Permission permission : permissions) {
            this.userPermissions.add(new PermissionModel(permission, false));
        }
    }

    private void initGroupList(List<UserGroup> userGroups) {
        this.userGroups.clear();
        for (UserGroup userGroup : userGroups) {
            this.userGroups.add(new GroupModel(userGroup, false, false));
        }
    }

    // first tab: select user
    public void selectUser(User user) {
        this.selectedUser = user;
        this.userSession.setSelectedUser(selectedUser);
        this.userPermissions.clear();

        List<Permission> permissions = permissionManager.getAllPermissions(selectedUser);
        initPermissionList(permissions);

        this.permissionRoot = new DefaultTreeNode("root", null);
        buildPermissionTree(permissions, permissionRoot);
        this.permissionTreeView = true;

        List<UserGroup> groups = userGroupService.getGroupsForUser(selectedUser);
        initGroupList(groups);
        this.groupRoot = new DefaultTreeNode("root", null);
        buildGroupTree(groups, groupRoot);
        this.groupTreeView = true;

    }

    public void saveUser() {
        userService.createUser(newUserName);
        newUserName = "new_user";
        users = userService.findUsers();
    }

    public void saveUser(User user) {
        if (user.equals(getSelectedUser())) {
            userSession.setUser(null);
        }
        userService.delete(user);
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
    public List<PermissionModel> getPermissions() {
        return this.userPermissions;
    }

    @Override
    public String getPermissionHeader() {
        return "Permissions for " + (getSelectedUser() != null ? getSelectedUser().getUsername() : "");
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionRoot;
    }

    @Override
    public void setShowGroupTreeView(boolean set) {
        this.groupTreeView = set;
    }

    @Override
    public void setShowPermissionTreeView(boolean set) {
        this.permissionTreeView = set;
    }

    @Override
    public boolean isShowGroupTreeView() {
        return groupTreeView;
    }

    @Override
    public boolean isShowPermissionTreeView() {
        return permissionTreeView;
    }

    @Override
    public List<GroupModel> getGroups() {
        return userGroups;
    }

    @Override
    public String getGroupHeader() {
        return "Groups for " + (getSelectedUser() != null ? getSelectedUser().getUsername() : "");
    }

    @Override
    public TreeNode getGroupRoot() {
        return groupRoot;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public void setNewUserName(String newUserName) {
        this.newUserName = newUserName;
    }

}
