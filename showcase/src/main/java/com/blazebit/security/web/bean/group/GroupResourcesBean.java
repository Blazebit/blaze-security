/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

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
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.ResourceNameExtension;
import com.blazebit.security.web.bean.SecurityBaseBean;
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
@ManagedBean(name = "groupResourcesBean")
public class GroupResourcesBean extends ResourceHandlingBaseBean implements PermissionView, Serializable {

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

    private List<Permission> groupPermissions = new ArrayList<Permission>();

    private TreeNode[] selectedPermissionNodes;
    private DefaultTreeNode newPermissionTreeRoot;
    private DefaultTreeNode currentPermissionTreeRoot;

    private DefaultTreeNode userPermissionTreeRoot;
    private TreeNode[] selectedUserNodes;

    private DefaultTreeNode resourceRoot;

    // permissionview
    private TreeNode permissionViewRoot;

    private List<Permission> currentPermissionsToConfirm;
    private Set<Permission> revokedPermissionsToConfirm;

    public void init() {
        //buildResourceTree();
        initPermissions();
        selectedPermissionNodes = new TreeNode[] {};
        resourceRoot=getResourceTree(groupPermissions, selectedPermissionNodes);
        
    }

    private void initPermissions() {
        List<UserGroup> parents = new ArrayList<UserGroup>();
        UserGroup parent = getSelectedUserGroup().getParent();
        parents.add(getSelectedUserGroup());
        while (parent != null) {
            parents.add(0, parent);
            parent = parent.getParent();
        }
        this.permissionViewRoot = new DefaultTreeNode("root", null);
        TreeNode groupNode = permissionViewRoot;
        List<Permission> permissions = null;
        for (UserGroup group : parents) {
            groupNode = new DefaultTreeNode(new NodeModel(group.getName(), ResourceType.USERGROUP, group), groupNode);
            groupNode.setExpanded(true);
            permissions = permissionManager.getAllPermissions(group);
            getPermissionTree(permissions, groupNode);
        }
        groupPermissions = permissions;
        ((NodeModel) groupNode.getData()).setMarking(Marking.GREEN);

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
                            // check if user has exact permission for this field or the whole entity
                            if (permissionDataAccess.findPermission(getSelectedUserGroup(), entityAction, entityFieldWithField) != null
                                || permissionDataAccess.findPermission(getSelectedUserGroup(), entityAction, entityField) != null) {
                                fieldNode.setSelected(true);
                            }
                        }
                        markParentAsSelectedIfChildrenAreSelected(actionNode);
                        entityActionFields.remove(action);
                    }
                }
                // remaining action fields for entity
                for (Action action : entityActionFields) {
                    EntityAction entityAction = (EntityAction) action;
                    DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                    if (permissionDataAccess.findPermission(getSelectedUserGroup(), entityAction, entityField) != null) {
                        actionNode.setSelected(true);
                    }
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
                confirmPermissions();
            }
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    public void processSelectedPermissions() {
        Set<Permission> selectedPermissions = processSelectedPermissions(selectedPermissionNodes, true);
        List<Permission> currentPermissions = new ArrayList<Permission>(groupPermissions);
        for (Permission permission : currentPermissions) {
            if (!isAuthorized(ActionConstants.GRANT, permission.getResource())) {
                selectedPermissions.add(permission);
            }
        }
        Set<Permission> toRevoke = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            toRevoke.addAll(permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUserGroup(), permission.getAction(), permission.getResource()));
        }
        revokedPermissionsToConfirm = new HashSet<Permission>();
        for (Permission permission : currentPermissions) {
            if (!contains(selectedPermissions, permission)) {
                revokedPermissionsToConfirm.add(permission);
            }
        }
        toRevoke.addAll(revokedPermissionsToConfirm);
        buildCurrentPermissionTree(currentPermissions, selectedPermissions, toRevoke);
        // new permission tree
        Set<Permission> granted = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            if (!contains(currentPermissions, permission)) {
                if (permissionDataAccess.isGrantable(getSelectedUserGroup(), permission.getAction(), permission.getResource())) {
                    granted.add(permission);
                }
            }
        }
        // filter out redundant permissions
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        for (Permission permission : granted) {
            EntityField entityField = (EntityField) permission.getResource();
            if (!entityField.isEmptyField()) {
                if (contains(granted, permissionFactory.create(permission.getAction(), entityFieldFactory.createResource(entityField.getEntity())))) {
                    redundantPermissions.add(permission);
                }
            }
        }
        granted.removeAll(redundantPermissions);
        currentPermissionsToConfirm = new ArrayList<Permission>();
        currentPermissionsToConfirm = (List<Permission>) removeAll(currentPermissions, revokedPermissionsToConfirm);
        currentPermissionsToConfirm.addAll(granted);
        buildNewPermissionTree(currentPermissionsToConfirm, granted);
    }

    private void buildNewPermissionTree(List<Permission> currentPermissions, Set<Permission> selectedPermissions) {
        newPermissionTreeRoot = new DefaultTreeNode();
        // current permissions
        Map<String, List<Permission>> mergedPermissionMap = groupPermissionsByEntity(currentPermissions, selectedPermissions);
        // go through resource actions and build tree + mark new ones
        for (String entity : mergedPermissionMap.keySet()) {
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), newPermissionTreeRoot);
            entityNode.setExpanded(true);
            List<Permission> permissionsByEntity = mergedPermissionMap.get(entity);
            Map<Action, List<Permission>> resourceActionMapByAction = groupPermissionsByAction(permissionsByEntity);

            for (Action action : resourceActionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);

                List<Permission> permissionsByAction = resourceActionMapByAction.get(action);
                for (Permission permission : permissionsByAction) {
                    if (!((EntityField) permission.getResource()).isEmptyField()) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(((EntityField) permission.getResource()).getField(), NodeModel.ResourceType.FIELD,
                            permission.getResource(), contains(selectedPermissions, permission) ? Marking.GREEN : Marking.NONE), actionNode);
                    } else {
                        ((NodeModel) actionNode.getData())
                            .setMarking(contains(selectedPermissions, permissionFactory.create(entityAction, entityField)) ? Marking.GREEN : Marking.NONE);
                    }
                }

                markParentIfChildrenAreMarked(actionNode);
            }

            markParentIfChildrenAreMarked(entityNode);
        }
    }

    /**
     * helper to mark parent when children model is marked
     * 
     * @param node
     */
    private void markParentIfChildrenAreMarked(TreeNode node) {
        if (node.getChildCount() > 0) {
            boolean foundOneUnMarked = false;
            Marking firstMarking = ((NodeModel) node.getChildren().get(0).getData()).getMarking();
            for (TreeNode child : node.getChildren()) {
                NodeModel childNodeData = (NodeModel) child.getData();
                if (!childNodeData.getMarking().equals(firstMarking)) {
                    foundOneUnMarked = true;
                    break;
                }
            }
            if (!foundOneUnMarked) {
                ((NodeModel) node.getData()).setMarking(firstMarking);
            }
        }
    }

    private void buildCurrentPermissionTree(List<Permission> currentPermissions, Set<Permission> selectedPermissions, Set<Permission> permissionsToRevoke) {
        currentPermissionTreeRoot = new DefaultTreeNode();
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(currentPermissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), currentPermissionTreeRoot);
            entityNode.setExpanded(true);

            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionGroup);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> resoucesByAction = permissionMapByAction.get(action);
                for (Permission _permissionByAction : resoucesByAction) {
                    AbstractPermission permissionByAction = (AbstractPermission) _permissionByAction;
                    if (!StringUtils.isEmpty(permissionByAction.getResource().getField())) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permissionByAction.getResource().getField(), NodeModel.ResourceType.FIELD,
                            permissionByAction.getResource(),
                            contains(permissionsToRevoke, permissionByAction) || !contains(selectedPermissions, permissionByAction) ? Marking.RED : Marking.NONE), actionNode);
                    } else {
                        // entity and action.
                        if (contains(permissionsToRevoke, permissionByAction) || !contains(selectedPermissions, permissionByAction)) {
                            ((NodeModel) actionNode.getData()).setMarking(Marking.RED);
                        }
                    }
                    boolean foundOneNotMarked = false;
                    for (TreeNode childNode : actionNode.getChildren()) {
                        if (!((NodeModel) childNode.getData()).isMarked()) {
                            foundOneNotMarked = true;
                            break;
                        }
                        if (!foundOneNotMarked) {
                            ((NodeModel) actionNode.getData()).setMarking(Marking.RED);
                        }
                    }

                }
            }
            boolean foundOneNotMarked = false;
            for (TreeNode childNode : entityNode.getChildren()) {
                if (!((NodeModel) childNode.getData()).isMarked()) {
                    foundOneNotMarked = true;
                    break;
                }
                if (!foundOneNotMarked) {
                    ((NodeModel) entityNode.getData()).setMarking(Marking.RED);
                }
            }
        }
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmPermissions() {
        currentPermissionsToConfirm = (List<Permission>) removeAll(currentPermissionsToConfirm, groupPermissions);
        for (Permission permission : currentPermissionsToConfirm) {
            permissionService.grant(userSession.getUser(), getSelectedUserGroup(), permission.getAction(), permission.getResource());
        }
        for (Permission permission : revokedPermissionsToConfirm) {
            permissionService.revoke(userSession.getUser(), getSelectedUserGroup(), permission.getAction(), permission.getResource());
        }

        Set<User> users = new HashSet<User>(userGroupService.getUsersFor(getSelectedUserGroup()));
        for (UserGroup userGroup : userGroupService.getGroupsForGroup(getSelectedUserGroup())) {
            mergeUserList(userGroup, users);
        }
        buildUserPermissionTree(users, currentPermissionsToConfirm, revokedPermissionsToConfirm);
    }

    private void buildUserPermissionTree(Set<User> users, List<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        userPermissionTreeRoot = new DefaultTreeNode();
        selectedUserNodes = new TreeNode[] {};
        List<User> sortedUsers = new ArrayList<User>(users);
        Collections.sort(sortedUsers, new Comparator<User>() {

            @Override
            public int compare(User o1, User o2) {
                return o1.getUsername().compareToIgnoreCase(o2.getUsername());
            }

        });
        for (User user : sortedUsers) {
            DefaultTreeNode permissionRoot = new DefaultTreeNode(new NodeModel(user.getUsername(), ResourceType.USER, user), userPermissionTreeRoot);
            permissionRoot.setExpanded(true);
            List<Permission> userPermissions = permissionManager.getPermissions(user);
            buildPermissionViewTreeForUser(permissionRoot, userPermissions, grantedPermissions, revokedPermissions);
        }

    }

    private void buildPermissionViewTreeForUser(TreeNode root, List<Permission> permissions, List<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions, grantedPermissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), root);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                NodeModel actionNodeModel = new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction);
                DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    if (!permission.getResource().isEmptyField()) {
                        NodeModel fieldNodeModel = new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD, permission.getResource());
                        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
                        // entity-action-field node
                        Marking marking = Marking.NONE;
                        if (contains(grantedPermissions, permission)) {
                            marking = Marking.GREEN;
                            fieldNode.setSelected(true);
                        } else {
                            if (implies(revokedPermissions, permission)) {
                                marking = Marking.RED;
                                fieldNode.setSelected(false);
                            } else {
                                fieldNode.setSelectable(false);
                            }
                        }
                        fieldNodeModel.setMarking(marking);
                    } else {
                        // entity-action node
                        Permission actionPermission = permissionFactory.create(entityAction, entityField);
                        Marking marking = Marking.NONE;
                        if (contains(grantedPermissions, actionPermission)) {
                            marking = Marking.GREEN;
                            actionNode.setSelected(true);
                        } else {
                            if (implies(revokedPermissions, actionPermission)) {
                                marking = Marking.RED;
                                actionNode.setSelected(false);
                            } else {
                                actionNode.setSelectable(false);
                            }
                        }
                        actionNodeModel.setMarking(marking);
                    }
                }
                propagateSelectionAndMarkingUp(actionNode, selectedUserNodes);
            }
            propagateSelectionAndMarkingUp(entityNode, selectedUserNodes);
        }

    }

    private void mergeUserList(UserGroup userGroup, Set<User> users) {
        users.addAll(userGroupService.getUsersFor(userGroup));
        for (UserGroup childGroup : userGroupService.getGroupsForGroup(userGroup)) {
            mergeUserList(childGroup, users);
        }
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmUsers() {
        for (TreeNode userNode : userPermissionTreeRoot.getChildren()) {
            NodeModel userNodeModel = (NodeModel) userNode.getData();
            if (ResourceType.USER.equals(userNodeModel.getType())) {
                for (TreeNode entityNode : userNode.getChildren()) {
                    NodeModel entityNodeModel = (NodeModel) entityNode.getData();
                    if (entityNode.isSelectable()) {
                        // process only action and field nodes!
                        for (TreeNode actionNode : entityNode.getChildren()) {
                            NodeModel actionNodeModel = (NodeModel) actionNode.getData();
                            if (actionNode.isSelected()) {
                                if (actionNode.getChildCount() == 0) {
                                    if (Marking.GREEN.equals(actionNodeModel.getMarking())) {
                                        // to grant entity-action permission
                                        permissionService.grant(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                (EntityField) entityNodeModel.getTarget());
                                    }
                                } else {
                                    for (TreeNode fieldNode : actionNode.getChildren()) {
                                        NodeModel fieldNodeModel = (NodeModel) fieldNode.getData();
                                        if (fieldNode.isSelected()) {
                                            // selected+green=grant field+action
                                            if (Marking.GREEN.equals(fieldNodeModel.getMarking())) {
                                                permissionService.grant(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                        (EntityField) fieldNodeModel.getTarget());
                                            }
                                        } else {
                                            // not selected+red=revoke field+action
                                            if (Marking.RED.equals(fieldNodeModel.getMarking())) {
                                                permissionService.revoke(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                         (EntityField) fieldNodeModel.getTarget());
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (Marking.RED.equals(actionNodeModel.getMarking())) {
                                    // to revoke entity-action permission
                                    permissionService.revoke(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                             (EntityField) entityNodeModel.getTarget());
                                }
                            }
                        }

                    }
                }

            }
        }
        init();
    }

    public DefaultTreeNode getResourceRoot() {
        return resourceRoot;
    }

    public DefaultTreeNode getUserRoot() {
        return userPermissionTreeRoot;
    }

    public UserGroup getSelectedUserGroup() {
        return userSession.getSelectedUserGroup();
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionViewRoot;
    }

    public DefaultTreeNode getNewPermissionTreeRoot() {
        return newPermissionTreeRoot;
    }

    public void setNewPermissionTreeRoot(DefaultTreeNode newPermissionTreeRoot) {
        this.newPermissionTreeRoot = newPermissionTreeRoot;
    }

    public DefaultTreeNode getCurrentPermissionTreeRoot() {
        return currentPermissionTreeRoot;
    }

    public void setCurrentPermissionTreeRoot(DefaultTreeNode currentPermissionTreeRoot) {
        this.currentPermissionTreeRoot = currentPermissionTreeRoot;
    }

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedPermissionNodes = selectedPermissionNodes;
    }

}
