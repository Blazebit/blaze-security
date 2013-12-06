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
    private DefaultTreeNode currentPermissionRoot;

    private boolean deletedGroup;
    private boolean movedGroup;

    private Map<User, Set<Permission>> currentRevokedUserMap = new HashMap<User, Set<Permission>>();
    private Map<User, Set<Permission>> currentReplacedUserMap = new HashMap<User, Set<Permission>>();
    private TreeNode[] selectedUserNodes = new TreeNode[] {};

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
        prepareUserViewForGroupDelete();
    }

    private void prepareUserViewForGroupDelete() {
        currentPermissionRoot = new DefaultTreeNode();
        newPermissionRoot = new DefaultTreeNode();

        List<Permission> groupPermissions = permissionManager.getPermissions(groupToDelete);
        List<User> users = userGroupDataAccess.collectUsers(groupToDelete, isEnabled(Company.GROUP_HIERARCHY));

        for (User user : users) {
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
            List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
            // user will be removed from group-> mark it red
            TreeNode currentUserNode = createCurrentUserNode(currentPermissionRoot, user, userPermissions, userDataPermissions, new HashSet<Permission>(), new HashSet<Permission>(
                groupPermissions));
            TreeNode newUserNode = createNewUserNode(newPermissionRoot, user, userPermissions, userDataPermissions, new HashSet<Permission>(), new HashSet<Permission>(
                groupPermissions));
            ((TreeNodeModel) currentUserNode.getData()).setMarking(Marking.REMOVED);
            ((TreeNodeModel) newUserNode.getData()).setMarking(Marking.REMOVED);
        }
        if (!isEnabled(Company.USER_LEVEL)) {
            confirmPermissions();
        }
    }

    private void prepareUserViewForGroupDragDrop(UserGroup selectedGroup, UserGroup oldParent, UserGroup newParent) {
        currentPermissionRoot = new DefaultTreeNode();
        newPermissionRoot = new DefaultTreeNode();

        Set<UserGroup> newGroups = new HashSet<UserGroup>();
        if (newParent != null) {
            newGroups.add(newParent);
        }
        Set<UserGroup> oldGroups = new HashSet<UserGroup>();
        if (oldParent != null) {
            oldGroups.add(oldParent);
        }

        // get granted and revoked permissions from added and removed groups
        Set<Permission> granted = groupPermissionHandling.getGroupPermissions(newGroups);
        if (!isEnabled(Company.FIELD_LEVEL)) {
            granted = permissionHandling.getParentPermissions(granted);
        }
        // if object level is not enabled, just ignore object level permissions
        if (!isEnabled(Company.OBJECT_LEVEL)) {
            granted = new HashSet<Permission>(permissionHandling.getSeparatedPermissions(granted).get(0));
        }

        Set<Permission> revoked = groupPermissionHandling.getGroupPermissions(oldGroups);
        // TODO do the same as with the granted?
        revoked = permissionHandling.eliminateRevokeConflicts(granted, revoked);

        List<User> users = userGroupDataAccess.collectUsers(selectedGroup, isEnabled(Company.GROUP_HIERARCHY));

        for (User user : users) {
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
            List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
            // user will be removed from group-> mark it red
            TreeNode currentUserNode = createCurrentUserNode(currentPermissionRoot, user, userPermissions, userDataPermissions, granted, revoked);
            TreeNode newUserNode = createNewUserNode(newPermissionRoot, user, userPermissions, userDataPermissions, granted, revoked);
            ((TreeNodeModel) currentUserNode.getData()).setMarking(Marking.REMOVED);
            ((TreeNodeModel) newUserNode.getData()).setMarking(Marking.REMOVED);
        }
        if (!isEnabled(Company.USER_LEVEL)) {
            confirmPermissions();
        }
    }

    private TreeNode createCurrentUserNode(DefaultTreeNode root, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> groupGranted, Set<Permission> groupRevoked) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        Set<Permission> allPermissions = concat(userPermissions, userDataPermissions);
        List<Set<Permission>> revoke = permissionHandling.getRevokableFromRevoked(allPermissions, groupRevoked, true);
        // put in usermap
        Set<Permission> revoked = revoke.get(0);
        currentRevokedUserMap.put(user, revoked);
        super.setNotRevoked(revoke.get(1));

        // get permissions which can be granted to the user
        List<Set<Permission>> grant = permissionHandling.getGrantable(permissionHandling.removeAll(allPermissions, groupRevoked), groupGranted);
        Set<Permission> grantable = grant.get(0);
        super.setNotGranted(grant.get(1));

        Set<Permission> additionalGranted = revoke.get(2);
        grantable.addAll(additionalGranted);
        grantable = permissionHandling.getNormalizedPermissions(grantable);
        // current permission tree
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(allPermissions, grantable);
        currentReplacedUserMap.put(user, replaced);

        Set<Permission> removable = new HashSet<Permission>();
        removable.addAll(replaced);
        removable.addAll(revoked);
        getImmutablePermissionTree(userNode, userPermissions, userDataPermissions, removable, Marking.REMOVED, !isEnabled(Company.FIELD_LEVEL));

        return userNode;
    }

    private TreeNode createNewUserNode(DefaultTreeNode root, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> groupGranted, Set<Permission> groupRevoked) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);

        Set<Permission> allPermissions = concat(userPermissions, userDataPermissions);
        List<Set<Permission>> revoke = permissionHandling.getRevokableFromRevoked(allPermissions, groupRevoked, true);
        Set<Permission> revoked = revoke.get(0);

        // get permissions which can be granted to the user
        List<Set<Permission>> grant = permissionHandling.getGrantable(permissionHandling.removeAll(allPermissions, groupRevoked), groupGranted);
        Set<Permission> grantable = grant.get(0);

        Set<Permission> additionalGranted = revoke.get(2);
        grantable.addAll(additionalGranted);
        grantable = permissionHandling.getNormalizedPermissions(grantable);

        // new permission tree
        List<Permission> currentPermissions = new ArrayList<Permission>(userPermissions);
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(userPermissions, grantable);
        currentPermissions = new ArrayList<Permission>(permissionHandling.removeAll(currentPermissions, replaced));
        currentPermissions.addAll(permissionHandling.getSeparatedPermissions(grantable).get(0));

        List<Permission> currentDataPermissions = new ArrayList<Permission>(userDataPermissions);
        replaced.addAll(permissionHandling.getReplacedByGranting(userDataPermissions, grantable));
        currentDataPermissions = new ArrayList<Permission>(permissionHandling.removeAll(currentDataPermissions, replaced));
        currentDataPermissions.addAll(permissionHandling.getSeparatedPermissions(grantable).get(1));

        getMutablePermissionTree(userNode, currentPermissions, currentDataPermissions, grantable, revoked, Marking.NEW, Marking.REMOVED);

        return userNode;
    }
    
    public void rebuildCurrentPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        rebuildCurrentPermissionTree();
    }

    public void rebuildCurrentPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
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
            userNode.getChildren().clear();
            // this is a removed user
            rebuildCurrentTree(userNode, permissions, selectedPermissions, currentRevokedUserMap.get(user), currentReplacedUserMap.get(user), !isEnabled(Company.FIELD_LEVEL));
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
            executeRevokeAndGrant(user, userPermissions, selectedPermissions, currentRevokedUserMap.get(user), currentReplacedUserMap.get(user));
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
            prepareUserViewForGroupDragDrop(groupToMove, groupToMove.getParent(), newParent);
        }

        int dropIndex = event.getDropIndex();
        movedGroup = true;
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

}
