/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.group;

import java.util.ArrayList;
import java.util.Collections;
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

import com.blazebit.security.entity.EntityPermissionUtils;
import com.blazebit.security.model.Features;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.showcase.service.UserService;
import com.blazebit.security.web.bean.base.GroupHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserModel;

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
    private DefaultTreeNode newObjectPermissionRoot;
    private DefaultTreeNode currentPermissionRoot;

    private DefaultTreeNode permissionTreeViewRoot;

    private Set<Permission> selectedGroupPermissions = new HashSet<Permission>();

    private TreeNode[] selectedUserNodes = new TreeNode[] {};
    private TreeNode[] selectedObjectUserNodes = new TreeNode[] {};

    private Map<User, Set<Permission>> replacables = new HashMap<User, Set<Permission>>();
    private Map<User, Set<Permission>> revokables = new HashMap<User, Set<Permission>>();

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
        selectedGroupPermissions = groupPermissionHandling.getGroupPermissions(addedGroups, isEnabled(Features.GROUP_HIERARCHY));
        if (!isEnabled(Features.FIELD_LEVEL)) {
            selectedGroupPermissions = permissionHandling.getSeparatedParentAndChildPermissions(selectedGroupPermissions).get(0);
        }
        // if object level is not enabled, just ignore object level permissions
        if (!isEnabled(Features.OBJECT_LEVEL)) {
            selectedGroupPermissions = new HashSet<Permission>(permissionHandling.getSeparatedPermissions(selectedGroupPermissions).get(0));
        }
    }

    private void initPermissions() {
        permissionTreeViewRoot = initGroupPermissions(getSelectedGroup(), !isEnabled(Features.FIELD_LEVEL));
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
        if (!event.getOldStep().equals(event.getNewStep()) && event.getOldStep().equals("users")) {
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

        newObjectPermissionRoot = new DefaultTreeNode();

        for (UserModel userModel : userList) {
            List<Permission> permissions = permissionManager.getPermissions(userModel.getUser());
            List<Permission> userPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(permissions).get(0);
            List<Permission> userDataPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(permissions).get(1);

            if (userModel.isSelected()) {
                // user is selected
                createCurrentUserNode(currentPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, true, !users.contains(userModel.getUser()), Marking.NEW);
                createNewUserNode(newPermissionRoot, newObjectPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, true,
                                  !users.contains(userModel.getUser()), Marking.NEW);
            } else {
                if (users.contains(userModel.getUser())) {
                    // user will be removed from group-> mark it red
                    createCurrentUserNode(currentPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, false, true, Marking.REMOVED);
                    createNewUserNode(newPermissionRoot, newObjectPermissionRoot, userModel.getUser(), userPermissions, userDataPermissions, false, true, Marking.REMOVED);

                }
            }
        }

        if (!isEnabled(Features.USER_LEVEL)) {
            confirmPermissions();
        }
    }

    private TreeNode createCurrentUserNode(TreeNode permissionRoot, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, boolean addedUser, boolean marked, Marking marking) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        if (marked) {
            userNodeModel.setMarking(marking);
        }
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);
        if (addedUser) {
            createCurrentPermissionTreeForAddedUser(user, userNode, userPermissions, userDataPermissions);
        } else {
            createCurrentPermissionTreeForRemovedUser(userNode, userPermissions, userDataPermissions);
        }
        return userNode;
    }

    private void createCurrentPermissionTreeForAddedUser(User user, DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        // added user receives new permissions from groups -> mark only
        // replaceable
        Set<Permission> granted = permissionHandling.getGrantable(concat(userPermissions, userDataPermissions), selectedGroupPermissions).get(0);
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(concat(userPermissions, userDataPermissions), granted);
        replacables.put(user, replaced);

        buildCurrentPermissionTree(userNode, userPermissions, userDataPermissions, new HashSet<Permission>(), replaced, !isEnabled(Features.FIELD_LEVEL));
    }

    private void createCurrentPermissionTreeForRemovedUser(DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        // group permissions will be revoked from user-> mark only revokables
        Set<Permission> revoked = permissionHandling.getRevokableFromRevoked(userPermissions, selectedGroupPermissions, true).get(0);
        buildCurrentPermissionTree(userNode, userPermissions, userDataPermissions, revoked, new HashSet<Permission>(), !isEnabled(Features.FIELD_LEVEL));
    }

    private TreeNode createNewUserNode(TreeNode permissionRoot, TreeNode objectPermissionRoot, User user, List<Permission> userPermissions, List<Permission> userDataPermissions, boolean addedUser, boolean marked, Marking marking) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        if (marked) {
            userNodeModel.setMarking(marking);
        }
        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, permissionRoot);
        userNode.setExpanded(true/* addedUser */);
        userNode.setSelectable(false);

        DefaultTreeNode userObjectNode = new DefaultTreeNode(userNodeModel, objectPermissionRoot);
        userObjectNode.setExpanded(true/* addedUser */);
        userObjectNode.setSelectable(false);

        if (addedUser) {
            createNewPermissionTreeForAddedUser(user, userNode, userPermissions, userObjectNode, userDataPermissions);
        } else {
            createNewPermissionTreeForRemovedUser(user, userNode, userPermissions, userObjectNode, userDataPermissions);
        }
        return userNode;
    }

    private void createNewPermissionTreeForAddedUser(User user, DefaultTreeNode userNode, List<Permission> userPermissions, DefaultTreeNode userObjectNode, List<Permission> userDataPermissions) {
        Set<Permission> granted = permissionHandling.getGrantable(concat(userPermissions, userDataPermissions), selectedGroupPermissions).get(0);
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(userPermissions, granted);

        buildNewPermissionTree(userNode, userPermissions, Collections.<Permission>emptyList(), new HashSet<Permission>(permissionHandling.getSeparatedPermissions(granted).get(0)),
                         new HashSet<Permission>(), replaced, !isEnabled(Features.FIELD_LEVEL), isEnabled(Features.USER_LEVEL), false);

        buildNewDataPermissionTree(userObjectNode, Collections.<Permission>emptyList(), userDataPermissions,
                                   new HashSet<Permission>(permissionHandling.getSeparatedPermissions(granted).get(1)), Collections.<Permission>emptySet(), replaced,
                                   !isEnabled(Features.FIELD_LEVEL), isEnabled(Features.USER_LEVEL));
    }

    // TODO simplify? how?
    private void createNewPermissionTreeForRemovedUser(User user, DefaultTreeNode userNode, List<Permission> userPermissions, DefaultTreeNode userObjectNode, List<Permission> userDataPermissions) {
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
        revokables.put(user, revoked);

        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        currentPermissions.addAll(permissionHandling.getSeparatedPermissions(granted).get(0));
        currentPermissions.addAll(permissionHandling.getSeparatedPermissions(toRevoke).get(0));

        Set<Permission> currentDataPermissions = new HashSet<Permission>(userDataPermissions);
        currentDataPermissions.addAll(permissionHandling.getSeparatedPermissions(granted).get(1));
        currentDataPermissions.addAll(permissionHandling.getSeparatedPermissions(toRevoke).get(1));

        currentPermissions = new HashSet<Permission>(permissionHandling.removeAll(currentPermissions, impliedBy));
        currentDataPermissions = new HashSet<Permission>(permissionHandling.removeAll(currentDataPermissions, impliedBy));

        if (isEnabled(Features.USER_LEVEL)) {
            getMutablePermissionTree(userNode, new ArrayList<Permission>(currentPermissions), Collections.<Permission>emptyList(), granted, revoked, Marking.NEW, Marking.REMOVED,
                                     !isEnabled(Features.FIELD_LEVEL), false);
            buildNewDataPermissionTree(userObjectNode, Collections.<Permission>emptyList(), new ArrayList<Permission>(currentDataPermissions), granted, revoked, Collections.<Permission>emptySet(),
                                       !isEnabled(Features.FIELD_LEVEL), true);
        } else {
            Set<Permission> currentUserPermissions = new HashSet<Permission>(userPermissions);
            currentUserPermissions = new HashSet<Permission>(permissionHandling.removeAll(currentUserPermissions, revoked));
            currentUserPermissions.addAll(granted);
            getImmutablePermissionTree(userNode, new ArrayList<Permission>(currentUserPermissions), new ArrayList<Permission>(), currentUserPermissions, Marking.NONE);
        }
    }

    public void rebuildCurrentPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        TreeNode selectedNode = event.getTreeNode();
        selectChildrenInstances(selectedNode, true);
        rebuildCurrentPermissionTree();
    }

    public void rebuildCurrentPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
        TreeNode selectedNode = event.getTreeNode();
        selectChildrenInstances(selectedNode, true);
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
            selectedPermissions.addAll(getSelectedPermissions(selectedObjectUserNodes));
            userNode.getChildren().clear();
            if (Marking.NEW.equals(userNodeModel.getMarking()) || Marking.NONE.equals(userNodeModel.getMarking())) {
                // this is an added or an existing user
                rebuildCurrentTree(userNode, permissions, selectedPermissions, new HashSet<Permission>(), replacables.get(user), !isEnabled(Features.FIELD_LEVEL));

            } else {
                // this is a removed user
                rebuildCurrentTree(userNode, permissions, selectedPermissions, revokables.get(user), new HashSet<Permission>(), !isEnabled(Features.FIELD_LEVEL));
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
            List<Permission> userPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserNodes, userNode);
            selectedPermissions.addAll(getSelectedPermissions(selectedObjectUserNodes, userNode));

            if (Marking.NEW.equals(userNodeModel.getMarking()) || Marking.NONE.equals(userNodeModel.getMarking())) {
                // add new users + resources or add new group resources to users
                executeRevokeAndGrant(user, allPermissions, selectedPermissions, new HashSet<Permission>(), replacables.get(user));
                userGroupService.addUserToGroup(user, getSelectedGroup());
            } else {
                if (Marking.REMOVED.equals(userNodeModel.getMarking())) {
                    // remove user and remove unselected resources too
                    executeRevokeAndGrant(user, userPermissions, selectedPermissions, revokables.get(user), new HashSet<Permission>());
                    userGroupService.removeUserFromGroup(user, getSelectedGroup());
                }
            }
        }
        initUsers();
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

    public TreeNode[] getSelectedObjectUserNodes() {
        return selectedObjectUserNodes;
    }

    public void setSelectedObjectUserNodes(TreeNode[] selectedObjectUserNodes) {
        this.selectedObjectUserNodes = selectedObjectUserNodes;
    }

    public DefaultTreeNode getNewObjectPermissionRoot() {
        return newObjectPermissionRoot;
    }

    public void setNewObjectPermissionRoot(DefaultTreeNode newObjectPermissionRoot) {
        this.newObjectPermissionRoot = newObjectPermissionRoot;
    }

}
