/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.base.GroupHandlingBaseBean;
import com.blazebit.security.web.service.api.UserGroupService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "groupBean")
@ViewScoped
public class GroupBean extends GroupHandlingBaseBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    private UserGroupService userGroupService;

    private List<UserGroup> groups = new ArrayList<UserGroup>();
    private List<User> users = new ArrayList<User>();
    private DefaultTreeNode groupRoot;
    private UserGroup selectedGroup;
    private DefaultTreeNode permissionRoot;
    private UserGroup newGroup = new UserGroup("new_group");
    private TreeNode selectedGroupTreeNode;
    private boolean parentGroup;

    public void backToIndex() throws IOException {
        userSession.setUser(null);
        ((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false)).invalidate();
        FacesContext.getCurrentInstance().getExternalContext().redirect("../index.xhtml");
        FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    @PostConstruct
    public void init() {
        initUserGroups();
    }

    private void initUserGroups() {
        // init groups tree
        if (isEnabled(Company.GROUP_HIERARCHY)) {
            List<UserGroup> parentGroups = userGroupDataAccess.getAllParentGroups(userSession.getSelectedCompany());
            this.groupRoot = new DefaultTreeNode("", null);
            groupRoot.setExpanded(true);
            for (UserGroup group : parentGroups) {
                createNode(group, groupRoot);
            }
        } else {
            groups = userGroupDataAccess.getAllGroups(userSession.getSelectedCompany());
        }
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

    public void saveGroup() {
        UserGroup newGroup = userGroupService.create(userSession.getSelectedCompany(), this.newGroup.getName());
        if (isEnabled(Company.GROUP_HIERARCHY)) {
            if (isParentGroup()) {
                newGroup.setParent(getSelectedGroup().getParent());
                getSelectedGroup().setParent(newGroup);
                userGroupService.save(getSelectedGroup());
                userGroupService.save(newGroup);
            } else {
                newGroup.setParent(getSelectedGroup());
                userGroupService.save(newGroup);
            }
        }
        // add permission to grant/revoke
        Set<Permission> grant = new HashSet<Permission>();
        grant.add(permissionFactory.create(actionFactory.createAction(ActionConstants.GRANT), createResource(newGroup)));
        grant.add(permissionFactory.create(actionFactory.createAction(ActionConstants.REVOKE), createResource(newGroup)));
        revokeAndGrant(userSession.getAdmin(), userSession.getUser(), new HashSet<Permission>(), grant, false);
        // reset
        initUserGroups();
        newGroup = new UserGroup();

    }

    public UserGroup getSelectedGroup() {
        return userSession.getSelectedUserGroup();
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public DefaultTreeNode getGroupRoot() {
        return groupRoot;
    }

    public void selectGroup() {
        if (selectedGroupTreeNode != null) {
            selectedGroup = (UserGroup) selectedGroupTreeNode.getData();
            initSelectedGroup();
        }
    }

    public void selectGroup(UserGroup userGroup) {
        selectedGroup = userGroup;
        initSelectedGroup();
    }

    public void onGroupSelect(SelectEvent event) {
        selectGroup((UserGroup) event.getObject());
    }

    private void initSelectedGroup() {
        userSession.setSelectedUserGroup(selectedGroup);
        this.users = userGroupDataAccess.getUsersFor(selectedGroup);
        permissionRoot = initGroupPermissions(getSelectedGroup(), !isEnabled(Company.FIELD_LEVEL));
    }

    public void unselectGroup() {
        selectedGroupTreeNode = null;
        userSession.setSelectedUserGroup(null);
        this.users.clear();
    }

    public void deleteGroup(UserGroup group) {
        if (group.equals(userSession.getSelectedUserGroup())) {
            userSession.setSelectedUserGroup(null);
            this.users = new ArrayList<User>();
            this.permissionRoot = new DefaultTreeNode("root", null);
        }
        userGroupService.delete(group);
        initUserGroups();
    }

    public TreeNode getPermissionViewRoot() {
        return this.permissionRoot;
    }

    public TreeNode getSelectedGroupTreeNode() {
        return selectedGroupTreeNode;
    }

    public void setSelectedGroupTreeNode(TreeNode selectedGroupTreeNode) {
        this.selectedGroupTreeNode = selectedGroupTreeNode;
        selectGroup();
    }

    public UserGroup getNewGroup() {
        return newGroup;
    }

    public void setNewGroup(UserGroup newGroup) {
        this.newGroup = newGroup;
    }

    public boolean isParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(boolean parentGroup) {
        newGroup.setName("");
        this.parentGroup = parentGroup;
    }

    public List<UserGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<UserGroup> groups) {
        this.groups = groups;
    }

}
