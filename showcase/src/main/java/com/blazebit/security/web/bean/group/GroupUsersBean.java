/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

import java.io.Serializable;
import java.util.ArrayList;
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
import com.blazebit.security.web.bean.PermissionTreeHandlingBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "groupUsersBean")
@ViewScoped
public class GroupUsersBean extends PermissionTreeHandlingBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private RoleService roleService;

    private List<User> users = new ArrayList<User>();
    private List<UserModel> userList = new ArrayList<UserModel>();

    private DefaultTreeNode newPermissionRoot;
    private DefaultTreeNode currentPermissionRoot;

    private DefaultTreeNode permissionTreeViewRoot;

    private Set<Permission> selectedGroupPermissions = new HashSet<Permission>();

    private TreeNode[] selectedUserNodes = new TreeNode[] {};

    public void init() {
        if (getSelectedGroup() != null) {
            initUsers();
            initPermissions();
        }
    }

    private void initPermissions() {
        selectedGroupPermissions.clear();

        List<UserGroup> parents = new ArrayList<UserGroup>();
        UserGroup parent = getSelectedGroup().getParent();
        parents.add(getSelectedGroup());
        while (parent != null) {
            parents.add(0, parent);
            parent = parent.getParent();
        }
        this.permissionTreeViewRoot = new DefaultTreeNode("root", null);
        TreeNode groupNode = permissionTreeViewRoot;
        for (UserGroup group : parents) {
            groupNode = new DefaultTreeNode(new TreeNodeModel(group.getName(), ResourceType.USERGROUP, group), groupNode);
            groupNode.setExpanded(true);
            List<Permission> groupPermissions = permissionManager.getPermissions(group);
            List<Permission> permissions = filterPermissions(groupPermissions).get(0);
            List<Permission> dataPermissions = filterPermissions(groupPermissions).get(1);
            selectedGroupPermissions.addAll(permissions);
            groupNode = getPermissionTree(permissions, dataPermissions);
        }
        Set<Permission> redundantPermissions = getRedundantPermissions(selectedGroupPermissions);
        selectedGroupPermissions.removeAll(redundantPermissions);
        // ((TreeNodeModel) groupNode.getData()).setMarking(Marking.NEW);

    }

    private void initUsers() {
        List<User> allUsers = userService.findUsers(userSession.getSelectedCompany());
        users = userGroupService.getUsersFor(getSelectedGroup());
        userList.clear();
        for (User user : allUsers) {
            userList.add(new UserModel(user, users.contains(user)));
        }
    }

    /**
     * wizard listener
     * 
     * @param event
     * @return
     */
    public String userWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("users")) {
            processSelectedUsers();
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    private void processSelectedUsers() {
        currentPermissionRoot = new DefaultTreeNode();
        newPermissionRoot = new DefaultTreeNode();

        for (UserModel userModel : userList) {
            if (userModel.isSelected()) {
                // user is selected
                TreeNode currentUserNode = createCurrentUserNode(currentPermissionRoot, userModel.getUser(), true);
                TreeNode newUserNode = createNewUserNode(newPermissionRoot, userModel.getUser(), true);
                // mark user as new -> green
                if (!users.contains(userModel.getUser())) {
                    ((TreeNodeModel) currentUserNode.getData()).setMarking(Marking.NEW);
                    ((TreeNodeModel) newUserNode.getData()).setMarking(Marking.NEW);
                }

            } else {
                if (users.contains(userModel.getUser())) {
                    // user will be removed from group-> mark it red
                    TreeNode currentUserNode = createCurrentUserNode(currentPermissionRoot, userModel.getUser(), false);
                    TreeNode newUserNode = createNewUserNode(newPermissionRoot, userModel.getUser(), false);
                    ((TreeNodeModel) currentUserNode.getData()).setMarking(Marking.REMOVED);
                    ((TreeNodeModel) newUserNode.getData()).setMarking(Marking.REMOVED);
                }
            }
        }

    }

    private TreeNode createCurrentUserNode(TreeNode permissionRoot, User user, boolean addedUser) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);
        if (addedUser) {
            createCurrentPermissionTreeForAddedUser(userNode);
        } else {
            createCurrentPermissionTreeForRemovedUser(userNode);
        }
        return userNode;
    }

    private void createCurrentPermissionTreeForAddedUser(DefaultTreeNode userNode) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);
        if (!userPermissions.isEmpty()) {

            Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedGroupPermissions);

            getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);
        } else {
            TreeNode noPermissions = new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode);
            noPermissions.setSelectable(false);

        }
    }

    private void createCurrentPermissionTreeForRemovedUser(DefaultTreeNode userNode) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);

        if (!userPermissions.isEmpty()) {

            List<Set<Permission>> grantedAndRevoked = getRevokedAndGrantedPermissionsWhenRevoking(userPermissions, user, selectedGroupPermissions);
            Set<Permission> revoked = grantedAndRevoked.get(0);
            getPermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);
        } else {
            TreeNode noPermissions = new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode);
            noPermissions.setSelectable(false);

        }
    }

    private TreeNode createNewUserNode(TreeNode permissionRoot, User user, boolean addedUser) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);
        if (addedUser) {
            createNewPermissionTreeForAddedUser(userNode);
        } else {
            createNewPermissionTreeForRemovedUser(userNode);
        }
        return userNode;
    }

    private void createNewPermissionTreeForAddedUser(DefaultTreeNode userNode) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);

        Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedGroupPermissions);
        userPermissions.removeAll(replaced);

        Set<Permission> grant = getGrantablePermissions(userPermissions, user, selectedGroupPermissions);
        userPermissions.addAll(grant);
        if (userSession.getSelectedCompany().isUserLevelEnabled()) {
            getSelectablePermissionTree(userNode, userPermissions, userDataPermissions, grant, new HashSet<Permission>(), Marking.NEW, Marking.REMOVED);
        } else {
            getPermissionTree(userNode, userPermissions,  userDataPermissions, grant, Marking.NEW);
        }
    }

    private void createNewPermissionTreeForRemovedUser(DefaultTreeNode userNode) {
        TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) userNodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);
        List<Set<Permission>> grantedAndRevoked = getRevokedAndGrantedPermissionsWhenRevoking(userPermissions, user, selectedGroupPermissions);
        Set<Permission> revoked = grantedAndRevoked.get(0);

        if (userSession.getSelectedCompany().isUserLevelEnabled()) {
            getSelectablePermissionTree(userNode, userPermissions, userDataPermissions, new HashSet<Permission>(), revoked, Marking.NEW, Marking.REMOVED);
        } else {
            getPermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);
        }
    }

    /**
     * listener for select unselect permissons in the new permission tree
     */
    public void rebuildCurrentPermissionTree() {
        for (TreeNode userNode : currentPermissionRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = filterPermissions(permissions).get(0);
            List<Permission> userDataPermissions = filterPermissions(permissions).get(1);
            
            if (!userPermissions.isEmpty()) {
                if (Marking.NEW.equals(userNodeModel.getMarking()) || Marking.NONE.equals(userNodeModel.getMarking())) {
                    // this is an added or an existing user

                    Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);
                    Set<Permission> granted = getGrantedPermission(userPermissions, selectedPermissions).get(0);
                    Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);

                    userNode.getChildren().clear();

                    getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);

                } else {
                    // this is a removed user
                    Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);
                    Set<Permission> revoked = getRevokedPermissions(userPermissions, selectedPermissions).get(0);

                    userNode.getChildren().clear();
                    getPermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);

                }
            } else {
                userNode.getChildren().clear();
                new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode).setSelectable(false);
            }
        }
    }

    /**
     * confirm button
     */
    public void confirmPermissions() {
        for (TreeNode userNode : newPermissionRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();
            List<Permission> userPermissions = permissionManager.getPermissions(user);
            if (Marking.NEW.equals(userNodeModel.getMarking()) || Marking.NONE.equals(userNodeModel.getMarking())) {
                // add new suers

                Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);

                Set<Permission> granted = getGrantedPermission(userPermissions, selectedPermissions).get(0);
                Set<Permission> finalGranted = grantImpliedPermissions(userPermissions, granted);
                Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);
                for (Permission permission : replaced) {
                    permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
                }
                for (Permission permission : finalGranted) {
                    permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
                }
                roleService.addSubjectToRole(user, getSelectedGroup());
            } else {
                // remove user
                Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);
                Set<Permission> revoked = getRevokedPermissions(userPermissions, selectedPermissions).get(0);
                Set<Permission> finalRevoked = revokeImpliedPermissions(userPermissions, revoked);

                for (Permission permission : finalRevoked) {
                    permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
                }
                roleService.removeSubjectFromRole(user, getSelectedGroup());
            }
        }
    }

    public UserGroup getSelectedGroup() {
        return userSession.getSelectedUserGroup();
    }

    public DefaultTreeNode getCurrentPermissionRoot() {
        return currentPermissionRoot;
    }

    public DefaultTreeNode getNewPermissionRoot() {
        return newPermissionRoot;
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionTreeViewRoot;
    }

    public List<UserModel> getUserList() {
        return userList;
    }

    public TreeNode[] getSelectedUserNodes() {
        return selectedUserNodes;
    }

    public void setSelectedUserNodes(TreeNode[] selectedUserNodes) {
        this.selectedUserNodes = selectedUserNodes;
    }

}
