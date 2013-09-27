/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.user;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
import com.blazebit.security.web.bean.PermissionHandlingBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.ResourceNameExtension;
import com.blazebit.security.web.bean.model.NodeModel;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "userResourcesBean")
public class UserResourcesBean extends PermissionHandlingBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    ResourceNameExtension resourceNameExtension;

    private TreeNode[] selectedPermissionNodes;
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

    public void init() {
        buildResourceTree();
        initPermissions();
    }

    private void initPermissions() {
        userPermissions = permissionManager.getAllPermissions(getSelectedUser());

        this.permissionViewRoot = new DefaultTreeNode("root", null);
        buildPermissionViewTree(userPermissions, permissionViewRoot);

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
                            if (permissionDataAccess.findPermission(getSelectedUser(), entityAction, entityFieldWithField) != null
                                || permissionDataAccess.findPermission(getSelectedUser(), entityAction, entityField) != null) {
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
                    if (permissionDataAccess.findPermission(getSelectedUser(), entityAction, entityField) != null) {
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
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    public void processSelectedPermissions() {
        selectedPermissions = processSelectedPermissions(selectedPermissionNodes);
        List<Permission> currentPermissions = new ArrayList<Permission>(userPermissions);
        Set<Permission> toRevoke = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            toRevoke.addAll(permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), permission.getAction(), permission.getResource()));
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
                if (permissionDataAccess.isGrantable(getSelectedUser(), permission.getAction(), permission.getResource())) {
                    granted.add(permission);
                }
            }
        }
        // filter out redundant permissions
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        for (Permission permission : granted) {
            EntityField entityField = (EntityField) permission.getResource();
            if (!entityField.isEmptyField()) {
                if (contains(granted, permissionFactory.create(getSelectedUser(), permission.getAction(), entityFieldFactory.createResource(entityField.getEntity())))) {
                    redundantPermissions.add(permission);
                }
            }
        }
        granted.removeAll(redundantPermissions);
        currentPermissionsToConfirm = new ArrayList<Permission>();
        currentPermissionsToConfirm = removeAll(currentPermissions, revokedPermissionsToConfirm);
        currentPermissionsToConfirm.addAll(granted);
        buildNewPermissionTree(currentPermissionsToConfirm, granted);
    }

    private void buildNewPermissionTree(List<Permission> currentPermissions, Set<Permission> selectedPermissions) {
        newPermissionTreeRoot = new DefaultTreeNode();
        // current permissions
        Map<String, List<Permission>> currentPermissionMap = groupPermissionsByEntity(currentPermissions);
        // selected permissions (existing + new - revoked)
        Map<String, List<Permission>> mergedPermissionMap = groupPermissionsByEntity(new ArrayList<Permission>(selectedPermissions));
        // merge into resource actions map
        for (String entity : currentPermissionMap.keySet()) {
            List<Permission> permissionGroup = new ArrayList<Permission>(currentPermissionMap.get(entity));
            for (Permission _permission : permissionGroup) {
                AbstractPermission permission = (AbstractPermission) _permission;
                List<Permission> temp = new ArrayList<Permission>();
                if (mergedPermissionMap.containsKey(permission.getResource().getEntity())) {
                    temp = mergedPermissionMap.get(permission.getResource().getEntity());
                } else {
                    temp = new ArrayList<Permission>();
                }
                if (permission.getResource().isEmptyField()) {
                    if (!temp.contains(permissionFactory.create(getSelectedUser(), permission.getAction(), permission.getResource()))) {
                        temp.add(permissionFactory.create(getSelectedUser(), permission.getAction(), permission.getResource()));
                        mergedPermissionMap.put(permission.getResource().getEntity(), temp);
                    }
                } else {
                    // check if entity is there
                    if (!temp.contains(permissionFactory.create(getSelectedUser(), permission.getAction(),
                                                                (EntityField) entityFieldFactory.createResource(permission.getResource().getEntity())))) {
                        temp.add(permissionFactory.create(getSelectedUser(), permission.getAction(), permission.getResource()));
                        mergedPermissionMap.put(permission.getResource().getEntity(), temp);
                    }
                }
            }
        }
        // go through resource actions and build tree + mark new ones
        for (String entity : mergedPermissionMap.keySet()) {
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), newPermissionTreeRoot);
            entityNode.setExpanded(true);
            List<Permission> permissionsByEntity = mergedPermissionMap.get(entity);
            Map<Action, List<Permission>> resourceActionMapByAction = groupPermissionsByAction(permissionsByEntity);

            for (Action action : resourceActionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction, false), entityNode);

                List<Permission> permissionsByAction = resourceActionMapByAction.get(action);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    if (!permission.getResource().isEmptyField()) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD, permission.getResource(),
                            contains(selectedPermissions, permission)), actionNode);
                    } else {
                        ((NodeModel) actionNode.getData()).setMarked(contains(selectedPermissions, permissionFactory.create(getSelectedUser(), entityAction, entityField)));
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
            for (TreeNode child : node.getChildren()) {
                if (!((NodeModel) child.getData()).isMarked()) {
                    foundOneUnMarked = true;
                    break;
                }
            }
            ((NodeModel) node.getData()).setMarked(!foundOneUnMarked);
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
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction, false), entityNode);
                actionNode.setExpanded(true);
                List<Permission> resoucesByAction = permissionMapByAction.get(action);
                for (Permission _permissionByAction : resoucesByAction) {
                    AbstractPermission permissionByAction = (AbstractPermission) _permissionByAction;
                    if (!StringUtils.isEmpty(permissionByAction.getResource().getField())) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permissionByAction.getResource().getField(), NodeModel.ResourceType.FIELD,
                            permissionByAction.getResource(), contains(permissionsToRevoke, permissionByAction) || !contains(selectedPermissions, permissionByAction)), actionNode);
                    } else {
                        // entity and action.
                        if (contains(permissionsToRevoke, permissionByAction) || !contains(selectedPermissions, permissionByAction)) {
                            ((NodeModel) actionNode.getData()).setMarked(true);
                        }
                    }
                    boolean foundOneNotMarked = false;
                    for (TreeNode childNode : actionNode.getChildren()) {
                        if (!((NodeModel) childNode.getData()).isMarked()) {
                            foundOneNotMarked = true;
                            break;
                        }
                        if (!foundOneNotMarked) {
                            ((NodeModel) actionNode.getData()).setMarked(true);
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
                    ((NodeModel) entityNode.getData()).setMarked(true);
                }
            }
        }
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirm() {
        for (Permission permission : currentPermissionsToConfirm) {
            if (!contains(userPermissions, permission)) {
                permissionService.grant(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
            }
        }
        for (Permission permission : revokedPermissionsToConfirm) {
            permissionService.revoke(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
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
