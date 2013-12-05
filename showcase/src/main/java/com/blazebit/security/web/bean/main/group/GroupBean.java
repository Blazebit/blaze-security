/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private DefaultTreeNode newPermissionRoot;
    private DefaultTreeNode currentPermissionRoot;

    private boolean deletedGroup;

    private List<Permission> groupPermissions;

    private Map<User, Set<Permission>> currentRevokedUserMap = new HashMap<User, Set<Permission>>();
    private TreeNode[] selectedUserNodes = new TreeNode[] {};

    private UserGroup groupToDelete;

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
        if (!isAuthorizedResource(ActionConstants.GRANT, newGroup)) {
            Set<Permission> grant = new HashSet<Permission>();
            grant.add(permissionFactory.create(actionFactory.createAction(ActionConstants.GRANT), createResource(newGroup)));
            grant.add(permissionFactory.create(actionFactory.createAction(ActionConstants.REVOKE), createResource(newGroup)));
            revokeAndGrant(userSession.getAdmin(), userSession.getUser(), new HashSet<Permission>(), grant, false);
        }
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
        deletedGroup = true;
        groupToDelete = group;
        prepareUserView();
    }

    private void prepareUserView() {
        currentPermissionRoot = new DefaultTreeNode();
        newPermissionRoot = new DefaultTreeNode();

        groupPermissions = permissionManager.getPermissions(groupToDelete);
        List<User> users = userGroupDataAccess.collectUsers(groupToDelete, isEnabled(Company.GROUP_HIERARCHY));

        for (User user : users) {
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
            List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
            // user will be removed from group-> mark it red
            TreeNode currentUserNode = createCurrentUserNode(currentPermissionRoot, user, userPermissions, userDataPermissions);
            TreeNode newUserNode = createNewUserNode(newPermissionRoot, user, userPermissions, userDataPermissions);
            ((TreeNodeModel) currentUserNode.getData()).setMarking(Marking.REMOVED);
            ((TreeNodeModel) newUserNode.getData()).setMarking(Marking.REMOVED);
        }
        if (!isEnabled(Company.USER_LEVEL)) {
            confirmPermissions();
        }
    }

    private TreeNode createCurrentUserNode(DefaultTreeNode root, User user, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);
        Set<Permission> revoked = permissionHandling.getRevokableFromRevoked(userPermissions, groupPermissions, true).get(0);
        getImmutablePermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);
        return userNode;
    }

    private TreeNode createNewUserNode(DefaultTreeNode root, User user, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);
        Set<Permission> revoked = permissionHandling.getRevokableFromRevoked(concat(userPermissions, userDataPermissions), groupPermissions, true).get(0);
        Set<Permission> granted = permissionHandling.getRevokableFromRevoked(concat(userPermissions, userDataPermissions), groupPermissions, true).get(2);
        // when user has entity permission and group has field permission, we have to build the tree in a way that the group
        // entity field permissions can be revoked and the rest of the fields can be kept.
        Set<Permission> impliedBy = new HashSet<Permission>();
        Set<Permission> toRevoke = new HashSet<Permission>();
        if (!granted.isEmpty()) {
            for (Permission permission : groupPermissions) {
                if (!permissionHandling.contains(revoked, permission) && permissionHandling.implies(revoked, permission)) {
                    impliedBy.addAll(permissionDataAccess.getImpliedBy(new ArrayList<Permission>(revoked), permission.getAction(), permission.getResource()));
                    toRevoke.add(permission);
                }
            }
        }
        revoked = new HashSet<Permission>(permissionHandling.removeAll(revoked, impliedBy));
        revoked.addAll(toRevoke);
        currentRevokedUserMap.put(user, revoked);

        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        currentPermissions.addAll(granted);
        currentPermissions.addAll(toRevoke);

        currentPermissions = new HashSet<Permission>(permissionHandling.removeAll(currentPermissions, impliedBy));

        if (isEnabled(Company.USER_LEVEL)) {
            getMutablePermissionTree(userNode, new ArrayList<Permission>(currentPermissions), userDataPermissions, granted, revoked, Marking.NEW, Marking.REMOVED);
        } else {
            List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
            currentUserPermissions = (List<Permission>) permissionHandling.removeAll(currentUserPermissions, revoked);
            currentUserPermissions.addAll(granted);
            getImmutablePermissionTree(userNode, currentUserPermissions, new ArrayList<Permission>(), new HashSet<Permission>(currentUserPermissions), Marking.NONE);
        }
        return userNode;
    }

    /**
     * listener for select unselect permissons in the new permission tree
     */
    public void rebuildCurrentPermissionTree() {
        for (TreeNode userNode : currentPermissionRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();
            List<Permission> permissions = permissionManager.getPermissions(user);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);
            userNode.getChildren().clear();
            // this is a removed user
            rebuildCurrentTree(userNode, permissions, selectedPermissions, currentRevokedUserMap.get(user), new HashSet<Permission>(), !isEnabled(Company.FIELD_LEVEL));
        }
    }

    /**
     * confirm button
     */
    public void confirmPermissions() {
        for (TreeNode userNode : newPermissionRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();

            List<Permission> allPermissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);

            // remove user and remove unselected resources too
            executeRevokeAndGrant(user, userPermissions, selectedPermissions, currentRevokedUserMap.get(user), new HashSet<Permission>());
            userGroupService.removeUserFromGroup(user, groupToDelete);
        }
        // reset
        deletedGroup = false;
        userGroupService.delete(groupToDelete);
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

    public boolean isDeletedGroup() {
        return deletedGroup;
    }

    public void setDeletedGroup(boolean deletedGroup) {
        this.deletedGroup = deletedGroup;
    }

    public DefaultTreeNode getNewPermissionRoot() {
        return newPermissionRoot;
    }

    public void setNewPermissionRoot(DefaultTreeNode newPermissionRoot) {
        this.newPermissionRoot = newPermissionRoot;
    }

    public DefaultTreeNode getCurrentPermissionRoot() {
        return currentPermissionRoot;
    }

    public void setCurrentPermissionRoot(DefaultTreeNode currentPermissionRoot) {
        this.currentPermissionRoot = currentPermissionRoot;
    }

    public TreeNode[] getSelectedUserNodes() {
        return selectedUserNodes;
    }

    public void setSelectedUserNodes(TreeNode[] selectedUserNodes) {
        this.selectedUserNodes = selectedUserNodes;
    }

}
