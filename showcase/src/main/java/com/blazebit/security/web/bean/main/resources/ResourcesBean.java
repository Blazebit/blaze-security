/*
 * \ * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.resources;

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

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.service.resource.UserGroupDataAccess;
import com.blazebit.security.web.bean.base.ResourceGroupHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.service.api.UserService;

@ViewScoped
@ManagedBean(name = "resourcesBean")
public class ResourcesBean extends ResourceGroupHandlingBaseBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private UserGroupDataAccess userGroupDataAccess;

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
    private List<UserGroupModel> groups = new ArrayList<UserGroupModel>();
    private TreeNode[] selectedGroupNodes;

    private Integer activeTabIndex = 0;
    // helper to store granted group permissions
    private Map<User, List<Permission>> userPermissionMap = new HashMap<User, List<Permission>>();
    private Map<UserGroup, Set<Permission>> grantedGroupPermissions = new HashMap<UserGroup, Set<Permission>>();
    private Map<UserGroup, List<Permission>> groupPermissionMap = new HashMap<UserGroup, List<Permission>>();
    private Map<UserGroup, Set<Permission>> groupReplaceables = new HashMap<UserGroup, Set<Permission>>();

    private String filter;

    public void filterTree() {
        try {
            resourceRoot = getResourceTree(filter);
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
    }

    public void init() {
        filter = "";
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
                    processGroupPermissions();
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
        userPermissionMap.clear();
        List<User> allUsers = userService.findUsers(userContext.getUser().getCompany());
        userList.clear();
        for (User user : allUsers) {
            userList.add(new UserModel(user, false));
            userPermissionMap.put(user, permissionManager.getPermissions(user));
        }
    }

    private void initUserGroups() {
        grantedGroupPermissions.clear();
        groupReplaceables.clear();
        groupPermissionMap.clear();
        List<UserGroup> parentGroups = userGroupDataAccess.getAllParentGroups(userContext.getUser().getCompany());
        this.groupRoot = new DefaultTreeNode("", null);
        groupRoot.setExpanded(true);
        for (UserGroup group : parentGroups) {
            storePermissions(group);
            createGroupNode(group, groupRoot);
        }
        this.groups.clear();
        for (UserGroup group : userGroupDataAccess.getAllGroups(userContext.getUser().getCompany())) {
            this.groups.add(new UserGroupModel(group, false, false));
        }
    }

    private void storePermissions(UserGroup group) {
        groupPermissionMap.put(group, permissionManager.getPermissions(group));
        for (UserGroup child : userGroupDataAccess.getGroupsForGroup(group)) {
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
        for (UserGroup child : userGroupDataAccess.getGroupsForGroup(group)) {
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
            createUserNodes(user, currentUserRoot, newUserRoot, selectedResourcePermissions);
        }
        return true;
    }

    private void createUserNodes(User user, TreeNode currentRoot, TreeNode newRoot, Set<Permission> selectedPermissions) {

        List<Permission> permissions = userPermissionMap.get(user);
        List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
        List<Permission> userDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);

        DefaultTreeNode userNode = new DefaultTreeNode(userNodeModel, currentRoot);
        userNode.setExpanded(true);
        userNode.setSelectable(false);

        List<Set<Permission>> grant = permissionHandling.getGrantable(userPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        dialogBean.setNotGranted(grant.get(1));
        dialogBean.setNotRevoked(new HashSet<Permission>());

        Set<Permission> replaced = permissionHandling.getReplacedByGranting(permissions, granted);
        replacables.put(user, replaced);

        buildCurrentPermissionTree(userNode, userPermissions, userDataPermissions, new HashSet<Permission>(), replaced, !isEnabled(Company.FIELD_LEVEL));

        DefaultTreeNode newUserNode = new DefaultTreeNode(userNodeModel, newRoot);
        newUserNode.setExpanded(true);
        newUserNode.setSelectable(false);

        buildNewPermissionTree(newUserNode, userPermissions, userDataPermissions, granted, new HashSet<Permission>(), replaced, !isEnabled(Company.FIELD_LEVEL),
                               isEnabled(Company.USER_LEVEL), true);

    }

    public void rebuildCurrentUserPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        rebuildCurrentUserPermissionTree();
    }

    public void rebuildCurrentUserPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
        rebuildCurrentUserPermissionTree();
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
            rebuildCurrentTree(userNode, permissions, selectedPermissions, new HashSet<Permission>(), replacables.get(user), !isEnabled(Company.FIELD_LEVEL));
        }
    }

    public void rebuildCurrentGroupPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        rebuildCurrentGroupPermissionTree();
    }

    public void rebuildCurrentGroupPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
        rebuildCurrentGroupPermissionTree();
    }

    public void rebuildCurrentGroupPermissionTree() {
        for (TreeNode groupNode : currentGroupRoot.getChildren()) {
            UserGroup userGroup = (UserGroup) ((TreeNodeModel) groupNode.getData()).getTarget();

            List<Permission> permissions = groupPermissionMap.get(userGroup);
            // current selected permissions
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
            groupNode.getChildren().clear();
            // add previously replaced permissions
            rebuildCurrentTree(groupNode, permissions, selectedPermissions, new HashSet<Permission>(), groupReplaceables.get(userGroup), !isEnabled(Company.FIELD_LEVEL));
        }
    }

    /**
     * wizard step: confirm permissions for user
     */
    public void confirmPermissions() {
        // first confirm group permissions
        for (UserGroup userGroup : grantedGroupPermissions.keySet()) {
            revokeAndGrant(userGroup, new HashSet<Permission>(), grantedGroupPermissions.get(userGroup), false);
        }

        for (TreeNode userNode : newUserRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            List<Permission> allPermissions = permissionManager.getPermissions(user);
            // now confirm groups and users
            executeRevokeAndGrant(user, allPermissions, selectedPermissions, new HashSet<Permission>(), replacables.get(user));
        }
        init();
    }

    // resources for groups
    private boolean processResourcesForGroups() {
        if (getSelectedGroups().isEmpty()) {
            return false;
        }
        currentGroupRoot = new DefaultTreeNode();
        newGroupRoot = new DefaultTreeNode();

        for (UserGroup userGroup : getSelectedGroups()) {
            createGroupNodes(userGroup, currentGroupRoot, newGroupRoot, selectedResourcePermissions);
        }
        return true;
    }

    private void createGroupNodes(UserGroup group, TreeNode currentRoot, TreeNode newRoot, Set<Permission> selectedPermissions) {

        List<Permission> permissions = groupPermissionMap.get(group);
        List<Permission> groupPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
        List<Permission> groupDataPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(1);
        TreeNodeModel userNodeModel = new TreeNodeModel(group.getName(), ResourceType.USERGROUP, group);

        DefaultTreeNode groupNode = new DefaultTreeNode(userNodeModel, currentRoot);
        groupNode.setExpanded(true);
        groupNode.setSelectable(false);

        List<Set<Permission>> grant = permissionHandling.getGrantable(groupPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        dialogBean.setNotGranted(grant.get(1));
        dialogBean.setNotRevoked(new HashSet<Permission>());

        Set<Permission> replaced = permissionHandling.getReplacedByGranting(permissions, granted);
        groupReplaceables.put(group, replaced);

        buildCurrentPermissionTree(groupNode, groupPermissions, groupDataPermissions, new HashSet<Permission>(), replaced, !isEnabled(Company.FIELD_LEVEL));

        DefaultTreeNode newGroupNode = new DefaultTreeNode(userNodeModel, newRoot);
        newGroupNode.setExpanded(true);
        newGroupNode.setSelectable(false);

        buildNewPermissionTree(newGroupNode, groupPermissions, groupDataPermissions, granted, new HashSet<Permission>(), replaced, !isEnabled(Company.FIELD_LEVEL), true, true);

    }

    private Set<UserGroup> getSelectedGroups() {
        Set<UserGroup> ret = new HashSet<UserGroup>();
        if (isEnabled(Company.GROUP_HIERARCHY)) {
            for (TreeNode treeNode : selectedGroupNodes) {
                UserGroup userGroup = (UserGroup) treeNode.getData();
                ret.add(userGroup);
            }
        } else {
            for (UserGroupModel model : groups) {
                if (model.isSelected()) {
                    ret.add(model.getUserGroup());
                }
            }
        }
        return ret;
    }

    // confirm groups
    public void processGroupPermissions() {
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode groupNode : newGroupRoot.getChildren()) {
            UserGroup userGroup = (UserGroup) ((TreeNodeModel) groupNode.getData()).getTarget();
            selectedGroups.add(userGroup);

            List<Permission> permissions = permissionManager.getPermissions(userGroup);
            List<Permission> groupPermissions = resourceUtils.getSeparatedPermissionsByResource(permissions).get(0);
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes, groupNode);

            Set<Permission> finalGranted = executeRevokeAndGrant(userGroup, groupPermissions, selectedPermissions, new HashSet<Permission>(), groupReplaceables.get(userGroup),
                                                                 true).get(1);

            // to be propagated to users
            grantedGroupPermissions.put(userGroup, finalGranted);
        }

        currentUserRoot = new DefaultTreeNode();
        newUserRoot = new DefaultTreeNode();
        prepareUserPropagationView(selectedGroups, grantedGroupPermissions, Collections.<UserGroup, Set<Permission>>emptyMap(), currentUserRoot, newUserRoot,
                                   selectedUserPermissionNodes);
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

    public List<UserGroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<UserGroupModel> groups) {
        this.groups = groups;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

}
