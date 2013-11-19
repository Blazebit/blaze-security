/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

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
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionTreeHandlingBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.service.api.UserGroupService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "groupBean")
@ViewScoped
public class GroupBean extends PermissionTreeHandlingBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    private UserGroupService userGroupService;

    private List<User> users = new ArrayList<User>();

    private DefaultTreeNode groupRoot;
    private UserGroup selectedGroup;
    private DefaultTreeNode permissionRoot;
    private UserGroup newGroup = new UserGroup("new_group");
    private TreeNode selectedGroupTreeNode;

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
        List<UserGroup> availableGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
        this.groupRoot = new DefaultTreeNode("", null);
        groupRoot.setExpanded(true);
        for (UserGroup group : availableGroups) {
            createNode(group, groupRoot);
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
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createNode(child, childNode);
        }
    }

    public void saveGroup() {
        UserGroup ug = userGroupService.createUserGroup(userSession.getSelectedCompany(), newGroup.getName());
        if (userSession.getSelectedCompany().isGroupHierarchyEnabled()) {
            if (getSelectedGroup() != null) {
                ug.setParent(getSelectedGroup());
                userGroupService.saveGroup(ug);
            }
        }

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
            userSession.setSelectedUserGroup(selectedGroup);
            this.users = userGroupService.getUsersFor(selectedGroup);
            initPermissions();
        }
    }

    public void unselectGroup() {
        selectedGroupTreeNode = null;
        userSession.setSelectedUserGroup(null);
        this.users.clear();
    }

    private void initPermissions() {
        List<UserGroup> parents = new ArrayList<UserGroup>();
        UserGroup parent = getSelectedGroup().getParent();
        parents.add(getSelectedGroup());
        while (parent != null) {
            parents.add(0, parent);
            parent = parent.getParent();
        }
        this.permissionRoot = new DefaultTreeNode("root", null);
        TreeNode groupNode = permissionRoot;
        for (UserGroup group : parents) {
            groupNode = new DefaultTreeNode(new TreeNodeModel(group.getName(), ResourceType.USERGROUP, group), groupNode);
            groupNode.setExpanded(true);
            groupNode.setSelectable(false);
            List<Permission> allPermissions = permissionManager.getPermissions(group);
            List<Permission> permissions = filterPermissions(allPermissions).get(0);
            List<Permission> dataPermissions = filterPermissions(allPermissions).get(1);
            getPermissionTree(groupNode, permissions, dataPermissions);
            if (getSelectedGroup().equals(group)) {
                ((TreeNodeModel) groupNode.getData()).setMarking(Marking.SELECTED);
            }
        }
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

    @Override
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

}
