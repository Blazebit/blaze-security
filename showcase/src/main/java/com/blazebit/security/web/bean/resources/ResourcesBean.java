/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.resources;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.lang.StringUtils;
import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionHandlingBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.ResourceNameExtension;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.bean.model.NodeModel.Marking;
import com.blazebit.security.web.bean.model.NodeModel.ResourceType;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "resourcesBean")
public class ResourcesBean extends PermissionHandlingBaseBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    private ResourceNameExtension resourceNameExtension;

    @Inject
    private UserGroupService userGroupService;

    @Inject
    private UserService userService;

    private DefaultTreeNode resourceRoot;
    private TreeNode[] selectedPermissionNodes;

    private List<UserModel> userList = new ArrayList<UserModel>();

    private TreeNode currentUserRoot;
    private TreeNode newUserRoot;
    private TreeNode currentGroupRoot;
    private TreeNode newGroupRoot;

    private Set<Permission> selectedPermissions;
    private DefaultTreeNode groupRoot;
    private TreeNode[] selectedGroupNodes;

    private Integer activeTabIndex = 0;

    public void init() {
        buildResourceTree();
    }

    private void buildResourceTree() {
        resourceRoot = new DefaultTreeNode("root", null);
        for (AnnotatedType<?> type : resourceNameExtension.getResourceNames()) {
            Class<?> entityClass = (Class<?>) type.getBaseType();
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entityClass);
            // check if logged in user can grant these resources
            if (permissionService.isGranted(userSession.getUser(), actionFactory.createAction(ActionConstants.GRANT), entityField)) {
                // entity
                DefaultTreeNode entityNode = new DefaultTreeNode("root", new NodeModel(entityField.getEntity(), NodeModel.ResourceType.ENTITY, entityField), resourceRoot);
                entityNode.setExpanded(true);
                List<Action> entityActionFields = actionFactory.getActionsForEntity();
                // fields for entity
                Field[] allFields = ReflectionUtils.getInstanceFields(entityClass);
                if (allFields.length > 0) {
                    // actions for fields
                    for (Action action : actionFactory.getActionsForField()) {
                        EntityAction entityAction = (EntityAction) action;
                        DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                        // actionNode.setExpanded(true);
                        // fields for entity
                        for (Field field : allFields) {
                            EntityField entityFieldWithField = (EntityField) entityFieldFactory.createResource(entityClass, field.getName());
                            DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(field.getName(), NodeModel.ResourceType.FIELD, entityFieldWithField), actionNode);
                            fieldNode.setExpanded(true);
                        }
                        markParentAsSelectedIfChildrenAreSelected(actionNode);
                        entityActionFields.remove(action);
                    }
                }
                // remaining action fields for entity
                for (Action action : entityActionFields) {
                    EntityAction entityAction = (EntityAction) action;
                    DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                }
                // fix selections -> propagate "checked" to entity if every child checked
                markParentAsSelectedIfChildrenAreSelected(entityNode);
            }
        }
    }

    /**
     * helper
     * 
     * @param node
     */
    private void markParentAsSelectedIfChildrenAreSelected(DefaultTreeNode node) {
        if (node.getChildCount() > 0) {
            boolean foundOneUnselected = false;
            for (TreeNode entityChild : node.getChildren()) {
                if (!entityChild.isSelected()) {
                    foundOneUnselected = true;
                    break;
                }
            }
            if (!foundOneUnselected) {
                node.setSelected(true);
            }
        }
    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            processSelectedPermissions();
        } else {
            if (event.getOldStep().equals("permissions")) {
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
        NodeModel groupNodeModel = new NodeModel(userGroup.getName(), ResourceType.USERGROUP, userGroup);
        currentGroupRoot = new DefaultTreeNode();
        DefaultTreeNode currentGroupNode = new DefaultTreeNode(groupNodeModel, currentGroupRoot);
        currentGroupNode.setExpanded(true);
        newGroupRoot = new DefaultTreeNode();
        DefaultTreeNode newGroupNode = new DefaultTreeNode(groupNodeModel, newGroupRoot);
        newGroupNode.setExpanded(true);
        createCurrentPermissionNode(currentGroupNode);
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
        NodeModel userNodeModel = new NodeModel(user.getUsername(), ResourceType.USER, user);
        DefaultTreeNode currentUserNode = new DefaultTreeNode(userNodeModel, currentUserRoot);
        currentUserNode.setExpanded(true);
        DefaultTreeNode newUserNode = new DefaultTreeNode(userNodeModel, newUserRoot);
        newUserNode.setExpanded(true);
        createCurrentPermissionNode(currentUserNode);
        createNewPermissionNode(newUserNode);
    }

    private void createNewPermissionNode(DefaultTreeNode userNode) {
        NodeModel nodeModel = (NodeModel) userNode.getData();
        List<Permission> permissions = null;
        Set<Permission> revokablePermissions = null;
        if (nodeModel.getTarget() instanceof User) {
            User user = (User) nodeModel.getTarget();
            permissions = permissionManager.getAllPermissions(user);
            revokablePermissions = getReplaceablePermissions(user, permissions, selectedPermissions);
        } else {
            if (nodeModel.getTarget() instanceof UserGroup) {
                UserGroup group = (UserGroup) nodeModel.getTarget();
                permissions = permissionManager.getAllPermissions(group);
                revokablePermissions = getReplaceablePermissions(group, permissions, selectedPermissions);
            }
        }
        permissions = removeAll(permissions, revokablePermissions);
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions, selectedPermissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), userNode);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    if (!permission.getResource().isEmptyField()) {
                        // decide marking
                        Marking marking = Marking.NONE;
                        if (!contains(permissions, permission) && contains(selectedPermissions, permission)) {
                            marking = Marking.GREEN;
                        }
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD, permission.getResource(),
                            marking), actionNode);
                    } else {
                        // mark actionNode if needed
                        Permission actionPermission = permissionFactory.create(entityAction, entityField);
                        Marking marking = Marking.NONE;
                        if (!contains(permissions, actionPermission) && contains(selectedPermissions, actionPermission)) {
                            marking = Marking.GREEN;
                        }
                        ((NodeModel) actionNode.getData()).setMarking(marking);

                    }
                }
                markAndSelectParents(actionNode, null);
            }
            markAndSelectParents(entityNode, null);
        }

    }

    private void createCurrentPermissionNode(DefaultTreeNode userNode) {
        NodeModel nodeModel = (NodeModel) userNode.getData();
        List<Permission> permissions = null;
        Set<Permission> revokablePermissions = null;
        if (nodeModel.getTarget() instanceof User) {
            User user = (User) nodeModel.getTarget();
            permissions = permissionManager.getAllPermissions(user);
            revokablePermissions = getReplaceablePermissions(user, permissions, selectedPermissions);
        } else {
            if (nodeModel.getTarget() instanceof UserGroup) {
                UserGroup group = (UserGroup) nodeModel.getTarget();
                permissions = permissionManager.getAllPermissions(group);
                revokablePermissions = getReplaceablePermissions(group, permissions, selectedPermissions);
            }
        }
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), userNode);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    if (!permission.getResource().isEmptyField()) {
                        // decide marking
                        Marking marking = contains(revokablePermissions, permission) ? Marking.RED : Marking.NONE;
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD, permission.getResource(),
                            marking), actionNode);
                    } else {
                        // mark actionNode if needed
                        Permission actionPermission = permissionFactory.create(entityAction, entityField);
                        Marking marking = contains(revokablePermissions, actionPermission) ? Marking.RED : Marking.NONE;
                        ((NodeModel) actionNode.getData()).setMarking(marking);

                    }
                }
                markAndSelectParents(actionNode, null);
            }
            markAndSelectParents(entityNode, null);
        }
    }

    /**
     * wizard step 1
     */
    public void processSelectedPermissions() {
        selectedPermissions = processSelectedPermissions(selectedPermissionNodes, true);
        initUsers();
        initUserGroups();
    }

    private void initUsers() {
        List<User> allUsers = userService.findUsers();
        userList.clear();
        for (User user : allUsers) {
            userList.add(new UserModel(user, false));
        }
    }

    private void initUserGroups() {
        // init groups tree
        List<UserGroup> availableGroups = userGroupService.getAllParentGroups();
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
        for (TreeNode groupNode : selectedGroupNodes) {
            UserGroup group = (UserGroup) groupNode.getData();
            for (User user : userGroupService.getUsersFor(group)) {
                for (Permission permission : selectedPermissions) {
                    if (permissionDataAccess.isGrantable(user, permission.getAction(), permission.getResource())) {
                        permissionService.grant(userSession.getUser(), user, permission.getAction(), permission.getResource());
                    }
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
        for (UserGroup group : selectedGroups) {
            for (User user : userGroupService.getUsersFor(group)) {
                createUserNode(user);
            }
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

    public DefaultTreeNode getResourceRoot() {
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

}
