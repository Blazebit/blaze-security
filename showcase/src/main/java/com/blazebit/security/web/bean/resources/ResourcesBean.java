/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeUnselectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.service.api.UserService;
import com.blazebit.security.web.util.Constants;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "resourcesBean")
public class ResourcesBean extends ResourceHandlingBaseBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    private UserGroupService userGroupService;

    @Inject
    private UserService userService;

    @Inject
    private UserSession userSession;

    @Inject
    private PermissionService permissionService;

    private TreeNode resourceRoot;
    private TreeNode[] selectedPermissionNodes = new TreeNode[] {};
    private TreeNode[] selectedUserPermissionNodes = new TreeNode[] {};

    private List<UserModel> userList = new ArrayList<UserModel>();

    private TreeNode currentUserRoot;
    private TreeNode newUserRoot;
    private TreeNode currentGroupRoot;
    private TreeNode newGroupRoot;

    private Set<Permission> selectedPermissions = new HashSet<Permission>();
    private DefaultTreeNode groupRoot;
    private TreeNode[] selectedGroupNodes;

    private Integer activeTabIndex = 0;

    public void init() {
        resourceRoot = getResourceTree();
    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            processSelectedPermissions();
        } else {
            if (event.getOldStep().equals("permissions")) {
                if (event.getNewStep().equals("resources")) {
                    return "resources";
                }
                if (Integer.valueOf(0).equals(activeTabIndex)) {
                    processPermissionsForUsers();

                    return "userPermissions";
                } else {
                    if (Integer.valueOf(1).equals(activeTabIndex)) {
                        processPermissionsForGroups();
                        return "groupPermissions";
                    }
                }
            } else {
                if (event.getOldStep().equals("groupPermissions")) {
                    // if (event.getNewStep().equals("resources")) {
                    // return "resources";
                    // }
                    if (event.getNewStep().equals("")) {
                        return "permissions";
                    }
                    confirmGroupPermissions();
                    return "groupUserPermissions";
                }
            }
        }
        return event.getNewStep();
    }

    private void processPermissionsForGroups() {
        for (TreeNode node : selectedGroupNodes) {
            createGroupNode((UserGroup) node.getData());
        }
    }

    private void createGroupNode(UserGroup userGroup) {
        TreeNodeModel groupNodeModel = new TreeNodeModel(userGroup.getName(), ResourceType.USERGROUP, userGroup);
        currentGroupRoot = new DefaultTreeNode();
        DefaultTreeNode currentGroupNode = new DefaultTreeNode(groupNodeModel, currentGroupRoot);
        currentGroupNode.setExpanded(true);
        newGroupRoot = new DefaultTreeNode();
        DefaultTreeNode newGroupNode = new DefaultTreeNode(groupNodeModel, newGroupRoot);
        newGroupNode.setExpanded(true);
        createCurrentPermissionNode(currentGroupNode, selectedPermissions);
        createNewPermissionNode(newGroupNode);
    }

    private void processPermissionsForUsers() {
        currentUserRoot = new DefaultTreeNode();
        newUserRoot = new DefaultTreeNode();
        for (UserModel userModel : userList) {
            if (userModel.isSelected()) {
                createUserNode(userModel.getUser());
            }
        }
    }

    private void createUserNode(User user) {
        TreeNodeModel userNodeModel = new TreeNodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode currentUserNode = new DefaultTreeNode(userNodeModel, currentUserRoot);
        currentUserNode.setExpanded(true);
        DefaultTreeNode newUserNode = new DefaultTreeNode(userNodeModel, newUserRoot);
        newUserNode.setSelectable(false);
        newUserNode.setExpanded(true);
        createCurrentPermissionNode(currentUserNode, selectedPermissions);
        createNewPermissionNode(newUserNode);
    }

    private void createNewPermissionNode(DefaultTreeNode userNode) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        List<Permission> permissions = null;
        Set<Permission> revokablePermissions = null;
        if (nodeModel.getTarget() instanceof User) {
            User user = (User) nodeModel.getTarget();
            permissions = permissionManager.getPermissions(user);
            revokablePermissions = getReplaceablePermissions(user, permissions, selectedPermissions);
        } else {
            if (nodeModel.getTarget() instanceof UserGroup) {
                UserGroup group = (UserGroup) nodeModel.getTarget();
                permissions = permissionManager.getPermissions(group);
                revokablePermissions = getReplaceablePermissions(group, permissions, selectedPermissions);
            }
        }
        permissions = (List<Permission>) removeAll(permissions, revokablePermissions);
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions, selectedPermissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), userNode);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission permission : permissionsByAction) {
                    if (!((EntityField) permission.getResource()).isEmptyField()) {
                        // decide marking
                        Marking marking = Marking.NONE;
                        if (!contains(permissions, permission) && contains(selectedPermissions, permission)) {
                            marking = Marking.GREEN;
                        }
                        TreeNodeModel fieldNodeModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
                            permission.getResource(), marking);
                        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
                        // fieldNode.setSelectable(Marking.GREEN.equals(marking));
                        // fieldNode.setSelected(Marking.GREEN.equals(marking));
                        if (permission.getResource() instanceof EntityObjectField) {
                            fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                        }
                    } else {
                        // mark actionNode if needed
                        Permission actionPermission = permissionFactory.create(entityAction, entityField);
                        Marking marking = Marking.NONE;
                        if (!contains(permissions, actionPermission) && contains(selectedPermissions, actionPermission)) {
                            marking = Marking.GREEN;
                        }
                        // actionNode.setSelectable(Marking.GREEN.equals(marking));
                        // actionNode.setSelected(Marking.GREEN.equals(marking));
                        ((TreeNodeModel) actionNode.getData()).setMarking(marking);
                        if (permission.getResource() instanceof EntityObjectField) {
                            ((TreeNodeModel) actionNode.getData()).setTooltip(Constants.CONTAINS_OBJECTS);
                        }
                    }
                }
                selectedUserPermissionNodes = ArrayUtils.addAll(selectedUserPermissionNodes, propagateNodePropertiesUpwards(actionNode));
            }
            selectedUserPermissionNodes = ArrayUtils.addAll(selectedUserPermissionNodes, propagateNodePropertiesUpwards(entityNode));
        }
    }

    private void createCurrentPermissionNode(DefaultTreeNode userNode, Set<Permission> selectedPermissions) {
        TreeNodeModel nodeModel = (TreeNodeModel) userNode.getData();
        List<Permission> permissions = null;
        Set<Permission> revokablePermissions = null;
        if (nodeModel.getTarget() instanceof User) {
            User user = (User) nodeModel.getTarget();
            permissions = permissionManager.getPermissions(user);
            revokablePermissions = getReplaceablePermissions(user, permissions, selectedPermissions);
        } else {
            if (nodeModel.getTarget() instanceof UserGroup) {
                UserGroup group = (UserGroup) nodeModel.getTarget();
                permissions = permissionManager.getPermissions(group);
                revokablePermissions = getReplaceablePermissions(group, permissions, selectedPermissions);
            }
        }
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), userNode);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission permission : permissionsByAction) {
                    if (!((EntityField) permission.getResource()).isEmptyField()) {
                        // decide marking
                        Marking marking = contains(revokablePermissions, permission) ? Marking.RED : Marking.NONE;
                        TreeNodeModel fieldNodeModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
                            permission.getResource(), marking);
                        if (permission.getResource() instanceof EntityObjectField) {
                            fieldNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                            if (Marking.NONE.equals(fieldNodeModel.getMarking())) {
                                fieldNodeModel.setMarking(Marking.BLUE);
                            }
                        }
                        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
                    } else {
                        // mark actionNode if needed
                        Permission actionPermission = permissionFactory.create(entityAction, entityField);
                        Marking marking = contains(revokablePermissions, actionPermission) ? Marking.RED : Marking.NONE;
                        TreeNodeModel actionNodeModel = (TreeNodeModel) actionNode.getData();
                        actionNodeModel.setMarking(marking);
                        if (permission.getResource() instanceof EntityObjectField) {
                            actionNodeModel.setTooltip(Constants.CONTAINS_OBJECTS);
                            if (Marking.NONE.equals(actionNodeModel.getMarking())) {
                                actionNodeModel.setMarking(Marking.BLUE);
                            }
                        }
                    }
                }
                propagateNodePropertiesUpwards(actionNode);
            }
            propagateNodePropertiesUpwards(entityNode);
        }
    }

    /**
     * wizard step 1
     */
    public void processSelectedPermissions() {
        selectedPermissions = getSelectedPermissions(selectedPermissionNodes, true);
        initUsers();
        initUserGroups();
    }

    private void initUsers() {
        List<User> allUsers = userService.findUsers(userSession.getSelectedCompany());
        // if (!revokeSelectedPermissions) {
        userList.clear();
        for (User user : allUsers) {
            userList.add(new UserModel(user, false));
        }
        // }
    }

    private void initUserGroups() {
        // init groups tree
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

    public void confirmUserPermissions() {
        for (UserModel userModel : userList) {
            if (userModel.isSelected()) {
                for (Permission permission : selectedPermissions) {
                    if (permissionDataAccess.isGrantable(userModel.getUser(), permission.getAction(), permission.getResource())) {
                        permissionService.grant(userSession.getUser(), userModel.getUser(), permission.getAction(), permission.getResource());
                    }
                }
            }
        }
        init();
    }

    public void confirmPropagatedUserPermissions() {
        for (TreeNode userNode : newUserRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, false);
            for (Permission permission : selectedPermissions) {
                if (permissionDataAccess.isGrantable(user, permission.getAction(), permission.getResource())) {
                    permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
                }
            }
        }
        init();
    }

    public void confirmGroupPermissions() {
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode node : selectedGroupNodes) {
            UserGroup userGroup = (UserGroup) node.getData();
            selectedGroups.add(userGroup);
            for (Permission permission : selectedPermissions) {
                if (permissionDataAccess.isGrantable((UserGroup) userGroup, permission.getAction(), permission.getResource())) {
                    permissionService.grant(userSession.getUser(), userGroup, permission.getAction(), permission.getResource());
                }
            }
        }
        currentUserRoot = new DefaultTreeNode();
        newUserRoot = new DefaultTreeNode();
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
        for (User user : sortedUsers) {
            createUserNode(user);
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
                createCurrentPermissionNode(currentUserNode, getSelectedPermissions(selectedUserPermissionNodes, false));
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
                createCurrentPermissionNode(currentUserNode, getSelectedPermissions(selectedUserPermissionNodes, false));
            }
        }
    }

    public TreeNode getResourceRoot() {
        return resourceRoot;
    }

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedPermissionNodes = selectedPermissionNodes;
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

}
