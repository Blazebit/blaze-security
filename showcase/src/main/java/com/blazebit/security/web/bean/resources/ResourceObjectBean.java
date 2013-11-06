package com.blazebit.security.web.bean.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.web.bean.PermissionHandlingBaseBean;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.util.Constants;

@Named
@ViewScoped
@Stateless
public class ResourceObjectBean extends PermissionHandlingBaseBean {

    @Inject
    private PermissionDataAccess permissionDataAccess;

    @Inject
    private PermissionManager permissionManager;

    private IdHolder selectedSubject;
    private List<RowModel> selectedObjects = new ArrayList<RowModel>();
    private List<String> selectedFields = new ArrayList<String>();
    private List<EntityAction> selectedActions = new ArrayList<EntityAction>();

    private TreeNode root;
    private TreeNode currentRoot;
    private TreeNode newRoot;

    private TreeNode[] selectedNodes = new TreeNode[] {};
    private HashSet<Permission> initialPermissions;

    private Set<Permission> grantedPermissions;
    private Set<Permission> revokedPermissions;
    private Set<Permission> notRevokable = new HashSet<Permission>();
    private Set<Permission> notGrantable = new HashSet<Permission>();

    public void init() {
        root = new DefaultTreeNode();
        initialPermissions = new HashSet<Permission>();
        for (RowModel selectedObject : selectedObjects) {
            EntityObjectField entityObjectResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), selectedObject
                .getEntity()
                .getId());
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(selectedObject.getFieldSummary(), ResourceType.ENTITY, entityObjectResource), root);
            entityNode.setExpanded(true);
            for (EntityAction action : selectedActions) {
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(action.getActionName(), ResourceType.ACTION, action), entityNode);
                actionNode.setExpanded(true);
                // fields
                if (!selectedFields.isEmpty()) {
                    for (String field : selectedFields) {
                        EntityObjectField entityObjectFieldResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), field,
                                                                                                                            selectedObject.getEntity().getId());
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(field, ResourceType.FIELD, entityObjectFieldResource), actionNode);

                        Permission permission = permissionFactory.create(action, entityObjectFieldResource);
                        initialPermissions.add(permission);
                        if (contains(getCurrentPermissions(), permission)) {
                            fieldNode.setSelected(true);
                        }
                        // check for parent permission (only in case of fields)
                        Permission parentPermission = permissionFactory.create(action, entityObjectFieldResource.getParent());
                        if (contains(getCurrentPermissions(), parentPermission)) {
                            fieldNode.setSelected(true);
                        }
                    }
                } else {
                    // no fields, only action
                    Permission permission = permissionFactory.create(action, entityObjectResource);
                    initialPermissions.add(permission);
                    if (contains(getCurrentPermissions(), permission)) {
                        actionNode.setSelected(true);
                    }
                }
                selectedNodes = ArrayUtils.addAll(selectedNodes, propagateSelectionUpwards(actionNode));
            }
            selectedNodes = ArrayUtils.addAll(selectedNodes, propagateSelectionUpwards(entityNode));
        }
    }

    public void processSelectedPermissions() {

        Set<Permission> selectedPermissions = getSelectedPermissions(selectedNodes, false);
        Set<Permission> deSelectedPermissions = new HashSet<Permission>();
        List<Permission> currentPermissions = getCurrentPermissions();
        notRevokable.clear();
        for (Permission initialPermission : initialPermissions) {
            if (!contains(selectedPermissions, initialPermission)) {
                // not selected
                if (implies(currentPermissions, initialPermission)) {
                    if (isRevokable(initialPermission)) {
                        deSelectedPermissions.add(initialPermission);
                        // TODO notify user that this cannot be revoked
                    } else {
                        notRevokable.add(initialPermission);
                    }
                }
            }
        }
        revokedPermissions = new HashSet<Permission>();
        revokedPermissions.addAll(deSelectedPermissions);
        // new permission will replace...
        for (Permission selectedPermission : selectedPermissions) {
            if (isGrantable(selectedPermission)) {
                revokedPermissions.addAll(getRevokablePermissions(selectedPermission));
            } else {
                notGrantable.add(selectedPermission);
            }
        }

        // build current tree
        currentRoot = buildPermissionTree(currentPermissions, revokedPermissions, Marking.RED);
        // build new tree
        List<Permission> newCurrentPermissions = new ArrayList<Permission>(currentPermissions);
        grantedPermissions = new HashSet<Permission>();
        notGrantable.clear();
        for (Permission selectedPermission : selectedPermissions) {
            if (isGrantable(selectedPermission)) {
                grantedPermissions.add(selectedPermission);
            } else {
                notGrantable.add(selectedPermission);
            }

        }
        newCurrentPermissions = (List<Permission>) removeAll(newCurrentPermissions, revokedPermissions);
        newCurrentPermissions.addAll(grantedPermissions);
        newRoot = buildPermissionTree(newCurrentPermissions, grantedPermissions, Marking.GREEN);

    }

    private Collection<? extends Permission> getRevokablePermissions(Permission selectedPermission) {
        if (selectedSubject instanceof Subject) {
            return permissionDataAccess.getRevokablePermissionsWhenRevoking((Subject) selectedSubject, selectedPermission.getAction(), selectedPermission.getResource());
        } else {
            if (selectedSubject instanceof Role) {
                return permissionDataAccess.getRevokablePermissionsWhenRevoking((Role) selectedSubject, selectedPermission.getAction(), selectedPermission.getResource());
            }
        }
        throw new IllegalArgumentException();
    }

    private boolean isRevokable(Permission initialPermission) {
        if (selectedSubject instanceof Subject) {
            return permissionDataAccess.isRevokable((Subject) selectedSubject, initialPermission.getAction(), initialPermission.getResource());
        } else {
            if (selectedSubject instanceof Role) {
                return permissionDataAccess.isRevokable((Role) selectedSubject, initialPermission.getAction(), initialPermission.getResource());
            }
        }
        throw new IllegalArgumentException();
    }

    private boolean isGrantable(Permission initialPermission) {
        if (selectedSubject instanceof Subject) {
            return permissionDataAccess.isGrantable((Subject) selectedSubject, initialPermission.getAction(), initialPermission.getResource());
        } else {
            if (selectedSubject instanceof Role) {
                return permissionDataAccess.isGrantable((Role) selectedSubject, initialPermission.getAction(), initialPermission.getResource());
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * 
     * @param permissions
     * @param markedPermissions
     */
    private TreeNode buildPermissionTree(List<Permission> permissions, Set<Permission> markedPermissions, Marking marking) {
        TreeNode currentPermissionTreeRoot = new DefaultTreeNode();
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), currentPermissionTreeRoot);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);

            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission permission : permissionsByAction) {
                    // entity with field-> create node
                    if (!((EntityField) permission.getResource()).isEmptyField()) {

                        DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
                            permission.getResource(), Marking.NONE), actionNode);

                        // set marking
                        if (permission.getResource() instanceof EntityObjectField) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.BLUE);
                            ((TreeNodeModel) fieldNode.getData()).setTooltip(Constants.CONTAINS_OBJECTS);
                        }
                        if (contains(markedPermissions, permission)) {
                            ((TreeNodeModel) fieldNode.getData()).setMarking(marking);
                        }
                    } else {
                        if (permission.getResource() instanceof EntityObjectField) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.BLUE);
                            ((TreeNodeModel) actionNode.getData()).setTooltip(Constants.CONTAINS_OBJECTS);
                        }
                        // TODO override blue marking with given marking????
                        // entity without field permission -> dont create node but mark action if permission is marked
                        if (contains(markedPermissions, permission)) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(marking);
                        }
                    }
                }
                propagateMarkingUpwards(actionNode);
            }
            propagateMarkingUpwards(entityNode);
        }
        return currentPermissionTreeRoot;
    }

    private List<Permission> getCurrentPermissions() {
        if (selectedSubject instanceof Subject) {
            return permissionManager.getPermissions((Subject) selectedSubject);
        } else {
            return permissionManager.getPermissions((Role) selectedSubject);
        }
    }

    public void confirmSelectedPermissions() {
        for (Permission permission : revokedPermissions) {
            revoke(permission);
        }
        for (Permission permission : grantedPermissions) {
            grant(permission);
        }
        init();
    }

    private void grant(Permission p) {
        if (selectedSubject instanceof Subject) {
            permissionService.grant(userSession.getUser(), (Subject) selectedSubject, p.getAction(), p.getResource());
        } else {
            if (selectedSubject instanceof Role) {
                permissionService.grant(userSession.getUser(), (Role) selectedSubject, p.getAction(), p.getResource());
            }
        }

    }

    private void revoke(Permission p) {
        if (selectedSubject instanceof Subject) {
            permissionService.revoke(userSession.getUser(), (Subject) selectedSubject, p.getAction(), p.getResource());
        } else {
            if (selectedSubject instanceof Role) {
                permissionService.revoke(userSession.getUser(), (Role) selectedSubject, p.getAction(), p.getResource());
            }
        }

    }

    public String permissionWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("selectPermissions")) {
            processSelectedPermissions();
        }
        return event.getNewStep();
    }

    public List<RowModel> getSelectedObjects() {
        return selectedObjects;
    }

    public void setSelectedObjects(List<RowModel> selectedObjects) {
        this.selectedObjects = selectedObjects;
    }

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public void setSelectedFields(List<String> selectedFields) {
        this.selectedFields = selectedFields;
    }

    public IdHolder getSelectedSubject() {
        return selectedSubject;
    }

    public void setSelectedSubject(IdHolder selectedSubject) {
        this.selectedSubject = selectedSubject;
    }

    public List<EntityAction> getSelectedActions() {
        return selectedActions;
    }

    public void setSelectedActions(List<EntityAction> selectedActions) {
        this.selectedActions = selectedActions;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(DefaultTreeNode root) {
        this.root = root;
    }

    public TreeNode getCurrentRoot() {
        return currentRoot;
    }

    public void setCurrentRoot(DefaultTreeNode currentRoot) {
        this.currentRoot = currentRoot;
    }

    public TreeNode getNewRoot() {
        return newRoot;
    }

    public void setNewRoot(DefaultTreeNode newRoot) {
        this.newRoot = newRoot;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public List<Permission> getNotRevokable() {
        return new ArrayList<Permission>(notRevokable);
    }

    public List<Permission> getNotGrantable() {
        return new ArrayList<Permission>();
    }

}
