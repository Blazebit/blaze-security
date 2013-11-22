/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
        List<Set<Permission>> revoke = permissionHandlingUtils.getRevokable(groupPermissions, selectedPermissions);
        Set<Permission> revoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));
        // get granted permissions
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantable(permissionHandlingUtils.removeAll(groupPermissions, revoked), selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        // get replaced permissions
        Set<Permission> allReplaced = permissionHandlingUtils.getReplacedByGranting(allPermissions, granted);

        // current permission tree without the revoked ones
        Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
        removedPermissions.addAll(allReplaced);
        currentPermissionTreeRoot = getPermissionTree(groupPermissions, groupDataPermissions, removedPermissions, Marking.REMOVED);

        // new permission tree without the revoked but with the granted ones
        List<Permission> currentPermissions = new ArrayList<Permission>(groupPermissions);
        Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(groupPermissions, granted);
        currentPermissions.removeAll(replaced);
        currentPermissions.addAll(granted);
        newPermissionTreeRoot = getSelectablePermissionTree(currentPermissions, new ArrayList<Permission>(), granted, revoked, Marking.NEW, Marking.REMOVED);
    }

    public void rebuildCurrentGroupPermissionTree() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = permissionHandlingUtils.getReplacedByGranting(groupPermissions, selectedResourcePermissions);

        // current selected permissions
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        // add back previously replaced, because we need to recalculate based on the current selections
        for (Permission replacedPermission : previouslyReplaced) {
            if (!permissionHandlingUtils.implies(selectedPermissions, replacedPermission)) {
                selectedPermissions.add(replacedPermission);
            }
        }

        List<Set<Permission>> revoke = permissionHandlingUtils.getRevokable(groupPermissions, selectedPermissions);
        Set<Permission> revoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));

        Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(allPermissions, selectedPermissions);

        Set<Permission> removedPermissions = new HashSet<Permission>();
        removedPermissions.addAll(revoked);
        removedPermissions.addAll(replaced);
        // current permission tree
        currentPermissionTreeRoot = getPermissionTree(groupPermissions, groupDataPermissions, removedPermissions, Marking.REMOVED);
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmGroupPermissions() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = permissionHandlingUtils.getReplacedByGranting(groupPermissions, selectedResourcePermissions);

        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        // add back previously replaced, because we need to recalculate based on the current selections
        for (Permission replacedPermission : previouslyReplaced) {
            if (!permissionHandlingUtils.implies(selectedPermissions, replacedPermission)) {
                selectedPermissions.add(replacedPermission);
            }
        }

        Set<Permission> revoked = permissionHandlingUtils.getRevokable(groupPermissions, selectedPermissions).get(0);
        Collection<Permission> currentPermissions = permissionHandlingUtils.removeAll(groupPermissions, revoked);
        Set<Permission> granted = permissionHandlingUtils.getGrantable(currentPermissions, selectedPermissions).get(0);

        for (Permission permission : revoked) {
            permissionService.revoke(userSession.getUser(), getSelectedGroup(), permission.getAction(), permission.getResource());
        }
        for (Permission permission : granted) {
            permissionService.grant(userSession.getUser(), getSelectedGroup(), permission.getAction(), permission.getResource());
        }

        prepareUserPropagationView(granted, revoked);
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
        if (!userPermissions.isEmpty()) {

            Set<Permission> revoked = new HashSet<Permission>();
            for (Permission permission : revokedPermissions) {
                if (permissionDataAccess.isRevokable(userPermissions, permission.getAction(), permission.getResource())) {
                    revoked.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(userPermissions, permission.getAction(), permission.getResource()));
                }
            }
            List<Permission> all = new ArrayList<Permission>();
            all.addAll(userPermissions);
            all.addAll(userDataPermissions);
            Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(all, grantedPermissions);

            Set<Permission> removables = new HashSet<Permission>();
            removables.addAll(revoked);
            removables.addAll(replaced);

            getPermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);

        } else {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode).setSelectable(false);
        }

    }

    private void createNewPermissionTree(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();

        Set<Permission> revoked = new HashSet<Permission>();
        for (Permission permission : revokedPermissions) {
            if (permissionDataAccess.isRevokable(userPermissions, permission.getAction(), permission.getResource())) {
                revoked.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(userPermissions, permission.getAction(), permission.getResource()));
            }
        }
        Set<Permission> granted = permissionHandlingUtils.getGrantable(permissionHandlingUtils.removeAll(userPermissions, revoked), grantedPermissions).get(0);

        List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
        currentUserPermissions.addAll(granted);
        currentUserPermissions.removeAll(revoked);

        if (!currentUserPermissions.isEmpty()) {
            if (userSession.getSelectedCompany().isUserLevelEnabled()) {
                getSelectablePermissionTree(userNode, currentUserPermissions, new ArrayList<Permission>(), granted, revoked, Marking.NEW, Marking.REMOVED);
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

    public void rebuildCurrentUserPermissionTree() {
        // TODO
    }

    // confirm button for users
    public void confirmUserPermissions() {
        for (TreeNode userNode : newUserPermissionTreeRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();

            List<Permission> allPermissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(0);

            Set<Permission> selectedPermissions;
            if (userSession.getSelectedCompany().isUserLevelEnabled()) {
                selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            } else {
                selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
            }

            Set<Permission> revoked = permissionHandlingUtils.getRevokable(userPermissions, selectedPermissions).get(0);

            Set<Permission> granted = permissionHandlingUtils.getGrantable(permissionHandlingUtils.removeAll(userPermissions, revoked), selectedPermissions).get(0);

            for (Permission permission : revoked) {
                permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
            for (Permission permission : granted) {
                permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
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
