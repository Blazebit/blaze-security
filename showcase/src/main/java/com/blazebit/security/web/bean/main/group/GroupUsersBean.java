/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.group;

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
import com.blazebit.security.web.bean.base.GroupHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "groupUsersBean")
@ViewScoped
public class GroupUsersBean extends GroupHandlingBaseBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private UserService userService;

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
        // prepare permissions to grant or revoke
        prepareGroupPermissions();
    }

    private void prepareGroupPermissions() {
        // get permissions based on hierarchy or not
        Set<UserGroup> addedGroups = new HashSet<UserGroup>();
        addedGroups.add(getSelectedGroup());
        // normalized merge of all the permissions of the group hierarchy
        selectedGroupPermissions = groupPermissionHandling.getGroupPermissions(addedGroups, isEnabled(Company.GROUP_HIERARCHY));
        // TODO if no field level enabled-> add only parent permissions. is this a good solution?
        if (!isEnabled(Company.FIELD_LEVEL)) {
            selectedGroupPermissions = permissionHandling.getParentPermissions(selectedGroupPermissions);
        }
        // if object level is not enabled, just ignore object level permissions
        if (!isEnabled(Company.OBJECT_LEVEL)) {
            selectedGroupPermissions = new HashSet<Permission>(permissionHandling.getSeparatedPermissions(selectedGroupPermissions).get(0));
        }
    }

    private void initPermissions() {
        permissionTreeViewRoot = initGroupPermissions(getSelectedGroup(), !isEnabled(Company.FIELD_LEVEL));
    }

    private void initUsers() {
        List<User> allUsers = userService.findUsers(userSession.getSelectedCompany());
        users = userGroupDataAccess.getUsersFor(getSelectedGroup());
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
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
            List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);

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

        if (!isEnabled(Company.USER_LEVEL)) {
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
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(concat(userPermissions, userDataPermissions),
                                                                            permissionHandling.getGrantable(userPermissions, selectedGroupPermissions).get(0));
        getImmutablePermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED, !isEnabled(Company.FIELD_LEVEL));
    }

    private void createCurrentPermissionTreeForRemovedUser(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        // group permissions will be revoked from user-> mark only revokables
        Set<Permission> revoked = permissionHandling.getRevokableFromRevoked(userPermissions, selectedGroupPermissions, true).get(0);
        getImmutablePermissionTree(userNode, userPermissions, userDataPermissions, revoked, Marking.REMOVED);
    }

    private TreeNode createNewUserNode(TreeNode permissionRoot, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, boolean addedUser) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);
        if (addedUser) {
            createNewPermissionTreeForAddedUser(user, userNode, userPermissions, userDataPermissions);
        } else {
            createNewPermissionTreeForRemovedUser(user, userNode, userPermissions, userDataPermissions);
        }
        return userNode;
    }

    private void createNewPermissionTreeForAddedUser(User user, DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        Set<Permission> granted = permissionHandling.getGrantable(concat(userPermissions, userDataPermissions), selectedGroupPermissions).get(0);
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(userPermissions, granted);

        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        currentPermissions.removeAll(replaced);
        currentPermissions.addAll(permissionHandling.getSeparatedPermissions(granted).get(0));

        Set<Permission> currentDataPermissions = new HashSet<Permission>(userDataPermissions);
        replaced.addAll(permissionHandling.getReplacedByGranting(userDataPermissions, granted));
        currentDataPermissions.removeAll(replaced);
        currentDataPermissions.addAll(permissionHandling.getSeparatedPermissions(granted).get(1));

        currentReplacedUserMap.put(user, replaced);

        if (isEnabled(Company.USER_LEVEL)) {
            getMutablePermissionTree(userNode, new ArrayList<Permission>(currentPermissions), new ArrayList<Permission>(currentDataPermissions), granted,
                                     new HashSet<Permission>(), Marking.NEW, Marking.REMOVED);
        } else {
            getImmutablePermissionTree(userNode, new ArrayList<Permission>(currentPermissions), new ArrayList<Permission>(), granted, Marking.NEW);
            selectedUserNodes = (TreeNode[]) ArrayUtils.addAll(selectedUserNodes, getSelectedNodes(userNode.getChildren()).toArray());
        }
    }

    private void createNewPermissionTreeForRemovedUser(User user, DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        Set<Permission> revoked = permissionHandling.getRevokableFromRevoked(concat(userPermissions, userDataPermissions), selectedGroupPermissions, true).get(0);
        Set<Permission> granted = permissionHandling.getRevokableFromRevoked(concat(userPermissions, userDataPermissions), selectedGroupPermissions, true).get(2);
        // when user has entity permission and group has field permission, we have to build the tree in a way that the group
        // entity field permissions can be revoked and the rest of the fields can be kept.
        Set<Permission> impliedBy = new HashSet<Permission>();
        Set<Permission> toRevoke = new HashSet<Permission>();
        if (!granted.isEmpty()) {
            for (Permission permission : selectedGroupPermissions) {
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
                rebuildCurrentTree(userNode, permissions, selectedPermissions, new HashSet<Permission>(), currentReplacedUserMap.get(user), !isEnabled(Company.FIELD_LEVEL));

            } else {
                // this is a removed user
                rebuildCurrentTree(userNode, permissions, selectedPermissions, currentRevokedUserMap.get(user), new HashSet<Permission>(), !isEnabled(Company.FIELD_LEVEL));
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
            List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);

            if (Marking.NEW.equals(userNodeModel.getMarking()) || Marking.NONE.equals(userNodeModel.getMarking())) {
                // add new users + resources or add new group resources to users
                executeRevokeAndGrant(user, userPermissions, selectedPermissions, new HashSet<Permission>(), currentReplacedUserMap.get(user));
                userGroupService.addUserToGroup(user, getSelectedGroup());
            } else {
                if (Marking.REMOVED.equals(userNodeModel.getMarking())) {
                    // remove user and remove unselected resources too
                    executeRevokeAndGrant(user, userPermissions, selectedPermissions, currentRevokedUserMap.get(user), new HashSet<Permission>());
                    userGroupService.removeUserFromGroup(user, getSelectedGroup());
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
