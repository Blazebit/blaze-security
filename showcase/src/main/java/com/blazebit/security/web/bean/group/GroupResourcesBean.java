/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.service.api.UserGroupService;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "groupResourcesBean")
public class GroupResourcesBean extends ResourceHandlingBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private UserGroupService userGroupService;

    private List<Permission> allPermissions = new ArrayList<Permission>();
    private List<Permission> groupPermissions = new ArrayList<Permission>();
    private List<Permission> groupDataPermissions = new ArrayList<Permission>();
    // wizard 1 step
    private TreeNode resourceRoot;
    private TreeNode[] selectedResourceNodes = new TreeNode[] {};
    // wizard 2 step
    private TreeNode newPermissionTreeRoot;
    private TreeNode currentPermissionTreeRoot;
    private TreeNode[] selectedGroupPermissionNodes = new TreeNode[] {};
    // wizard 3 step
    private DefaultTreeNode currentUserPermissionTreeRoot;
    private DefaultTreeNode newUserPermissionTreeRoot;
    private TreeNode[] selectedUserPermissionNodes = new TreeNode[] {};
    // permissionview
    private TreeNode permissionViewRoot;
    // group
    private Set<Permission> currentRevoked;
    private Set<Permission> currentReplaced;
    // users
    private Map<User, Set<Permission>> currentRevokedUserMap = new HashMap<User, Set<Permission>>();
    private Map<User, Set<Permission>> currentReplacedUserMap = new HashMap<User, Set<Permission>>();

    public void init() {
        initPermissions();
        initPermissionTree();
        initResourceTree();
    }

    private void initResourceTree() {
        try {
            resourceRoot = getResourceTree(groupPermissions);
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
    }

    private void initPermissions() {
        allPermissions = permissionManager.getPermissions(getSelectedGroup());
        groupPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(0);
        groupDataPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(1);
    }

    private void initPermissionTree() {
        this.permissionViewRoot = new DefaultTreeNode("root", null);
        TreeNode groupNode = permissionViewRoot;
        groupNode = new DefaultTreeNode(new TreeNodeModel(getSelectedGroup().getName(), ResourceType.USERGROUP, getSelectedGroup(), Marking.SELECTED), groupNode);
        groupNode.setExpanded(true);
        groupNode.setSelectable(false);
        getPermissionTree(groupNode, groupPermissions, groupDataPermissions);
    }

    public UserGroup getSelectedGroup() {
        return userSession.getSelectedUserGroup();
    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            processSelectedResources();
        } else {
            if (event.getOldStep().equals("permissions") && !event.getNewStep().equals("resources")) {
                confirmGroupPermissions();

                if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
                    confirmUserPermissions();
                }
            }
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    public void processSelectedResources() {
        // get selected permissions
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        // get revoked permissions
        List<Set<Permission>> revoke = permissionHandlingUtils.getRevokableFromSelected(groupPermissions, selectedPermissions);
        currentRevoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));
        // get granted permissions
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(groupPermissions, currentRevoked), selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));

        // get replaced permissions
        Set<Permission> allReplaced = permissionHandlingUtils.getReplacedByGranting(allPermissions, granted);

        // current permission tree without the revoked ones
        Set<Permission> removedPermissions = new HashSet<Permission>(currentRevoked);
        removedPermissions.addAll(allReplaced);
        currentPermissionTreeRoot = getPermissionTree(groupPermissions, groupDataPermissions, removedPermissions, Marking.REMOVED);

        // modify current group permissions based on resource selection
        List<Permission> currentPermissions = new ArrayList<Permission>(groupPermissions);

        // new permission tree without the revoked but with the granted ones
        currentReplaced = permissionHandlingUtils.getReplacedByGranting(groupPermissions, granted);
        currentPermissions.removeAll(currentReplaced);
        currentPermissions.addAll(granted);
        newPermissionTreeRoot = getSelectablePermissionTree(currentPermissions, new ArrayList<Permission>(), granted, currentRevoked, Marking.NEW, Marking.REMOVED);
    }

    public void rebuildCurrentGroupPermissionTree() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        currentPermissionTreeRoot = rebuildCurrentTree(allPermissions, selectedPermissions, currentRevoked, currentReplaced);
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmGroupPermissions() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        List<Set<Permission>> result = performRevokeAndGrant(getSelectedGroup(), allPermissions, selectedPermissions, currentRevoked, currentReplaced);
        prepareUserPropagationView(result.get(1), result.get(0));
        // reset
        init();
    }

    private void prepareUserPropagationView(Set<Permission> granted, Set<Permission> revoked) {
        // show user propagation view
        Set<User> users = new HashSet<User>(userGroupService.getUsersFor(getSelectedUserGroup()));
        List<UserGroup> groups = userGroupService.getGroupsForGroup(getSelectedUserGroup());
        for (UserGroup userGroup : groups) {
            mergeUserList(userGroup, users);
        }
        List<User> sortedUsers = new ArrayList<User>(users);
        Collections.sort(sortedUsers, new Comparator<User>() {

            @Override
            public int compare(User o1, User o2) {
                return o1.getUsername().compareToIgnoreCase(o2.getUsername());
            }

        });
        buildUserPermissionTrees(sortedUsers, granted, revoked);
        // if user level is not enabled confirm user permissions immediately
        if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            confirmUserPermissions();
        }
    }

    private void buildUserPermissionTrees(List<User> users, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        currentUserPermissionTreeRoot = new DefaultTreeNode();
        newUserPermissionTreeRoot = new DefaultTreeNode();
        for (User user : users) {

            DefaultTreeNode userNode = new DefaultTreeNode(new TreeNodeModel(user.getUsername(), ResourceType.USER, user), currentUserPermissionTreeRoot);
            userNode.setExpanded(true);
            userNode.setSelectable(false);

            List<Permission> allPermissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(0);
            List<Permission> userDataPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(1);

            createCurrentPermissionTree(userNode, userPermissions, userDataPermissions, grantedPermissions, revokedPermissions);

            DefaultTreeNode newUserNode = new DefaultTreeNode(new TreeNodeModel(user.getUsername(), ResourceType.USER, user), newUserPermissionTreeRoot);
            newUserNode.setExpanded(true);
            newUserNode.setSelectable(false);
            createNewPermissionTree(newUserNode, userPermissions, new ArrayList<Permission>(), grantedPermissions, revokedPermissions);
        }
    }

    private void createCurrentPermissionTree(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        // get permissions which can be revoked from the user
        List<Set<Permission>> revoke = permissionHandlingUtils.getRevokableFromRevoked(userPermissions, revokedPermissions, true);
        Set<Permission> revoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));

        // get permissions which can be granted to the user
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(userPermissions, revoked), grantedPermissions);
        Set<Permission> grantable = grant.get(0);
        super.setNotGranted(grant.get(1));

        Set<Permission> additionalGranted = revoke.get(2);
        grantable.addAll(additionalGranted);
        // TODO merge needed?
        grantable = permissionHandlingUtils.getNormalizedPermissions(grantable);

        // current permission tree
        Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(allPermissions, grantable);
        Set<Permission> removable = new HashSet<Permission>();
        removable.addAll(replaced);
        removable.addAll(revoked);
        getPermissionTree(userNode, userPermissions, userDataPermissions, removable, Marking.REMOVED);
    }

    private void createNewPermissionTree(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();

        // get permissions which can be revoked from the user
        List<Set<Permission>> revoke = permissionHandlingUtils.getRevokableFromRevoked(userPermissions, revokedPermissions, true);
        Set<Permission> currentRevoked = revoke.get(0);
        currentRevokedUserMap.put(user, currentRevoked);
        super.setNotRevoked(revoke.get(1));

        // get permissions which can be granted to the user
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(userPermissions, currentRevoked), grantedPermissions);
        Set<Permission> grantable = grant.get(0);
        super.setNotGranted(grant.get(1));

        Set<Permission> additionalGranted = revoke.get(2);
        grantable.addAll(additionalGranted);
        // TODO merge needed?
        grantable = permissionHandlingUtils.getNormalizedPermissions(grantable);

        // new permission tree
        List<Permission> currentPermissions = new ArrayList<Permission>(userPermissions);
        Set<Permission> currentReplaced = permissionHandlingUtils.getReplacedByGranting(userPermissions, grantable);
        currentReplacedUserMap.put(user, currentReplaced);
        currentPermissions = new ArrayList<Permission>(permissionHandlingUtils.removeAll(currentPermissions, currentReplaced));
        currentPermissions.addAll(grantable);

        if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            getSelectablePermissionTree(userNode, currentPermissions, new ArrayList<Permission>(), grantable, currentRevoked, Marking.NEW, Marking.REMOVED);
        } else {

            Set<Permission> removable = new HashSet<Permission>();
            removable.addAll(currentReplaced);
            removable.addAll(currentRevoked);

            currentPermissions = new ArrayList<Permission>(permissionHandlingUtils.removeAll(currentPermissions, removable));
            newPermissionTreeRoot = getPermissionTree(userNode, currentPermissions, new ArrayList<Permission>(), grantable, Marking.NEW);
            selectedUserPermissionNodes = (TreeNode[]) ArrayUtils.addAll(selectedUserPermissionNodes, getSelectedNodes(userNode.getChildren()));
        }
    }

    private void mergeUserList(UserGroup userGroup, Set<User> users) {
        users.addAll(userGroupService.getUsersFor(userGroup));
        for (UserGroup childGroup : userGroupService.getGroupsForGroup(userGroup)) {
            mergeUserList(childGroup, users);
        }
    }

    public void rebuildCurrentUserPermissionTree() {
        for (TreeNode userNode : currentUserPermissionTreeRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            List<Permission> userPermissions = permissionManager.getPermissions(user);
            rebuildCurrentTree(userNode, userPermissions, selectedPermissions, currentRevokedUserMap.get(user), currentReplacedUserMap.get(user));
        }
    }

    // confirm button for users
    public void confirmUserPermissions() {
        for (TreeNode userNode : newUserPermissionTreeRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();

            List<Permission> allPermissions = permissionManager.getPermissions(user);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            performRevokeAndGrant(user, allPermissions, selectedPermissions, currentRevokedUserMap.get(user), currentReplacedUserMap.get(user));

        }
        init();
    }

    public TreeNode getResourceRoot() {
        return resourceRoot;
    }

    public UserGroup getSelectedUserGroup() {
        return userSession.getSelectedUserGroup();
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionViewRoot;
    }

    public TreeNode getNewPermissionTreeRoot() {
        return newPermissionTreeRoot;
    }

    public void setNewPermissionTreeRoot(DefaultTreeNode newPermissionTreeRoot) {
        this.newPermissionTreeRoot = newPermissionTreeRoot;
    }

    public TreeNode getCurrentPermissionTreeRoot() {
        return currentPermissionTreeRoot;
    }

    public void setCurrentPermissionTreeRoot(DefaultTreeNode currentPermissionTreeRoot) {
        this.currentPermissionTreeRoot = currentPermissionTreeRoot;
    }

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedResourceNodes = selectedPermissionNodes;
    }

    public TreeNode getCurrentUserPermissionTreeRoot() {
        return currentUserPermissionTreeRoot;
    }

    public TreeNode getNewUserPermissionTreeRoot() {
        return newUserPermissionTreeRoot;
    }

    public TreeNode[] getSelectedUserPermissionNodes() {
        return selectedUserPermissionNodes;
    }

    public void setSelectedUserPermissionNodes(TreeNode[] selectedUserPermissionNodes) {
        this.selectedUserPermissionNodes = selectedUserPermissionNodes;
    }

    public TreeNode[] getSelectedGroupPermissionNodes() {
        return selectedGroupPermissionNodes;
    }

    public void setSelectedGroupPermissionNodes(TreeNode[] selectedGroupPermissionNodes) {
        this.selectedGroupPermissionNodes = selectedGroupPermissionNodes;
    }

}
