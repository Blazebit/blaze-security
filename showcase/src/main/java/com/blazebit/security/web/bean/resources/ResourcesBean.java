/*
 * \ * To change this template, choose Tools | Templates and open the template in the editor.
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

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.Company;
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

    private Map<User, List<Permission>> userPermissionMap = new HashMap<User, List<Permission>>();
    private Map<UserGroup, List<Permission>> groupPermissionMap = new HashMap<UserGroup, List<Permission>>();

    private Map<User, Set<Permission>> currentReplacedUserMap = new HashMap<User, Set<Permission>>();
    private Map<UserGroup, Set<Permission>> currentReplacedGroupMap = new HashMap<UserGroup, Set<Permission>>();

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
            userPermissionMap.put(user, permissionManager.getPermissions(user));
        }
    }

    private void initUserGroups() {
        List<UserGroup> parentGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
        this.groupRoot = new DefaultTreeNode("", null);
        groupRoot.setExpanded(true);
        for (UserGroup group : parentGroups) {
            storePermissions(group);
            createGroupNode(group, groupRoot);
        }
    }

    private void storePermissions(UserGroup group) {
        groupPermissionMap.put(group, permissionManager.getPermissions(group));
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            storePermissions(child);
        }
    }

    /**
     * helper to build tree
     * 
     * @param group
     * @param node
     */
    private void createGroupNode(UserGroup group, DefaultTreeNode node) {
        DefaultTreeNode childNode = new DefaultTreeNode(group, node);
        childNode.setExpanded(true);
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createGroupNode(child, childNode);
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
        // create user trees
        currentUserRoot = new DefaultTreeNode();
        newUserRoot = new DefaultTreeNode();
        for (User user : selectedUsers) {

            List<Permission> permissions = userPermissionMap.get(user);
            List<Permission> userPermissions = permissionHandlingUtils.filterPermissions(permissions).get(0);
            List<Permission> userDataPermissions = permissionHandlingUtils.filterPermissions(permissions).get(1);

            createCurrentUserNode(user, currentUserRoot, selectedResourcePermissions, userPermissions, userDataPermissions);
            // if authorizer was able to select users they are selectable, no need to check for user level enabled
            createNewUserNode(user, newUserRoot, selectedResourcePermissions, userPermissions, true);
        }

        return true;
    }

    private void createCurrentUserNode(User user, TreeNode root, Set<Permission> selectedPermissions, List<Permission> userPermissions, List<Permission> userDataPermissions) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        createCurrentUserPermissionNode(user, userNode, selectedPermissions, userPermissions, userDataPermissions);

    }

    private void createCurrentUserPermissionNode(User user, DefaultTreeNode userNode, Set<Permission> selectedPermissions, List<Permission> userPermissions, List<Permission> userDataPermissions) {

        List<Set<Permission>> grant = permissionHandlingUtils.getGrantableFromSelected(userPermissions, selectedPermissions);
        super.setNotGranted(grant.get(1));

        List<Permission> permissions = new ArrayList<Permission>();
        permissions.addAll(userPermissions);
        permissions.addAll(userDataPermissions);

        Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(permissions, selectedPermissions);
        
        // current permission tree
        getPermissionTree(userNode, userPermissions, userDataPermissions, replaced, Marking.REMOVED);
    }

    private void createNewUserNode(User user, TreeNode root, Set<Permission> selectedPermissions, List<Permission> userPermissions, boolean selectable) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, root);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        createNewUserPermissionNode(user, userNode, selectedPermissions, userPermissions, selectable);
    }

    private void createNewUserPermissionNode(User user, DefaultTreeNode userNode, Set<Permission> selectedPermissions, List<Permission> userPermissions, boolean selectable) {

        List<Set<Permission>> grant = permissionHandlingUtils.getGrantableFromSelected(userPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        // TODO fix this to put it per user
        super.setNotGranted(grant.get(1));

        Set<Permission> replacedByGranting = permissionHandlingUtils.getReplacedByGranting(userPermissions, selectedPermissions);
        currentReplacedUserMap.put(user, replacedByGranting);

        // modify current user permissions based on resource selection
        List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
        // new permission tree without the replaced but with the granted + revoked ones, marked properly
        currentUserPermissions.removeAll(replacedByGranting);
        currentUserPermissions.addAll(granted);

        if (selectable) {
            getSelectablePermissionTree(userNode, currentUserPermissions, new ArrayList<Permission>(), granted, new HashSet<Permission>(), Marking.NEW, Marking.REMOVED);
        } else {
            // workaround because if tree is not displayed the selected nodes are not set
            getPermissionTree(userNode, currentUserPermissions, new ArrayList<Permission>(), granted, Marking.NEW);
            selectedUserPermissionNodes = (TreeNode[]) ArrayUtils.addAll(selectedUserPermissionNodes, getSelectedNodes(userNode.getChildren()).toArray());
        }
    }

    /**
     * rebuild user tree
     */
    public void rebuildCurrentUserPermissionTree() {
        for (TreeNode userNode : currentUserRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();

            List<Permission> permissions = userPermissionMap.get(user);
            // current selected permissions
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            // current permission tree
            userNode.getChildren().clear();
            rebuildCurrentTree(userNode, permissions, selectedPermissions, new HashSet<Permission>(), currentReplacedUserMap.get(user));
        }
    }

    /**
     * wizard step: confirm permissions for user
     */
    public void confirmUserPermissions() {
        for (TreeNode userNode : newUserRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            List<Permission> allPermissions = permissionManager.getPermissions(user);
            performRevokeAndGrant(user, allPermissions, selectedPermissions, new HashSet<Permission>(), currentReplacedUserMap.get(user));
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
            UserGroup userGroup = (UserGroup) node.getData();

            List<Permission> allPermissions = groupPermissionMap.get(userGroup);
            List<Permission> groupPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(0);
            List<Permission> groupDataPermissions = permissionHandlingUtils.filterPermissions(allPermissions).get(1);

            createCurrentGroupNode(currentGroupRoot, userGroup, groupPermissions, groupDataPermissions);
            createNewGroupNode(newGroupRoot, userGroup, groupPermissions);
        }
        return true;
    }

    private void createCurrentGroupNode(TreeNode root, UserGroup userGroup, List<Permission> groupPermissions, List<Permission> groupDataPermissions) {
        TreeNodeModel groupNodeModel = new TreeNodeModel(userGroup.getName(), ResourceType.USERGROUP, userGroup);

        DefaultTreeNode currentGroupNode = new DefaultTreeNode(groupNodeModel, currentGroupRoot);
        currentGroupNode.setExpanded(true);
        currentGroupNode.setSelectable(false);

        createCurrentGroupPermissionNode(currentGroupNode, selectedResourcePermissions, groupPermissions, groupDataPermissions);
    }

    private void createCurrentGroupPermissionNode(DefaultTreeNode groupNode, Set<Permission> selectedPermissions, List<Permission> groupPermissions, List<Permission> groupDataPermissions) {
        Set<Permission> replaced = permissionHandlingUtils.getReplacedByGranting(groupPermissions, selectedPermissions);

        if (!groupPermissions.isEmpty()) {
            getPermissionTree(groupNode, groupPermissions, groupDataPermissions, replaced, Marking.REMOVED);
        } else {
            new DefaultTreeNode(new TreeNodeModel("No permissions available", null, null), groupNode).setSelectable(false);
        }

    }

    private void createNewGroupNode(TreeNode root, UserGroup userGroup, List<Permission> groupPermissions) {
        TreeNodeModel groupNodeModel = new TreeNodeModel(userGroup.getName(), ResourceType.USERGROUP, userGroup);

        DefaultTreeNode newGroupNode = new DefaultTreeNode(groupNodeModel, newGroupRoot);
        newGroupNode.setExpanded(true);
        newGroupNode.setSelectable(false);

        createNewGroupPermissionNode(userGroup, newGroupNode, selectedResourcePermissions, groupPermissions);
    }

    private void createNewGroupPermissionNode(UserGroup userGroup, DefaultTreeNode groupNode, Set<Permission> selectedPermissions, List<Permission> groupPermissions) {
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantableFromSelected(groupPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        // TODO fix this to put it per group
        super.setNotGranted(grant.get(1));

        Set<Permission> currentReplaced = permissionHandlingUtils.getReplacedByGranting(groupPermissions, granted);
        currentReplacedGroupMap.put(userGroup, currentReplaced);
        // modify current user permissions based on resource selection
        List<Permission> currentGroupPermissions = new ArrayList<Permission>(groupPermissions);
        // new permission tree without the replaced but with the granted + revoked ones, marked properly
        currentGroupPermissions.removeAll(currentReplaced);
        currentGroupPermissions.addAll(granted);
        getSelectablePermissionTree(groupNode, currentGroupPermissions, new ArrayList<Permission>(), granted, new HashSet<Permission>(), Marking.NEW, Marking.REMOVED);
    }

    public void rebuildCurrentGroupPermissionTree() {
        for (TreeNode groupNode : currentGroupRoot.getChildren()) {
            UserGroup userGroup = (UserGroup) ((TreeNodeModel) groupNode.getData()).getTarget();

            List<Permission> permissions = groupPermissionMap.get(userGroup);
            // current selected permissions
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes);
            groupNode.getChildren().clear();
            // add previously replaced permissions
            rebuildCurrentTree(groupNode, permissions, selectedPermissions, new HashSet<Permission>(), currentReplacedGroupMap.get(userGroup));
        }
    }

    // confirm groups
    public void confirmGroupPermissions() {
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode groupNode : newGroupRoot.getChildren()) {
            UserGroup userGroup = (UserGroup) ((TreeNodeModel) groupNode.getData()).getTarget();
            selectedGroups.add(userGroup);

            List<Permission> permissions = permissionManager.getPermissions(userGroup);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes, groupNode);
            Set<Permission> finalGranted = performRevokeAndGrant(userGroup, permissions, selectedPermissions, new HashSet<Permission>(), currentReplacedGroupMap.get(userGroup))
                .get(1);

            // to be propagated to users
            grantedGroupPermissions.put(userGroup, finalGranted);
        }

        prepareUserPropagationView(selectedGroups);
    }

    private void prepareUserPropagationView(Set<UserGroup> selectedGroups) {
        currentReplacedUserMap.clear();
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
        createUserPermissionTreesAfterGroupConfirmation(sortedUsers, Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL)));
        // if user level is not enabled confirm user permissions immediately
        if (!Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.USER_LEVEL))) {
            confirmUserPermissions();
        }
    }

    private void createUserPermissionTreesAfterGroupConfirmation(List<User> selectedUsers, boolean selectable) {
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
            List<Permission> permissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = permissionHandlingUtils.filterPermissions(permissions).get(0);
            List<Permission> userDataPermissions = permissionHandlingUtils.filterPermissions(permissions).get(1);
            selectedPermissions = permissionHandlingUtils.getNormalizedPermissions(selectedPermissions);
            createCurrentUserNode(user, currentUserRoot, selectedPermissions, userPermissions, userDataPermissions);
            createNewUserNode(user, newUserRoot, selectedPermissions, userPermissions, selectable);
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
