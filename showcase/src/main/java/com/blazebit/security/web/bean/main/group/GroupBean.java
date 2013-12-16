/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.primefaces.event.TreeDragDropEvent;
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
    private DefaultTreeNode newObjectPermissionRoot;
    private DefaultTreeNode currentPermissionRoot;

    private boolean deletedGroup;
    private boolean movedGroup;

    private Map<User, Set<Permission>> initialRevoke = new HashMap<User, Set<Permission>>();
    private Map<User, Set<Permission>> initialReplace = new HashMap<User, Set<Permission>>();
    private TreeNode[] selectedUserNodes = new TreeNode[] {};
    private TreeNode[] selectedObjectUserNodes = new TreeNode[] {};

    private UserGroup groupToDelete;
    private UserGroup groupToMove;
    private UserGroup newParent;

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
            revokeAndGrant(userSession.getAdmin(), userContext.getUser(), new HashSet<Permission>(), grant, false);
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

    /**
     * triggers user propagation
     * 
     * @param group
     */
    public void deleteGroup(UserGroup group) {
        if (group.equals(userSession.getSelectedUserGroup())) {
            userSession.setSelectedUserGroup(null);
            this.users = new ArrayList<User>();
            this.permissionRoot = new DefaultTreeNode("root", null);
        }
        groupToDelete = group;
        List<User> users = userGroupDataAccess.collectUsers(groupToDelete, isEnabled(Company.GROUP_HIERARCHY));
        if (!users.isEmpty()) {
            deletedGroup = true;
            prepareUserViewForGroupDelete(users);
        } else {
            userGroupService.delete(groupToDelete);
        }
    }

    private void prepareUserViewForGroupDelete(List<User> users) {
        currentPermissionRoot = new DefaultTreeNode();
        newPermissionRoot = new DefaultTreeNode();
        newObjectPermissionRoot = new DefaultTreeNode();

        Set<Permission> permissionsToRevoke = getPermissionsToRevoke(groupToDelete);

        for (User user : users) {
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
            List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
            // user will be removed from group-> mark it red
            createUserNode(TreeType.CURRENT, currentPermissionRoot, null, user, userPermissions, userDataPermissions, new HashSet<Permission>(), permissionsToRevoke,
                           Marking.REMOVED);
            createUserNode(TreeType.NEW, newPermissionRoot, newObjectPermissionRoot, user, userPermissions, userDataPermissions, new HashSet<Permission>(), permissionsToRevoke,
                           Marking.REMOVED);
        }
        if (!isEnabled(Company.USER_LEVEL)) {
            confirmPermissions();
        }
    }

    private Set<Permission> getPermissionsToRevoke(UserGroup selectedGroup) {
        Set<Permission> revoked = new HashSet<Permission>(permissionManager.getPermissions(selectedGroup));
        // if field level is not enabled dont revoke field permissions
        if (!isEnabled(Company.FIELD_LEVEL)) {
            revoked = permissionHandling.getSeparatedParentAndChildPermissions(revoked).get(0);
        }
        // if object level is not enabled, just ignore object level permissions
        if (!isEnabled(Company.OBJECT_LEVEL)) {
            revoked = new HashSet<Permission>(permissionHandling.getSeparatedPermissions(revoked).get(0));
        }
        return revoked;
    }

    /**
     * group change event
     * 
     * @param event
     */
    public void onDragDrop(TreeDragDropEvent event) {
        TreeNode dragNode = event.getDragNode();
        groupToMove = (UserGroup) dragNode.getData();

        TreeNode dropNode = event.getDropNode();
        if (dropNode.getData() instanceof UserGroup) {
            newParent = (UserGroup) dropNode.getData();
        } else {
            newParent = null;
        }

        if ((newParent == null && groupToMove.getParent() == null) || (newParent != null && newParent.equals(groupToMove.getParent()))) {
            // nothing has changed
        } else {
            List<User> users = userGroupDataAccess.collectUsers(groupToMove, isEnabled(Company.GROUP_HIERARCHY));
            if (!users.isEmpty()) {
                prepareUserViewForGroupDragDrop(users, groupToMove, groupToMove.getParent(), newParent);
                movedGroup = true;
            } else {
                // just save new group position
                movedGroup = false;
                groupToMove.setParent(newParent);
                userGroupService.save(groupToMove);
                initUserGroups();
            }
        }
    }

    private void prepareUserViewForGroupDragDrop(List<User> users, UserGroup selectedGroup, UserGroup oldParent, UserGroup newParent) {
        currentPermissionRoot = new DefaultTreeNode();
        newPermissionRoot = new DefaultTreeNode();
        newObjectPermissionRoot = new DefaultTreeNode();

        Set<UserGroup> newGroups = new HashSet<UserGroup>();
        if (newParent != null) {
            newGroups.add(newParent);
        }
        Set<UserGroup> oldGroups = new HashSet<UserGroup>();
        if (oldParent != null) {
            oldGroups.add(oldParent);
        }

        // get granted and revoked permissions from added and removed groups
        Set<Permission> granted = getPermissionsToGrant(newGroups);
        Set<Permission> revoked = getPermissionsToRevoke(oldGroups, granted);

        for (User user : users) {
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
            List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
            // user will be removed from group-> mark it red

            createUserNode(TreeType.CURRENT, currentPermissionRoot, null, user, userPermissions, userDataPermissions, granted, revoked, Marking.REMOVED);
            createUserNode(TreeType.NEW, newPermissionRoot, newObjectPermissionRoot, user, userPermissions, userDataPermissions, granted, revoked, Marking.REMOVED);

        }
        if (!isEnabled(Company.USER_LEVEL)) {
            confirmPermissions();
        }
    }

    private Set<Permission> getPermissionsToRevoke(Set<UserGroup> oldGroups, Set<Permission> granted) {
        Set<Permission> revoked = groupPermissionHandling.getGroupPermissions(oldGroups);
        if (!isEnabled(Company.FIELD_LEVEL)) {
            revoked = permissionHandling.getSeparatedParentAndChildPermissions(revoked).get(0);
        }
        // if object level is not enabled, just ignore object level permissions
        if (!isEnabled(Company.OBJECT_LEVEL)) {
            revoked = new HashSet<Permission>(permissionHandling.getSeparatedPermissions(revoked).get(0));
        }
        revoked = permissionHandling.eliminateRevokeConflicts(granted, revoked);
        return revoked;
    }

    private Set<Permission> getPermissionsToGrant(Set<UserGroup> newGroups) {
        Set<Permission> granted = groupPermissionHandling.getGroupPermissions(newGroups);
        if (!isEnabled(Company.FIELD_LEVEL)) {
            granted = permissionHandling.getSeparatedParentAndChildPermissions(granted).get(0);
        }
        // if object level is not enabled, just ignore object level permissions
        if (!isEnabled(Company.OBJECT_LEVEL)) {
            granted = new HashSet<Permission>(permissionHandling.getSeparatedPermissions(granted).get(0));
        }
        return granted;
    }

    private enum TreeType {
        CURRENT,
        NEW;
    }

    private TreeNode createUserNode(TreeType type, DefaultTreeNode root, DefaultTreeNode objectRoot, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> groupGranted, Set<Permission> groupRevoked, Marking marking) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user, marking);
        TreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        TreeNode userObjectNode = null;
        if (objectRoot != null) {
            userObjectNode = new DefaultTreeNode(userNodeModel, objectRoot);
            userObjectNode.setExpanded(true);
            userObjectNode.setSelectable(false);
        }

        Set<Permission> allPermissions = concat(userPermissions, userDataPermissions);
        // get permissions that can be revoked
        List<Set<Permission>> revoke = permissionHandling.getRevokableFromRevoked(allPermissions, groupRevoked, true);
        Set<Permission> revoked = revoke.get(0);
        initialRevoke.put(user, revoked);
        dialogBean.setNotRevoked(revoke.get(1));

        // get permissions which can be granted to the user
        List<Set<Permission>> grant = permissionHandling.getGrantable(permissionHandling.removeAll(allPermissions, revoked), groupGranted);
        Set<Permission> grantable = grant.get(0);
        dialogBean.setNotGranted(grant.get(1));

        Set<Permission> additionalGranted = revoke.get(2);
        grantable.addAll(additionalGranted);
        grantable = permissionHandling.getNormalizedPermissions(grantable);
        // current permission tree
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(allPermissions, grantable);
        initialReplace.put(user, replaced);

        switch (type) {
            case CURRENT:
                return buildCurrentPermissionTree(userNode, userPermissions, userDataPermissions, revoked, replaced, !isEnabled(Company.FIELD_LEVEL));
            case NEW:
                buildNewPermissionTree(userNode, userPermissions, Collections.EMPTY_LIST, new HashSet<Permission>(permissionHandling.getSeparatedPermissions(grantable).get(0)),
                                       new HashSet<Permission>(permissionHandling.getSeparatedPermissions(revoked).get(0)), replaced, !isEnabled(Company.FIELD_LEVEL),
                                       isEnabled(Company.USER_LEVEL), true);
                buildNewDataPermissionTree(userObjectNode, Collections.EMPTY_LIST, userDataPermissions,
                                           new HashSet<Permission>(permissionHandling.getSeparatedPermissions(grantable).get(1)), new HashSet<Permission>(permissionHandling
                                               .getSeparatedPermissions(revoked)
                                               .get(1)), replaced, !isEnabled(Company.FIELD_LEVEL), isEnabled(Company.USER_LEVEL));
                return null;
        }
        return null;
    }

    public void rebuildCurrentPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        TreeNode selectedNode = event.getTreeNode();
        selectChildrenInstances(selectedNode, true);
        rebuildCurrentPermissionTree();
    }

    public void rebuildCurrentPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
        TreeNode selectedNode = event.getTreeNode();
        selectChildrenInstances(selectedNode, false);
        rebuildCurrentPermissionTree();
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
            selectedPermissions.addAll(getSelectedPermissions(selectedObjectUserNodes, userNode));
            userNode.getChildren().clear();
            // this is a removed user
            rebuildCurrentTree(userNode, permissions, selectedPermissions, initialRevoke.get(user), initialReplace.get(user), !isEnabled(Company.FIELD_LEVEL));
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
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);
            selectedPermissions.addAll(getSelectedPermissions(selectedObjectUserNodes, userNode));
            // remove user and remove unselected resources too
            executeRevokeAndGrant(user, allPermissions, selectedPermissions, initialRevoke.get(user), initialReplace.get(user));
            if (deletedGroup) {
                userGroupService.removeUserFromGroup(user, groupToDelete);
            }
        }
        // reset
        if (deletedGroup) {
            deletedGroup = false;
            userGroupService.delete(groupToDelete);
        }
        if (movedGroup) {
            movedGroup = false;
            groupToMove.setParent(newParent);
            userGroupService.save(groupToMove);
        }
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

    public boolean isMovedGroup() {
        return movedGroup;
    }

    public void setMovedGroup(boolean movedGroup) {
        this.movedGroup = movedGroup;
    }

    public DefaultTreeNode getNewObjectPermissionRoot() {
        return newObjectPermissionRoot;
    }

    public void setNewObjectPermissionRoot(DefaultTreeNode newObjectPermissionRoot) {
        this.newObjectPermissionRoot = newObjectPermissionRoot;
    }

    public TreeNode[] getSelectedObjectUserNodes() {
        return selectedObjectUserNodes;
    }

    public void setSelectedObjectUserNodes(TreeNode[] selectedObjectUserNodes) {
        this.selectedObjectUserNodes = selectedObjectUserNodes;
    }

}
