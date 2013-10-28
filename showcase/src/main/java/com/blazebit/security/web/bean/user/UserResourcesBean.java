/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.lang.StringUtils;
import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.bean.model.NodeModel.Marking;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "userResourcesBean")
public class UserResourcesBean extends ResourceHandlingBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private TreeNode[] selectedPermissionNodes = new TreeNode[] {};
    private DefaultTreeNode newPermissionTreeRoot;
    private DefaultTreeNode currentPermissionTreeRoot;

    private Set<EntityField> selectedResources = new HashSet<EntityField>();
    private List<Permission> userPermissions = new ArrayList<Permission>();
    private Set<Permission> selectedPermissions;
    private DefaultTreeNode resourceRoot;

    // permissionview
    private TreeNode permissionViewRoot;

    private List<Permission> currentPermissionsToConfirm;
    private Set<Permission> revokedPermissionsToConfirm;

    private List<Permission> userDataPermissions = new ArrayList<Permission>();

    public void init() {
        initPermissions();
        selectedPermissionNodes = new TreeNode[] {};
        resourceRoot = getResourceTree(userPermissions, selectedPermissionNodes);
    }

    private void initPermissions() {
        userPermissions = permissionManager.getPermissions(getSelectedUser());
        userDataPermissions = permissionManager.getDataPermissions(getSelectedUser());
        this.permissionViewRoot = new DefaultTreeNode("root", null);
        getPermissionTree(permissionManager.getAllPermissions(getSelectedUser()), permissionViewRoot);

    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            processSelectedPermissions();
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    public void processSelectedPermissions() {
        // get selected permissions
        selectedPermissions = processSelectedPermissions(selectedPermissionNodes, true);
        // add permissions of ther user that did not appear in the list because the logged in user does not have permission to
        // grant or revoke them
        for (Permission permission : permissionManager.getPermissions(userSession.getSelectedUser())) {
            if (!isAuthorized(ActionConstants.GRANT, permission.getResource())) {
                selectedPermissions.add(permission);
            }
        }
        List<Permission> currentPermissions = new ArrayList<Permission>(userPermissions);
        Set<Permission> toRevoke = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            Set<Permission> removeWhenGranting = permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), permission.getAction(), permission.getResource());
            for (Permission toBeRevoked : removeWhenGranting) {
                //check logged in user can revoke it or not
                if (isAuthorized(ActionConstants.REVOKE, toBeRevoked.getResource())) {
                    toRevoke.add(toBeRevoked);
                }
            }
        }
        revokedPermissionsToConfirm = new HashSet<Permission>();
        for (Permission currentPermission : currentPermissions) {
            if (!contains(selectedPermissions, currentPermission)) {
                if (isAuthorized(ActionConstants.GRANT, currentPermission.getResource())) {
                    revokedPermissionsToConfirm.add(currentPermission);
                }
            }
        }
        toRevoke.addAll(revokedPermissionsToConfirm);
        List<Permission> currentUserPermissions = new ArrayList<Permission>(currentPermissions);
        currentUserPermissions = (List<Permission>) removeAll(currentUserPermissions, toRevoke);
        List<Permission> current = new ArrayList<Permission>(currentPermissions);
        current.addAll(userDataPermissions);
        currentPermissionTreeRoot = buildCurrentPermissionTree(current, selectedPermissions, toRevoke);
        // new permission tree
        Set<Permission> granted = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            if (!contains(currentUserPermissions, permission)) {
                if (permissionDataAccess.isGrantable(currentUserPermissions, getSelectedUser(), permission.getAction(), permission.getResource())) {
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
                actionNode.setExpanded(true);
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

    /**
     * permission tree with current permissions, revokable permissions marked as red
     * 
     * @param currentPermissions
     * @param selectedPermissions
     * @param permissionsToRevoke
     * @return
     */
    private DefaultTreeNode buildCurrentPermissionTree(List<Permission> currentPermissions, Set<Permission> selectedPermissions, Set<Permission> permissionsToRevoke) {
        DefaultTreeNode currentPermissionTreeRoot = new DefaultTreeNode();
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
                for (Permission permission : resoucesByAction) {
                    if (!StringUtils.isEmpty(((EntityField) permission.getResource()).getField())) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(((EntityField) permission.getResource()).getField(), NodeModel.ResourceType.FIELD,
                            permission.getResource(), contains(permissionsToRevoke, permission)/*
                                                                                                * ||
                                                                                                * !contains(selectedPermissions,
                                                                                                * permission)
                                                                                                */? Marking.RED : Marking.NONE), actionNode);
                        if (permission.getResource() instanceof EntityObjectField) {
                            ((NodeModel) fieldNode.getData()).setTooltip("Contains permissions for specific entity objects");
                            ((NodeModel) fieldNode.getData()).setMarking(Marking.BLUE);
                        }
                    } else {
                        // entity and action.
                        Permission entityPermission = permissionFactory.create(entityAction, entityField);
                        if (contains(permissionsToRevoke, entityPermission)/*
                                                                            * || !contains(selectedPermissions,
                                                                            * entityPermission)
                                                                            */) {
                            ((NodeModel) actionNode.getData()).setMarking(Marking.RED);
                        } else {
                            if (permission.getResource() instanceof EntityObjectField) {
                                ((NodeModel) actionNode.getData()).setTooltip("Contains permissions for specific entity objects");
                                ((NodeModel) actionNode.getData()).setMarking(Marking.BLUE);
                            }
                        }
                    }
                    propagateSelectionAndMarkingUp(actionNode, null);
                }
            }
            propagateSelectionAndMarkingUp(entityNode, null);
        }
        return currentPermissionTreeRoot;
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirm() {
        for (Permission permission : revokedPermissionsToConfirm) {
            permissionService.revoke(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        }

        for (Permission permission : currentPermissionsToConfirm) {
            if (!contains(userPermissions, permission) && !contains(userDataPermissions, permission)) {
                permissionService.grant(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
            }
        }
        init();
    }

    public DefaultTreeNode getResourceRoot() {
        return resourceRoot;
    }

    public Set<EntityField> getSelectedResources() {
        return selectedResources;
    }

    public void setSelectedResources(Set<EntityField> selectedResources) {
        this.selectedResources = selectedResources;
    }

    public User getSelectedUser() {
        return userSession.getSelectedUser();
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
