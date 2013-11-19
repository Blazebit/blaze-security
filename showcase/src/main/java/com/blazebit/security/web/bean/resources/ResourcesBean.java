/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.resources;

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

import org.primefaces.event.FlowEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeUnselectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "resourcesBean")
public class ResourcesBean extends ResourceHandlingBaseBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private UserGroupService userGroupService;

    @Inject
    private UserService userService;

    private TreeNode resourceRoot;
    private TreeNode[] selectedResourceNodes = new TreeNode[] {};
    private TreeNode[] selectedUserPermissionNodes = new TreeNode[] {};
    private TreeNode[] selectedGroupPermissionNodes = new TreeNode[] {};

    private List<UserModel> userList = new ArrayList<UserModel>();

    private TreeNode currentUserRoot;
    private TreeNode newUserRoot;

    private TreeNode currentGroupRoot;
    private TreeNode newGroupRoot;
    // selected permissions after the selected resources
    private Set<Permission> selectedResourcePermissions = new HashSet<Permission>();

    private DefaultTreeNode groupRoot;
    private TreeNode[] selectedGroupNodes;

    private Integer activeTabIndex = 0;
    // helper to store granted group permissions
    private Map<UserGroup, Set<Permission>> grantedGroupPermissions = new HashMap<UserGroup, Set<Permission>>();

    public void init() {
        try {
            resourceRoot = getResourceTree();
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            processSelectedResources();
        } else {
            if (event.getOldStep().equals("subject")) {
                if (event.getNewStep().equals("resources")) {
                    return "resources";
                }
                if (Integer.valueOf(0).equals(activeTabIndex)) {
                    if (processResourcesForUsers()) {
                        return "userPermissions";
                    } else {
                        // if there is no selected user stay
                        return "subject";
                    }
                } else {
                    if (Integer.valueOf(1).equals(activeTabIndex)) {
                        if (processResourcesForGroups()) {
                            return "groupPermissions";
                        } else {
                            // if there is no selected group stay
                            return "subject";
                        }
                    }
                }
            } else {
                if (event.getOldStep().equals("groupPermissions")) {
                    // if (event.getNewStep().equals("resources")) {
                    // return "resources";
                    // }
                    if (event.getNewStep().equals("")) {
                        return "subject";
                    }
                    confirmGroupPermissions();
                    return "groupUserPermissions";
                }
            }
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    public void processSelectedResources() {
        selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        initUsers();
        initUserGroups();
    }

    private void initUsers() {
        List<User> allUsers = userService.findUsers(userSession.getSelectedCompany());
        userList.clear();
        for (User user : allUsers) {
            userList.add(new UserModel(user, false));
        }
    }

    private void initUserGroups() {
        List<UserGroup> availableGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
        this.groupRoot = new DefaultTreeNode("", null);
        groupRoot.setExpanded(true);
        for (UserGroup group : availableGroups) {
            createNode(group, groupRoot);
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
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createNode(child, childNode);
        }
    }

    private List<User> getSelectedUsers() {
        List<User> ret = new ArrayList<User>();
        for (UserModel userModel : userList) {
            if (userModel.isSelected()) {
                ret.add(userModel.getUser());
            }
        }
        return ret;
    }

    // resources for users
    private boolean processResourcesForUsers() {
        List<User> selectedUsers = getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            return false;
        }
        createUserPermissionTrees(selectedUsers, true);
        return true;
    }

    private void createUserPermissionTrees(List<User> selectedUsers, boolean selectable) {
        currentUserRoot = new DefaultTreeNode();
        newUserRoot = new DefaultTreeNode();
        for (User user : selectedUsers) {
            List<UserGroup> groupsOfUser = userGroupService.getGroupsForUser(user);
            Set<Permission> selectedPermissions = new HashSet<Permission>();
            for (UserGroup userGroup : groupsOfUser) {
                if (grantedGroupPermissions.containsKey(userGroup)) {
                    selectedPermissions.addAll(grantedGroupPermissions.get(userGroup));
                }
            }

            createCurrentUserNode(user, currentUserRoot, selectedPermissions);
            createNewUserNode(user, newUserRoot, selectedPermissions, selectable);
        }
    }

    private void createCurrentUserNode(User user, TreeNode root, Set<Permission> selectedPermissions) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        createCurrentUserPermissionNode(userNode, selectedPermissions);

    }

    private void createCurrentUserPermissionNode(DefaultTreeNode userNode, Set<Permission> selectedPermissions) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) nodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);

        List<Set<Permission>> grant = getGrantablePermissions(userPermissions, selectedPermissions);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);
        // current permission tree
        if (!userPermissions.isEmpty()) {
            getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);
        } else {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), userNode).setSelectable(false);
        }

    }

    private void createNewUserNode(User user, TreeNode root, Set<Permission> selectedPermissions, boolean selectable) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        createNewUserPermissionNode(userNode, selectedPermissions, selectable);
    }

    private void createNewUserPermissionNode(DefaultTreeNode userNode, Set<Permission> selectedPermissions, boolean selectable) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        User user = (User) nodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(user);
        List<Permission> userPermissions = filterPermissions(permissions).get(0);
        List<Permission> userDataPermissions = filterPermissions(permissions).get(1);

        List<Set<Permission>> grant = getGrantablePermissions(userPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);
        // modify current user permissions based on resource selection
        List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
        // new permission tree without the replaced but with the granted + revoked ones, marked properly
        currentUserPermissions.removeAll(replaced);
        currentUserPermissions.addAll(granted);
        if (selectable) {
            getSelectablePermissionTree(userNode, currentUserPermissions, userDataPermissions, granted, new HashSet<Permission>(), Marking.NEW, Marking.REMOVED);
        } else {
            getPermissionTree(userNode, currentUserPermissions, userDataPermissions, granted, Marking.NEW);
        }
    }

    public void confirmUserPermissions() {
        for (TreeNode userNode : newUserRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            Set<Permission> selectedPermissions;
            if (userSession.getSelectedCompany().isUserLevelEnabled()) {
                selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            } else {
                // workaround because if tree is not displayed the selected nodes are not set
                List<TreeNode> nodes = getAllChildren(userNode.getChildren());
                selectedPermissions = getSelectedPermissions(nodes.toArray(new TreeNode[nodes.size()]));
            }

            List<Permission> userPermissions = permissionManager.getPermissions(user);
            Set<Permission> granted = getGrantablePermissions(userPermissions, selectedPermissions).get(0);
            Set<Permission> finalGranted = grantImpliedPermissions(userPermissions, granted);
            Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);

            for (Permission permission : replaced) {
                permissionService.revoke(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
            for (Permission permission : finalGranted) {
                permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
            }
        }
        init();
    }
    
    // resources for groups
    private boolean processResourcesForGroups() {
        if (selectedGroupNodes.length == 0) {
            return false;
        }
        currentGroupRoot = new DefaultTreeNode();
        newGroupRoot = new DefaultTreeNode();

        for (TreeNode node : selectedGroupNodes) {
            createCurrentGroupNode(currentGroupRoot, (UserGroup) node.getData());
            createNewGroupNode(newGroupRoot, (UserGroup) node.getData());
        }
        return true;
    }

    private void createCurrentGroupNode(TreeNode root, UserGroup userGroup) {
        TreeNodeModel groupNodeModel = new TreeNodeModel(userGroup.getName(), ResourceType.USERGROUP, userGroup);

        DefaultTreeNode currentGroupNode = new DefaultTreeNode(groupNodeModel, currentGroupRoot);
        currentGroupNode.setExpanded(true);
        currentGroupNode.setSelectable(false);

        createCurrentGroupPermissionNode(currentGroupNode, selectedResourcePermissions);

    }

    private void createNewGroupNode(TreeNode root, UserGroup userGroup) {
        TreeNodeModel groupNodeModel = new TreeNodeModel(userGroup.getName(), ResourceType.USERGROUP, userGroup);

        DefaultTreeNode newGroupNode = new DefaultTreeNode(groupNodeModel, newGroupRoot);
        newGroupNode.setExpanded(true);
        newGroupNode.setSelectable(false);

        createNewGroupPermissionNode(newGroupNode, selectedResourcePermissions);
    }

    private void createNewGroupPermissionNode(DefaultTreeNode groupNode, Set<Permission> selectedPermissions) {
        TreeNodeModel nodeModel = (TreeNodeModel) groupNode.getData();
        UserGroup userGroup = (UserGroup) nodeModel.getTarget();
        List<Permission> userGroupPermissions = filterPermissions(permissionManager.getPermissions(userGroup)).get(0);
        List<Permission> userGroupDataPermissions = filterPermissions(permissionManager.getPermissions(userGroup)).get(1);

        List<Set<Permission>> grant = getGrantablePermissions(userGroupPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = getReplacedPermissions(userGroup, granted);
        // modify current user permissions based on resource selection
        List<Permission> currentGroupPermissions = new ArrayList<Permission>(userGroupPermissions);
        // new permission tree without the replaced but with the granted + revoked ones, marked properly
        currentGroupPermissions.removeAll(replaced);
        currentGroupPermissions.addAll(granted);
        getSelectablePermissionTree(groupNode, currentGroupPermissions, userGroupDataPermissions, granted, new HashSet<Permission>(), Marking.NEW, Marking.REMOVED);
    }

    private void createCurrentGroupPermissionNode(DefaultTreeNode groupNode, Set<Permission> selectedPermissions) {
        TreeNodeModel nodeModel = (TreeNodeModel) groupNode.getData();
        UserGroup userGroup = (UserGroup) nodeModel.getTarget();
        List<Permission> permissions = permissionManager.getPermissions(userGroup);
        List<Permission> groupPermissions = filterPermissions(permissions).get(0);
        List<Permission> groupDataPermissions = filterPermissions(permissions).get(1);

        List<Set<Permission>> grant = getGrantablePermissions(groupPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = getReplacedPermissions(userGroup, granted);

        if (!groupPermissions.isEmpty()) {
            getPermissionTree(groupNode, groupPermissions, groupDataPermissions, replaced, Marking.REMOVED);
        } else {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), groupNode).setSelectable(false);
        }

    }

    public void rebuildCurrentUserPermissionTree() {
        for (TreeNode userNode : currentUserRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = filterPermissions(permissions).get(0);
            List<Permission> userDataPermissions = filterPermissions(permissions).get(1);
            // current selected permissions
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes);

            Set<Permission> replaced = getReplacedPermissions(userPermissions, selectedPermissions);
            userNode.getChildren().clear();
            // current permission tree
            getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);
        }
    }

    public void rebuildCurrentGroupPermissionTree() {
        for (TreeNode groupNode : currentGroupRoot.getChildren()) {
            UserGroup userGroup = (UserGroup) ((TreeNodeModel) groupNode.getData()).getTarget();
            List<Permission> permissions = permissionManager.getPermissions(userGroup);
            List<Permission> groupPermissions = filterPermissions(permissions).get(0);
            List<Permission> groupDataPermissions = filterPermissions(permissions).get(1);
            // current selected permissions
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes);

            Set<Permission> granted = getGrantablePermissions(groupPermissions, selectedPermissions).get(0);
            Set<Permission> replaced = getReplacedPermissions(userGroup, granted);
            groupNode.getChildren().clear();
            // current permission tree
            if (!groupPermissions.isEmpty()) {
                getPermissionTree(groupNode, groupPermissions, groupDataPermissions, replaced, Marking.REMOVED);
            } else {
                new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), groupNode).setSelectable(false);
            }
        }
    }

    public void confirmGroupPermissions() {
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode groupNode : newGroupRoot.getChildren()) {
            UserGroup userGroup = (UserGroup) ((TreeNodeModel) groupNode.getData()).getTarget();
            selectedGroups.add(userGroup);

            List<Permission> groupPermissions = permissionManager.getPermissions(userGroup);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes, groupNode);

            Set<Permission> granted = getGrantablePermissions(groupPermissions, selectedPermissions).get(0);
            Set<Permission> finalGranted = grantImpliedPermissions(groupPermissions, granted);
            Set<Permission> replaced = getReplacedPermissions(userGroup, granted);

            for (Permission permission : replaced) {
                permissionService.revoke(userSession.getUser(), userGroup, permission.getAction(), permission.getResource());
            }
            for (Permission permission : finalGranted) {
                permissionService.grant(userSession.getUser(), userGroup, permission.getAction(), permission.getResource());
            }
            grantedGroupPermissions.put(userGroup, finalGranted);
        }
        Set<User> users = new HashSet<User>();
        for (UserGroup group : selectedGroups) {
            collectUsers(group, users);
        }
        List<User> sortedUsers = new ArrayList<User>(users);
        Collections.sort(sortedUsers, new Comparator<User>() {

            @Override
            public int compare(User o1, User o2) {
                return o1.getUsername().compareToIgnoreCase(o2.getUsername());
            }

        });
        createUserPermissionTrees(sortedUsers, userSession.getSelectedCompany().isUserLevelEnabled());
        // if user level is not enabled confirm user permissions immediately
        if (!userSession.getSelectedCompany().isUserLevelEnabled()) {
            confirmUserPermissions();
        }
    }

    private void collectUsers(UserGroup group, Set<User> users) {
        for (User user : userGroupService.getUsersFor(group)) {
            users.add(user);
        }
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            collectUsers(child, users);
        }
    }

    public void userGroupTabChange() {
        if (Integer.valueOf(0).equals(activeTabIndex)) {
            initUsers();
        } else {
            if (Integer.valueOf(1).equals(activeTabIndex)) {
                initUserGroups();
            }
        }
    }

    public void groupUserPermissionListener(NodeUnselectEvent event) {
        currentUserRoot = new DefaultTreeNode();
        for (TreeNode groupNode : selectedGroupNodes) {
            UserGroup group = (UserGroup) groupNode.getData();
            for (User user : userGroupService.getUsersFor(group)) {
                TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
                DefaultTreeNode currentUserNode = new DefaultTreeNode(userNodeModel, currentUserRoot);
                currentUserNode.setExpanded(true);
                createCurrentUserPermissionNode(currentUserNode, getSelectedPermissions(selectedUserPermissionNodes));
            }
        }
    }

    public void groupUserPermissionListener(NodeSelectEvent event) {
        currentUserRoot = new DefaultTreeNode();
        for (TreeNode groupNode : selectedGroupNodes) {
            UserGroup group = (UserGroup) groupNode.getData();
            for (User user : userGroupService.getUsersFor(group)) {
                TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
                DefaultTreeNode currentUserNode = new DefaultTreeNode(userNodeModel, currentUserRoot);
                currentUserNode.setExpanded(true);
                createCurrentGroupPermissionNode(currentUserNode, getSelectedPermissions(selectedUserPermissionNodes));
            }
        }
    }

    public TreeNode getResourceRoot() {
        return resourceRoot;
    }

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedResourceNodes = selectedPermissionNodes;
    }

    public Integer getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(Integer activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }

    public List<UserModel> getUserList() {
        return userList;
    }

    public TreeNode getCurrentUserRoot() {
        return currentUserRoot;
    }

    public TreeNode getNewUserRoot() {
        return newUserRoot;
    }

    public TreeNode getCurrentGroupRoot() {
        return currentGroupRoot;
    }

    public TreeNode getNewGroupRoot() {
        return newGroupRoot;
    }

    public DefaultTreeNode getGroupRoot() {
        return groupRoot;
    }

    public TreeNode[] getSelectedGroupNodes() {
        return selectedGroupNodes;
    }

    public void setSelectedGroupNodes(TreeNode[] selectedGroupNodes) {
        this.selectedGroupNodes = selectedGroupNodes;
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
