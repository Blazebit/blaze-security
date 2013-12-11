/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.user;

import java.io.IOException;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebRoleRefPermission;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.base.GroupHandlingBaseBean;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "userBean")
@ViewScoped
public class UserBean extends GroupHandlingBaseBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private UserService userService;

    private List<User> users = new ArrayList<User>();
    private User selectedUser;
    private TreeNode permissionRoot;
    private TreeNode groupRoot;
    private List<UserGroupModel> groups = new ArrayList<UserGroupModel>();
    private User newUser = new User();

    // redirects to start page
    public void backToIndex() throws IOException {
        userSession.setUser(null);
        ((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false)).invalidate();
        FacesContext.getCurrentInstance().getExternalContext().redirect("../../index.xhtml");
        FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    // loads all users, excludes logged in user
    @PostConstruct
    public void init() throws PolicyContextException {
        users = userService.findUsers(userSession.getSelectedCompany());
        users.remove(userContext.getUser());
        if (getSelectedUser() != null) {
            selectUser(getSelectedUser());
        }

        // test JACC

        Policy policy = Policy.getPolicy();
        HttpServletRequest request = (HttpServletRequest) PolicyContext.getContext(HttpServletRequest.class.getName());
        Principal principal = request.getUserPrincipal();

        CodeSource cs = new CodeSource(null, (java.security.cert.Certificate[]) null);
        Principal principals[] = new Principal[] { principal };
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, principals);

        PermissionCollection pc = policy.getPermissions(pd);
        pc.implies(new WebRoleRefPermission("a", "b"));

    }

    // first tab: select user -> display groups and permissions of selected user
    public void selectUser(User user) {
        this.selectedUser = user;
        this.userSession.setSelectedUser(selectedUser);

        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
        List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);

        this.permissionRoot = new DefaultTreeNode("root", null);
        permissionRoot = getImmutablePermissionTree(userPermissions, userDataPermissions, !isEnabled(Company.FIELD_LEVEL));

        List<UserGroup> groups = userGroupDataAccess.getGroupsForUser(selectedUser);
        this.groupRoot = buildGroupTree(groups);
        this.groups.clear();
        for (UserGroup group : groups) {
            this.groups.add(new UserGroupModel(group, false, false));
        }
    }

    // saves new user
    public void saveUser() {
        userService.createUser(userSession.getSelectedCompany(), newUser.getUsername());
        users = userService.findUsers(userSession.getSelectedCompany());
        users.remove(userContext.getUser());
    }

    public void deleteUser(User user) {
        if (user.equals(userSession.getSelectedUser())) {
            userSession.setSelectedUser(null);
        }
        userService.delete(user);
        users = userService.findUsers(userSession.getSelectedCompany());
        users.remove(userContext.getUser());
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

    public TreeNode getPermissionViewRoot() {
        return permissionRoot;
    }

    public TreeNode getGroupRoot() {
        return groupRoot;
    }

    public User getNewUser() {
        return newUser;
    }

    public void setNewUser(User newUser) {
        this.newUser = newUser;
    }

    public List<UserGroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<UserGroupModel> groups) {
        this.groups = groups;
    }

}
