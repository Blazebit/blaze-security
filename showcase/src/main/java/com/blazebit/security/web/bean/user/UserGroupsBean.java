/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.GroupHandlerBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.RoleService;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "userGroupsBean")
public class UserGroupsBean extends GroupHandlerBaseBean implements PermissionView, Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private RoleService roleService;

    private List<Permission> userPermissions = new ArrayList<Permission>();
    private List<Permission> userDataPermissions = new ArrayList<Permission>();
    private List<Permission> allPermissions = new ArrayList<Permission>();

    private List<UserGroup> currentUserGroups = new ArrayList<UserGroup>();
    // group tree
    private TreeNode[] selectedGroupNodes = new TreeNode[] {};
    private DefaultTreeNode groupRoot = new DefaultTreeNode();
    // permission view tree
    private TreeNode permissionViewRoot = new DefaultTreeNode();
    // selected group to view group permissions
    private UserGroup selectedGroup;

    // after group selection
    private Set<UserGroup> addedGroups = new HashSet<UserGroup>();
    private Set<UserGroup> removedGroups = new HashSet<UserGroup>();
    private Set<Permission> currentReplaced = new HashSet<Permission>();
    private Set<Permission> currentRevoked = new HashSet<Permission>();

    // compare 2 permission trees
    private TreeNode currentPermissionTreeRoot;
    private TreeNode newPermissionTreeRoot;
    // select from the new permission tree
    private TreeNode[] selectedPermissionNodes = new TreeNode[] {};

    private Map<UserGroup, List<Permission>> groupPermissionsMap = new HashMap<UserGroup, List<Permission>>();

    public void init() {
        initUserGroups();
        initUserPermissions();
    }

    /**
     * wizard listener
     * 
     * @param event
     * @return
     */
    public String groupWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("groups")) {
            processSelectedGroups();
        }
        if (event.getOldStep().equals("groupPermissions") && event.getNewStep().equals("groups")) {
            init();
        }
        return event.getNewStep();
    }

    private void initUserPermissions() {
        allPermissions = permissionManager.getPermissions(getSelectedUser());
        List<List<Permission>> permissions = permissionHandlingUtils.filterPermissions(allPermissions);
        userPermissions = permissions.get(0);
        userDataPermissions = permissions.get(1);
        this.permissionViewRoot = getPermissionTree(userPermissions, userDataPermissions);
    }

    private void initUserGroups() {
        this.currentUserGroups = userGroupService.getGroupsForUser(getSelectedUser());
        selectedGroupNodes = new TreeNode[] {};
        List<UserGroup> parentGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
        storeGroupPermissionMap(parentGroups);
        this.groupRoot = getGroupTree(parentGroups, currentUserGroups);
    }

    private void storeGroupPermissionMap(List<UserGroup> parentGroups) {
        for (UserGroup userGroup : parentGroups) {
            storeChildGroupPermissions(userGroup);
        }
    }

    private void storeChildGroupPermissions(UserGroup userGroup) {
        groupPermissionsMap.put(userGroup, permissionManager.getPermissions(userGroup));
        for (UserGroup child : userGroupService.getGroupsForGroup(userGroup)) {
            storeChildGroupPermissions(child);
        }
    }

    /**
     * wizard step: process selected groups
     */
    public void processSelectedGroups() {
        // store added and removed groups for later processing
        List<Set<UserGroup>> addedAndRemovedGroups = groupPermissionHandlingUtils.getAddedAndRemovedUserGroups(getSelectedUser(), getSelectedGroups());
        addedGroups = addedAndRemovedGroups.get(0);
        removedGroups = addedAndRemovedGroups.get(1);

        // get granted and revoked permissions from added and removed groups
        Set<Permission> granted = new HashSet<Permission>(permissionHandlingUtils.filterPermissions(groupPermissionHandlingUtils.getGrantedFromGroup(getSelectedGroups())).get(0));
        Set<Permission> revoked = new HashSet<Permission>(permissionHandlingUtils.filterPermissions(groupPermissionHandlingUtils.getRevokedByGroup(removedGroups)).get(0));
        revoked = permissionHandlingUtils.getRevokedByEliminatingConflicts(granted, revoked);

        // get permissions which can be revoked from the user
        List<Set<Permission>> revoke = permissionHandlingUtils.getRevokableFromRevoked(userPermissions, revoked, true);
        currentRevoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));

        // get permissions which can be granted to the user
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(userPermissions, currentRevoked), granted);
        Set<Permission> grantable = grant.get(0);
        super.setNotGranted(grant.get(1));

        Set<Permission> additionalGranted = revoke.get(2);
        grantable.addAll(additionalGranted);
        // TODO merge needed?
        grantable = permissionHandlingUtils.getNormalizedPermissions(grantable);

        // // merge grant and revoke based on the current permissions
        // List<Set<Permission>> revokeAndGrant = permissionHandlingUtils.getRevokedAndGrantedAfterMerge(userPermissions,
        // revokable, grantable);
        // revokable = revokeAndGrant.get(0);
        // grantable = revokeAndGrant.get(1);

        // current permission tree
        Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(allPermissions, grantable);

        Set<Permission> removable = new HashSet<Permission>();
        removable.addAll(replaced);
        removable.addAll(currentRevoked);
        currentPermissionTreeRoot = getPermissionTree(userPermissions, userDataPermissions, removable, Marking.REMOVED);

        // new permission tree
        List<Permission> currentPermissions = new ArrayList<Permission>(userPermissions);
        currentReplaced = permissionHandlingUtils.getReplacedByGranting(userPermissions, grantable);
        currentPermissions = new ArrayList<Permission>(permissionHandlingUtils.removeAll(currentPermissions, currentReplaced));
        currentPermissions.addAll(grantable);

        if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            newPermissionTreeRoot = getSelectablePermissionTree(currentPermissions, new ArrayList<Permission>(), grantable, currentRevoked, Marking.NEW, Marking.REMOVED);
        } else {
            currentPermissions = new ArrayList<Permission>(permissionHandlingUtils.removeAll(currentPermissions, removable));
            newPermissionTreeRoot = getPermissionTree(currentPermissions, new ArrayList<Permission>(), grantable, Marking.NEW);
        }
    }

    private Set<UserGroup> getSelectedGroups() {
        Set<UserGroup> ret = new HashSet<UserGroup>();
        for (TreeNode treeNode : selectedGroupNodes) {
            ret.add(((UserGroupModel) treeNode.getData()).getUserGroup());
        }
        return ret;
    }

    /**
     * listener for select unselect permissons in the new permission tree
     */
    public void rebuildCurrentPermissionTree() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        currentPermissionTreeRoot = rebuildCurrentTree(allPermissions, selectedPermissions, currentRevoked, currentReplaced);
    }

    /**
     * confirm button
     */
    public void confirm() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        executeRevokeAndGrant(getSelectedUser(), userPermissions, selectedPermissions, currentRevoked, currentReplaced);

        for (UserGroup group : removedGroups) {
            roleService.removeSubjectFromRole(getSelectedUser(), group);
        }
        for (UserGroup group : addedGroups) {
            roleService.addSubjectToRole(getSelectedUser(), group);
        }
        init();
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionViewRoot;
    }

    public TreeNode[] getSelectedGroupNodes() {
        return selectedGroupNodes;
    }

    public void setSelectedGroupNodes(TreeNode[] selectedGroupNodes) {
        this.selectedGroupNodes = selectedGroupNodes;
    }

    public DefaultTreeNode getGroupRoot() {
        return groupRoot;
    }

    public List<UserGroup> getUserGroups() {
        return currentUserGroups;
    }

    public User getSelectedUser() {
        return userSession.getSelectedUser();
    }

    public UserGroup getSelectedGroup() {
        return selectedGroup;
    }

    public TreeNode getCurrentPermissionTreeRoot() {
        return currentPermissionTreeRoot;
    }

    public TreeNode getNewPermissionTreeRoot() {
        return newPermissionTreeRoot;
    }

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissions) {
        this.selectedPermissionNodes = selectedPermissions;
    }

    public void setSelectedGroup(UserGroup selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    // dialog
    public List<Permission> getSelectedGroupPermissions() {
        if (selectedGroup != null) {
            return permissionManager.getPermissions(selectedGroup);
        } else {
            return new ArrayList<Permission>();
        }
    }

}
