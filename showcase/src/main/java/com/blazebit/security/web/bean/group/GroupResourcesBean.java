/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.service.api.UserGroupService;

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
    private TreeNode currentUserPermissionTreeRoot;
    private TreeNode newUserPermissionTreeRoot;
    private TreeNode[] selectedUserPermissionNodes = new TreeNode[] {};
    // permissionview
    private TreeNode permissionViewRoot;

    public void init() {
        initPermissions();
        try {
            resourceRoot = getResourceTree(groupPermissions);
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
    }

    private void initPermissions() {
        groupPermissions = permissionManager.getPermissions(getSelectedGroup());
        this.permissionViewRoot = new DefaultTreeNode("root", null);
        TreeNode groupNode = permissionViewRoot;
        groupNode = new DefaultTreeNode(new TreeNodeModel(getSelectedGroup().getName(), ResourceType.USERGROUP, getSelectedGroup(), Marking.SELECTED), groupNode);
        groupNode.setExpanded(true);
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
                if (!userSession.getSelectedCompany().isUserLevelEnabled()) {
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
        List<Set<Permission>> revoke = getRevokedPermissions(groupPermissions, selectedPermissions);
        Set<Permission> revoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));
        // get granted permissions
        List<Set<Permission>> grant = getGrantedPermission(groupPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        // get replaced permissions
        Set<Permission> replaced = getReplacedPermissions(getSelectedUserGroup(), granted);
        // current user permissions
        List<Permission> currentUserPermissions = new ArrayList<Permission>(groupPermissions);
        // current permission tree without the revoked ones
        Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
        removedPermissions.addAll(replaced);
        currentPermissionTreeRoot = getPermissionTree(currentUserPermissions, groupDataPermissions, removedPermissions, Marking.REMOVED);
        // new permission tree without the revoked but with the granted ones
        currentUserPermissions.removeAll(replaced);
        currentUserPermissions.addAll(granted);
        newPermissionTreeRoot = getSelectablePermissionTree(currentUserPermissions, granted, revoked, Marking.NEW, Marking.REMOVED);
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmGroupPermissions() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = getReplacedPermissions(getSelectedGroup(), selectedResourcePermissions);
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        selectedPermissions.addAll(previouslyReplaced);
        Set<Permission> granted = getGrantedPermission(groupPermissions, selectedPermissions).get(0);
        Set<Permission> finalGranted = grantImpliedPermissions(groupPermissions, granted);
        Set<Permission> replaced = getReplacedPermissions(getSelectedGroup(), granted);
        Set<Permission> revoked = getRevokedPermissions(groupPermissions, selectedPermissions).get(0);
        Set<Permission> finalRevoked = revokeImpliedPermissions(groupPermissions, revoked);

        for (Permission permission : finalRevoked) {
            permissionService.revoke(userSession.getUser(), getSelectedUserGroup(), permission.getAction(), permission.getResource());
        }
        for (Permission permission : replaced) {
            permissionService.revoke(userSession.getUser(), getSelectedUserGroup(), permission.getAction(), permission.getResource());
        }
        for (Permission permission : finalGranted) {
            permissionService.grant(userSession.getUser(), getSelectedUserGroup(), permission.getAction(), permission.getResource());
        }
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
        currentUserPermissionTreeRoot = buildCurrentUserPermissionTree(sortedUsers, granted, revoked);
        newUserPermissionTreeRoot = buildNewUserPermissionTree(sortedUsers, granted, revoked);
        // reset group permission view
        initPermissions();
    }

    private TreeNode buildCurrentUserPermissionTree(List<User> users, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        TreeNode ret = new DefaultTreeNode();
        for (User user : users) {
            DefaultTreeNode currentUserRoot = new DefaultTreeNode(new TreeNodeModel(user.getUsername(), ResourceType.USER, user), ret);
            currentUserRoot.setExpanded(true);
            currentUserRoot.setSelectable(false);
            List<Permission> userPermissions = permissionManager.getPermissions(user);

            createCurrentPermissionTree(currentUserRoot, filterPermissions(userPermissions).get(0), filterPermissions(userPermissions).get(1), grantedPermissions,
                                        revokedPermissions);
        }
        return ret;
    }

    private TreeNode buildNewUserPermissionTree(List<User> users, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        TreeNode ret = new DefaultTreeNode();
        for (User user : users) {
            DefaultTreeNode newUserRoot = new DefaultTreeNode(new TreeNodeModel(user.getUsername(), ResourceType.USER, user), ret);
            newUserRoot.setExpanded(true);
            newUserRoot.setSelectable(false);
            List<Permission> userPermissions = permissionManager.getPermissions(user);
            createNewPermissionTree(newUserRoot, filterPermissions(userPermissions).get(0), filterPermissions(userPermissions).get(1), grantedPermissions, revokedPermissions);
        }
        return ret;
    }

    private void createCurrentPermissionTree(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();
        if (!userPermissions.isEmpty()) {

            List<Set<Permission>> revokedAndGranted = getRevokedAndGrantedPermissionsWhenRevoking(userPermissions, user, revokedPermissions);
            Set<Permission> revokedOK = revokedAndGranted.get(0);
            Set<Permission> revokedWhenRevoked = revokedAndGranted.get(1);

            Set<Permission> granted = getGrantablePermissions(userPermissions, user, grantedPermissions);
            Set<Permission> replaced = getReplacedPermissions(userPermissions, grantedPermissions);
            userPermissions.removeAll(replaced);

            Set<Permission> revoked = new HashSet<Permission>(replaced);
            revoked.addAll(revokedWhenRevoked);
            revoked.addAll(revokedOK);

            getPermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);

        } else {
            TreeNode noPermissions = new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode);
            noPermissions.setSelectable(false);

        }

    }

    private void createNewPermissionTree(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();

        List<Set<Permission>> revokedAndGranted = getRevokedAndGrantedPermissionsWhenRevoking(userPermissions, user, revokedPermissions);
        Set<Permission> revokedOK = revokedAndGranted.get(0);
        Set<Permission> revokedWhenRevoked = revokedAndGranted.get(1);
        Set<Permission> grantedWhenRevoked = revokedAndGranted.get(2);

        List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
        currentUserPermissions.removeAll(revokedWhenRevoked);
        currentUserPermissions.removeAll(revokedOK);
        currentUserPermissions.addAll(grantedWhenRevoked);

        Set<Permission> grantedWhenGranted = getGrantablePermissions(currentUserPermissions, user, grantedPermissions);
        Set<Permission> revokedWhenGranted = getReplacedPermissions(currentUserPermissions, grantedPermissions);
        currentUserPermissions.removeAll(revokedWhenGranted);

        currentUserPermissions.addAll(grantedWhenGranted);

        Set<Permission> revoked = new HashSet<Permission>();// TODO put something in it

        Set<Permission> granted = new HashSet<Permission>(grantedWhenGranted);
        granted.addAll(grantedWhenRevoked);

        if (!currentUserPermissions.isEmpty()) {
            if (userSession.getSelectedCompany().isUserLevelEnabled()) {
                getSelectablePermissionTree(userNode, currentUserPermissions, granted, revoked, Marking.NEW, Marking.REMOVED);
            } else {
                userPermissions.removeAll(revoked);
                getPermissionTree(userNode, currentUserPermissions, userDataPermissions, granted, Marking.NEW);
            }

        } else {
            TreeNode noPermissions = new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode);
            noPermissions.setSelectable(false);
        }

    }

    private void mergeUserList(UserGroup userGroup, Set<User> users) {
        users.addAll(userGroupService.getUsersFor(userGroup));
        for (UserGroup childGroup : userGroupService.getGroupsForGroup(userGroup)) {
            mergeUserList(childGroup, users);
        }
    }

    public void confirmUserPermissions() {
        for (TreeNode userNode : newUserPermissionTreeRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();
            List<Permission> userPermissions = permissionManager.getPermissions(user);
            Set<Permission> selectedPermissions;
            if (userSession.getSelectedCompany().isUserLevelEnabled()) {
                selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            } else {
                selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
            }
            Set<Permission> granted = getGrantedPermission(userPermissions, selectedPermissions).get(0);
            Set<Permission> finalGranted = grantImpliedPermissions(userPermissions, granted);
            Set<Permission> revoked = getRevokedPermissions(userPermissions, selectedPermissions).get(0);
            Set<Permission> finalRevoked = revokeImpliedPermissions(userPermissions, revoked);
            Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);
            for (Permission permission : finalRevoked) {
                permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
            for (Permission permission : replaced) {
                permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
            for (Permission permission : finalGranted) {
                permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
        }
        init();
    }

    public void rebuildCurrentGroupPermissionTree() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = getReplacedPermissions(getSelectedGroup(), selectedResourcePermissions);

        // current selected permissions
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        selectedPermissions.addAll(previouslyReplaced);

        Set<Permission> granted = getGrantedPermission(groupPermissions, selectedPermissions).get(0);
        Set<Permission> replaced = getReplacedPermissions(groupPermissions, selectedPermissions);
        Set<Permission> revoked = getRevokedPermissions(groupPermissions, selectedPermissions).get(0);

        Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
        removedPermissions.addAll(replaced);
        // current permission tree
        currentPermissionTreeRoot = getPermissionTree(groupPermissions, groupDataPermissions, removedPermissions, Marking.REMOVED);
    }

    public void rebuildCurrentUserPermissionTree() {

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
