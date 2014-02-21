/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.user;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.security.jacc.PolicyContextException;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.integration.service.UserGroupDataAccess;
import com.blazebit.security.model.Permission;
import com.blazebit.security.web.bean.base.GroupHandlingBaseBean;
import com.blazebit.security.web.bean.main.resources.ResourceObjectBean;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.UserService;
import com.blazebit.security.web.util.WebUtil;

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

    @Inject
    private UserContext userContext;

    @Inject
    private ResourceObjectBean resourceObjectBean;

    @Inject
    private UserGroupDataAccess userGroupDataAccess;

    private List<User> users = new ArrayList<User>();
    private List<User> actAsUsers = new ArrayList<User>();
    private User selectedUser;
    private TreeNode permissionRoot;
    private TreeNode groupRoot;
    private DefaultTreeNode actAsGroupRoot;
    private List<UserGroup> actAsGroups;
    private List<UserGroupModel> groups = new ArrayList<UserGroupModel>();
    private User newUser = new User();
    private TreeNode[] selectedGroups = new TreeNode[] {};

    // loads all users, excludes logged in user
    public void init() throws PolicyContextException {
        Company selectedCompany = userSession.getSelectedCompany();
        users = userService.findUsers(selectedCompany);
        users.remove(userContext.getUser());
        if (getSelectedUser() != null) {
            selectUser(getSelectedUser());
        }
        this.actAsUsers = new ArrayList<User>(users);
        this.actAsUsers.remove(userSession.getSelectedUser());
        initUserGroups();
    }

    private void initUserGroups() {
        // init groups tree
        if (isEnabled(Company.GROUP_HIERARCHY)) {
            List<UserGroup> parentGroups = userGroupDataAccess.getAllParentGroups(userSession.getSelectedCompany());
            this.actAsGroupRoot = new DefaultTreeNode("", null);
            actAsGroupRoot.setExpanded(true);
            for (UserGroup group : parentGroups) {
                createNode(group, actAsGroupRoot);
            }
        }
        actAsGroups = userGroupDataAccess.getAllGroups(userSession.getSelectedCompany());
    }

    /**
     * helper to build tree
     * 
     * @param group
     * @param node
     */
    private void createNode(UserGroup group, DefaultTreeNode node) {
        DefaultTreeNode childNode = new DefaultTreeNode(group, node);
        childNode.setExpanded(true);
        for (UserGroup child : userGroupDataAccess.getGroupsForGroup(group)) {
            createNode(child, childNode);
        }
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
        this.actAsUsers = new ArrayList<User>(users);
        this.actAsUsers.remove(userSession.getSelectedUser());
    }

    // saves new user
    public void saveUser() {
        if (!StringUtils.isEmpty(newUser.getUsername())) {
            userService.createUser(userSession.getSelectedCompany(), newUser.getUsername());
            users = userService.findUsers(userSession.getSelectedCompany());
            users.remove(userContext.getUser());
        }
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

    public List<User> getActAsUsers() {
        return actAsUsers;
    }

    public void setActAsUsers(List<User> actAsUsers) {
        this.actAsUsers = actAsUsers;
    }

    public void grantActAsForUser() {
        grantRevokeObjectPermissionActAsForUsers("grant");
    }

    public void revokeActAsForUser() {
        grantRevokeObjectPermissionActAsForUsers("revoke");
    }

    public void grantActAsForGroup() {
        grantRevokeObjectPermissionActAsForGroups("grant");
    }

    public void revokeActAsForGroup() {
        grantRevokeObjectPermissionActAsForGroups("revoke");
    }

    private void grantRevokeObjectPermissionActAsForUsers(String action) {
        resourceObjectBean.setAction(action);
        resourceObjectBean.setSelectedSubject(userSession.getSelectedUser());
        List<EntityAction> actions = new ArrayList<EntityAction>();
        actions.add((EntityAction) actionFactory.createAction(ActionConstants.ACT_AS));
        resourceObjectBean.setSelectedActions(actions);
        resourceObjectBean.setPrevPath(FacesContext.getCurrentInstance().getViewRoot().getViewId());
        for (User user : actAsUsers) {
            if (user.isSelected()) {
                resourceObjectBean.getSelectedObjects().add(new RowModel(user, "User:" + user.getUsername()));

            }
        }
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/main/resource/object_resources.xhtml", false);
    }

    private void grantRevokeObjectPermissionActAsForGroups(String action) {
        resourceObjectBean.setAction(action);
        resourceObjectBean.setSelectedSubject(userSession.getSelectedUser());
        List<EntityAction> actions = new ArrayList<EntityAction>();
        actions.add((EntityAction) actionFactory.createAction(ActionConstants.ACT_AS));
        resourceObjectBean.setSelectedActions(actions);
        resourceObjectBean.setPrevPath(FacesContext.getCurrentInstance().getViewRoot().getViewId());
        if (isEnabled(Company.GROUP_HIERARCHY)) {
            for (TreeNode groupNode : selectedGroups) {
                resourceObjectBean.getSelectedObjects().add(new RowModel((UserGroup) groupNode.getData(), "UserGroup:" + ((UserGroup) groupNode.getData()).getName()));
            }
        } else {
            for (UserGroup group : actAsGroups) {
                if (group.isSelected()){
                    resourceObjectBean.getSelectedObjects().add(new RowModel(group, "UserGroup:" + group.getName()));
                }
            }
        }
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/main/resource/object_resources.xhtml", false);
    }

    public DefaultTreeNode getActAsGroupRoot() {
        return actAsGroupRoot;
    }

    public void setActAsGroupRoot(DefaultTreeNode actAsGroupRoot) {
        this.actAsGroupRoot = actAsGroupRoot;
    }

    public List<UserGroup> getActAsGroups() {
        return actAsGroups;
    }

    public void setActAsGroups(List<UserGroup> actAsGroups) {
        this.actAsGroups = actAsGroups;
    }

    public TreeNode[] getSelectedGroups() {
        return selectedGroups;
    }

    public void setSelectedGroups(TreeNode[] selectedGroups) {
        this.selectedGroups = selectedGroups;
    }

}
