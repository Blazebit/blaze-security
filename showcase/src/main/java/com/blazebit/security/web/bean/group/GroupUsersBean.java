/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

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

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.Company;
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
    private Map<User, Set<Permission>> currentReplacedUserMap = new HashMap<User, Set<Permission>>();
    private Map<User, Set<Permission>> currentRevokedUserMap = new HashMap<User, Set<Permission>>();

    public void init() {
        initUsers();
        initPermissions();
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
            groupNode.setSelectable(false);
            List<Permission> groupPermissions = permissionManager.getPermissions(group);
            List<Permission> permissions = permissionHandlingUtils.filterPermissions(groupPermissions).get(0);
            List<Permission> dataPermissions = permissionHandlingUtils.filterPermissions(groupPermissions).get(1);
            selectedGroupPermissions.addAll(permissions);
            groupNode = getPermissionTree(permissions, dataPermissions);
        }
        selectedGroupPermissions = permissionHandlingUtils.getNormalizedPermissions(selectedGroupPermissions);
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
            List<Permission> permissions = permissionManager.getPermissions(userModel.getUser());
            List<Permission> userPermissions = permissionHandlingUtils.filterPermissions(permissions).get(0);
            List<Permission> userDataPermissions = permissionHandlingUtils.filterPermissions(permissions).get(1);

            if (userModel.isSelected()) {
                // user is selected
                TreeNode currentUserNode = createCurrentUserNode(currentPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, true);
                TreeNode newUserNode = createNewUserNode(newPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, true);
                // mark user as new -> green
                if (!users.contains(userModel.getUser())) {
                    ((TreeNodeModel) currentUserNode.getData()).setMarking(Marking.NEW);
                    ((TreeNodeModel) newUserNode.getData()).setMarking(Marking.NEW);
                }

            } else {
                if (users.contains(userModel.getUser())) {
                    // user will be removed from group-> mark it red
                    TreeNode currentUserNode = createCurrentUserNode(currentPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, false);
                    TreeNode newUserNode = createNewUserNode(newPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, false);
                    ((TreeNodeModel) currentUserNode.getData()).setMarking(Marking.REMOVED);
                    ((TreeNodeModel) newUserNode.getData()).setMarking(Marking.REMOVED);
                }
            }
        }

        if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            confirmPermissions();
        }
    }

    private TreeNode createCurrentUserNode(TreeNode permissionRoot, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, boolean addedUser) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);
        if (addedUser) {
            createCurrentPermissionTreeForAddedUser(userNode, userPermissions, userDataPermissions);
        } else {
            createCurrentPermissionTreeForRemovedUser(userNode, userPermissions, userDataPermissions);
        }
        return userNode;
    }

    private void createCurrentPermissionTreeForAddedUser(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        // added user receives new permissions from groups -> mark only
        // replaceable
        Set<Permission> replaced = permissionHandlingUtils
            .getReplacedByGranting(concat(userPermissions, userDataPermissions), permissionHandlingUtils.getGrantableFromSelected(userPermissions, selectedGroupPermissions).get(0));
        getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);
    }

    private void createCurrentPermissionTreeForRemovedUser(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        // group permissions will be revoked from user-> mark only revokables
        Set<Permission> revoked = permissionHandlingUtils.getRevokableFromRevoked(userPermissions, selectedGroupPermissions, true).get(0);
        getPermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);
    }

    private TreeNode createNewUserNode(TreeNode permissionRoot, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, boolean addedUser) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);
        if (addedUser) {
            createNewPermissionTreeForAddedUser(user, userNode, userPermissions);
        } else {
            createNewPermissionTreeForRemovedUser(user, userNode, userPermissions);
        }
        return userNode;
    }

    private void createNewPermissionTreeForAddedUser(User user, DefaultTreeNode userNode, List<Permission> userPermissions) {
        Set<Permission> granted = permissionHandlingUtils.getGrantableFromSelected(userPermissions, selectedGroupPermissions).get(0);
        Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(userPermissions, granted);
        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        currentPermissions.removeAll(replaced);
        currentPermissions.addAll(granted);
        currentReplacedUserMap.put(user, replaced);
        if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            getSelectablePermissionTree(userNode, new ArrayList<Permission>(currentPermissions), new ArrayList<Permission>(), granted, new HashSet<Permission>(), Marking.NEW,
                                        Marking.REMOVED);
        } else {
            getPermissionTree(userNode, new ArrayList<Permission>(currentPermissions), new ArrayList<Permission>(), granted, Marking.NEW);
            selectedUserNodes = (TreeNode[]) ArrayUtils.addAll(selectedUserNodes, getSelectedNodes(userNode.getChildren()).toArray());
        }
    }

    private void createNewPermissionTreeForRemovedUser(User user, DefaultTreeNode userNode, List<Permission> userPermissions) {
        Set<Permission> revoked = permissionHandlingUtils.getRevokableFromRevoked(userPermissions, selectedGroupPermissions, true).get(0);
        Set<Permission> granted = permissionHandlingUtils.getRevokableFromRevoked(userPermissions, selectedGroupPermissions, true).get(2);
        // when user has entity permission and group has field permission, we have to build the tree in a way that the group
        // entity field permissions can be revoked and the rest of the fields can be kept.
        Set<Permission> impliedBy = new HashSet<Permission>();
        Set<Permission> toRevoke = new HashSet<Permission>();
        if (!granted.isEmpty()) {
            for (Permission permission : selectedGroupPermissions) {
                if (!permissionHandlingUtils.contains(revoked, permission) && permissionHandlingUtils.implies(revoked, permission)) {
                    impliedBy.addAll(permissionDataAccess.getImpliedBy(new ArrayList<Permission>(revoked), permission.getAction(), permission.getResource()));
                    toRevoke.add(permission);
                }
            }
        }
        revoked = new HashSet<Permission>(permissionHandlingUtils.removeAll(revoked, impliedBy));
        revoked.addAll(toRevoke);
        currentRevokedUserMap.put(user, revoked);

        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        currentPermissions.addAll(granted);
        currentPermissions.addAll(toRevoke);

        currentPermissions = new HashSet<Permission>(permissionHandlingUtils.removeAll(currentPermissions, impliedBy));

        if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            getSelectablePermissionTree(userNode, new ArrayList<Permission>(currentPermissions), new ArrayList<Permission>(), granted, revoked, Marking.NEW, Marking.REMOVED);
        } else {
            List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
            currentUserPermissions = (List<Permission>) permissionHandlingUtils.removeAll(currentUserPermissions, revoked);
            currentUserPermissions.addAll(granted);
            getPermissionTree(userNode, currentUserPermissions, new ArrayList<Permission>(), new HashSet<Permission>(currentUserPermissions), Marking.NONE);
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
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);
            userNode.getChildren().clear();
            if (Marking.NEW.equals(userNodeModel.getMarking()) || Marking.NONE.equals(userNodeModel.getMarking())) {
                // this is an added or an existing user
                rebuildCurrentTree(userNode, permissions, selectedPermissions, new HashSet<Permission>(), currentReplacedUserMap.get(user));

            } else {
                // this is a removed user
                rebuildCurrentTree(userNode, permissions, selectedPermissions, currentRevokedUserMap.get(user), new HashSet<Permission>());
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

            List<Permission> allPermissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(0);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);

            if (Marking.NEW.equals(userNodeModel.getMarking()) || Marking.NONE.equals(userNodeModel.getMarking())) {
                // add new users + resources or add new group resources to users
                performRevokeAndGrant(user, userPermissions, selectedPermissions, new HashSet<Permission>(), currentReplacedUserMap.get(user));
                roleService.addSubjectToRole(user, getSelectedGroup());
            } else {
                if (Marking.REMOVED.equals(userNodeModel.getMarking())) {
                    // remove user and remove unselected resources too
                    performRevokeAndGrant(user, userPermissions, selectedPermissions, currentRevokedUserMap.get(user), new HashSet<Permission>());
                    roleService.removeSubjectFromRole(user, getSelectedGroup());
                }
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
