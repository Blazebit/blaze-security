/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

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
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.lang.StringUtils;
import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.ResourceNameExtension;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.service.api.UserGroupService;

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
        // buildResourceTree();
        initPermissions();
        selectedPermissionNodes = new TreeNode[] {};
        resourceRoot = getResourceTree(groupPermissions);

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
            groupNode = new DefaultTreeNode(new TreeNodeModel(group.getName(), ResourceType.USERGROUP, group), groupNode);
            groupNode.setExpanded(true);
            permissions = permissionManager.getPermissions(group);
            getPermissionTree(permissions, groupNode);
        }
        groupPermissions = permissions;
        ((TreeNodeModel) groupNode.getData()).setMarking(Marking.GREEN);

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
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes, true);
        List<Permission> currentPermissions = new ArrayList<Permission>(groupPermissions);
        for (Permission permission : currentPermissions) {
            if (!isGranted(ActionConstants.GRANT, permission.getResource())) {
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
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), newPermissionTreeRoot);
            entityNode.setExpanded(true);
            List<Permission> permissionsByEntity = mergedPermissionMap.get(entity);
            Map<Action, List<Permission>> resourceActionMapByAction = groupPermissionsByAction(permissionsByEntity);

            for (Action action : resourceActionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);

                List<Permission> permissionsByAction = resourceActionMapByAction.get(action);
                for (Permission permission : permissionsByAction) {
                    if (!((EntityField) permission.getResource()).isEmptyField()) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
                            permission.getResource(), contains(selectedPermissions, permission) ? Marking.GREEN : Marking.NONE), actionNode);
                    } else {
                        ((TreeNodeModel) actionNode.getData())
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
            Marking firstMarking = ((TreeNodeModel) node.getChildren().get(0).getData()).getMarking();
            for (TreeNode child : node.getChildren()) {
                TreeNodeModel childNodeData = (TreeNodeModel) child.getData();
                if (!childNodeData.getMarking().equals(firstMarking)) {
                    foundOneUnMarked = true;
                    break;
                }
            }
            if (!foundOneUnMarked) {
                ((TreeNodeModel) node.getData()).setMarking(firstMarking);
            }
        }
    }

    private void buildCurrentPermissionTree(List<Permission> currentPermissions, Set<Permission> selectedPermissions, Set<Permission> permissionsToRevoke) {
        currentPermissionTreeRoot = new DefaultTreeNode();
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(currentPermissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), currentPermissionTreeRoot);
            entityNode.setExpanded(true);

            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionGroup);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> resoucesByAction = permissionMapByAction.get(action);
                for (Permission _permissionByAction : resoucesByAction) {
                    AbstractPermission permissionByAction = (AbstractPermission) _permissionByAction;
                    if (!StringUtils.isEmpty(permissionByAction.getResource().getField())) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(permissionByAction.getResource().getField(), TreeNodeModel.ResourceType.FIELD,
                            permissionByAction.getResource(),
                            contains(permissionsToRevoke, permissionByAction) || !contains(selectedPermissions, permissionByAction) ? Marking.RED : Marking.NONE), actionNode);
                    } else {
                        // entity and action.
                        if (contains(permissionsToRevoke, permissionByAction) || !contains(selectedPermissions, permissionByAction)) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.RED);
                        }
                    }
                    boolean foundOneNotMarked = false;
                    for (TreeNode childNode : actionNode.getChildren()) {
                        if (!((TreeNodeModel) childNode.getData()).isMarked()) {
                            foundOneNotMarked = true;
                            break;
                        }
                        if (!foundOneNotMarked) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.RED);
                        }
                    }

                }
            }
            boolean foundOneNotMarked = false;
            for (TreeNode childNode : entityNode.getChildren()) {
                if (!((TreeNodeModel) childNode.getData()).isMarked()) {
                    foundOneNotMarked = true;
                    break;
                }
                if (!foundOneNotMarked) {
                    ((TreeNodeModel) entityNode.getData()).setMarking(Marking.RED);
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
            DefaultTreeNode permissionRoot = new DefaultTreeNode(new TreeNodeModel(user.getUsername(), ResourceType.USER, user), userPermissionTreeRoot);
            permissionRoot.setExpanded(true);
            List<Permission> userPermissions = filterPermissions(permissionManager.getPermissions(user)).get(0);
            buildPermissionViewTreeForUser(permissionRoot, userPermissions, grantedPermissions, revokedPermissions);
        }

    }

    private void buildPermissionViewTreeForUser(TreeNode root, List<Permission> permissions, List<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions, grantedPermissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), root);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                TreeNodeModel actionNodeModel = new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction);
                DefaultTreeNode actionNode = new DefaultTreeNode(actionNodeModel, entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    if (!permission.getResource().isEmptyField()) {
                        TreeNodeModel fieldNodeModel = new TreeNodeModel(permission.getResource().getField(), TreeNodeModel.ResourceType.FIELD, permission.getResource());
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
                selectedUserNodes = ArrayUtils.addAll(selectedUserNodes, propagateNodePropertiesUpwards(actionNode));
            }
            selectedUserNodes = ArrayUtils.addAll(selectedUserNodes, propagateNodePropertiesUpwards(entityNode));
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
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            if (ResourceType.USER.equals(userNodeModel.getType())) {
                for (TreeNode entityNode : userNode.getChildren()) {
                    TreeNodeModel entityNodeModel = (TreeNodeModel) entityNode.getData();
                    if (entityNode.isSelectable()) {
                        // process only action and field nodes!
                        for (TreeNode actionNode : entityNode.getChildren()) {
                            TreeNodeModel actionNodeModel = (TreeNodeModel) actionNode.getData();
                            if (actionNode.isSelected()) {
                                if (actionNode.getChildCount() == 0) {
                                    if (Marking.GREEN.equals(actionNodeModel.getMarking())) {
                                        // to grant entity-action permission
                                        permissionService.grant(userSession.getUser(), (User) userNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget(),
                                                                (EntityField) entityNodeModel.getTarget());
                                    }
                                } else {
                                    for (TreeNode fieldNode : actionNode.getChildren()) {
                                        TreeNodeModel fieldNodeModel = (TreeNodeModel) fieldNode.getData();
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
