package com.blazebit.security.web.bean.resources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.inject.Named;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.web.bean.PermissionTreeHandlingBaseBean;
import com.blazebit.security.web.bean.model.FieldModel;
import com.blazebit.security.web.bean.model.RowModel;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.util.FieldUtils;

@Named
@ViewScoped
@Stateless
public class ResourceObjectBean extends PermissionTreeHandlingBaseBean {

    private IdHolder selectedSubject;
    private List<RowModel> selectedObjects = new ArrayList<RowModel>();
    private List<FieldModel> selectedFields = new ArrayList<FieldModel>();
    private List<EntityAction> selectedActions = new ArrayList<EntityAction>();
    private String action = "grant";

    private TreeNode resourceRoot = new DefaultTreeNode();
    private TreeNode[] selectedResourceNodes = new TreeNode[] {};
    private TreeNode[] selectedPermissionNodes = new TreeNode[] {};

    private TreeNode currentPermissionRoot = new DefaultTreeNode();
    private TreeNode newPermissionRoot = new DefaultTreeNode();

    private List<Permission> currentPermissions = new ArrayList<Permission>();

    private boolean confirmed = false;

    public void init() {
        currentPermissions = getCurrentPermissions();
        resourceRoot = new DefaultTreeNode();
        confirmed = false;
        for (RowModel selectedObject : selectedObjects) {
            EntityObjectField entityObjectResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), selectedObject
                .getEntity()
                .getId());
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(selectedObject.getFieldSummary(), ResourceType.ENTITY, entityObjectResource), resourceRoot);
            entityNode.setExpanded(true);

            createActionNodes(selectedActions, selectedObject, entityObjectResource, entityNode);
            createActionNodes(selectedCollectionActions, selectedObject, entityObjectResource, entityNode);

            propagateNodePropertiesTo(entityNode);
        }
    }

    private void createActionNodes(List<EntityAction> selectedActions, RowModel selectedObject, EntityObjectField entityObjectResource, DefaultTreeNode entityNode) {
        for (EntityAction action : selectedActions) {
            DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(action.getActionName(), ResourceType.ACTION, action), entityNode);
            actionNode.setExpanded(true);
            // fields
            if (!selectedFields.isEmpty()) {
                // selectedFields
                List<Field> fields = FieldUtils.getPrimitiveFields(selectedObject.getEntity().getClass());
                for (Field field : fields) {
                    EntityObjectField entityObjectFieldResource = (EntityObjectField) entityFieldFactory.createResource(selectedObject.getEntity().getClass(), field.getName(),
                                                                                                                        selectedObject.getEntity().getId());
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(field.getName(), ResourceType.FIELD, entityObjectFieldResource), actionNode);

                    Permission permission = permissionFactory.create(action, entityObjectFieldResource);
                    setFieldNodeProperties(field, fieldNode, permission);
                }
            } else {
                // no fields, only action
                Permission permission = permissionFactory.create(action, entityObjectResource);
                setActionNodeProperties(action, actionNode, permission);
            }

            propagateSelectionUpwards(actionNode);
        }
    }

    private void setActionNodeProperties(EntityAction action, DefaultTreeNode actionNode, Permission permission) {
        if (this.action.equals("grant")) {
            if (implies(currentPermissions, permission)) {
                // already existing permission -> when granting dont allow user to revoke
                actionNode.setSelectable(false);
            } else {
                actionNode.setSelected(true);
                ((TreeNodeModel) actionNode.getData()).setMarking(Marking.NEW);
            }
        } else {
            if (this.action.equals("revoke")) {
                if (revokes(currentPermissions, permission)) {
                    actionNode.setSelected(true);
                    ((TreeNodeModel) actionNode.getData()).setMarking(Marking.REMOVED);
                }
            }
        }
    }

    private void setFieldNodeProperties(Field field, DefaultTreeNode fieldNode, Permission permission) {
        if (this.action.equals("grant")) {
            if (implies(currentPermissions, permission)) {
                fieldNode.setSelectable(false);
            } else {
                if (selectedFields.contains(new FieldModel(field.getName()))) {
                    fieldNode.setSelected(true);
                    ((TreeNodeModel) fieldNode.getData()).setMarking(Marking.NEW);
                }
            }
        } else {
            if (this.action.equals("revoke")) {
                if (selectedFields.contains(new FieldModel(field.getName()))) {
                    fieldNode.setSelected(true);
                    ((TreeNodeModel) fieldNode.getData()).setMarking(Marking.REMOVED);
                }
            }
        }
    }

    // wizard step
    public void processSelectedPermissions() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        if ("grant".equals(action)) {
            Set<Permission> granted = getGrantedPermission(currentPermissions, selectedPermissions).get(0);
            super.setNotGranted(getGrantedPermission(currentPermissions, selectedPermissions).get(1));
            Set<Permission> revoked = getRevokedPermissions(currentPermissions, selectedPermissions).get(0);
            super.setNotRevoked(getRevokedPermissions(currentPermissions, selectedPermissions).get(1));
            Set<Permission> replaced = getReplacedPermissions(currentPermissions, selectedPermissions);
            Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
            removedPermissions.addAll(replaced);
            // build current tree
            // revoked or replaced permissions cannot be displayed because they can only be object permissions and those cannot
            // be marked
            currentPermissionRoot = getPermissionTree(currentPermissions, new HashSet<Permission>(), Marking.REMOVED);
            // build new tree
            List<Permission> modifiedCurrentPermissions = new ArrayList<Permission>(currentPermissions);
            modifiedCurrentPermissions.removeAll(removedPermissions);
            modifiedCurrentPermissions.addAll(granted);
            newPermissionRoot = getPermissionTree(modifiedCurrentPermissions, selectedPermissions, Marking.NEW);
        } else {
            if ("revoke".equals(action)) {
                Set<Permission> revoked = getRevokablePermissions(selectedPermissions);
                Set<Permission> finalRevoked = getPermissionsWithImpliedActionsToRevoke(currentPermissions, revoked);
                currentPermissionRoot = getPermissionTree(currentPermissions, finalRevoked, Marking.REMOVED);
                // build new tree
                List<Permission> modifiedCurrentPermissions = new ArrayList<Permission>(currentPermissions);
                modifiedCurrentPermissions.removeAll(finalRevoked);
                newPermissionRoot = getPermissionTree(modifiedCurrentPermissions, new HashSet<Permission>(), Marking.NONE);
            }
        }

    }

    private List<Permission> getCurrentPermissions() {
        if (selectedSubject instanceof Subject) {
            return permissionManager.getPermissions((Subject) selectedSubject);
        } else {
            return permissionManager.getPermissions((Role) selectedSubject);
        }
    }

    // confirm
    public void confirmSelectedPermissions() {
        if (action.equals("grant")) {
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
            Set<Permission> granted = getGrantedPermission(currentPermissions, selectedPermissions).get(0);
            super.setNotGranted(getGrantedPermission(currentPermissions, selectedPermissions).get(1));
            Set<Permission> replaced = getReplacedPermissions(currentPermissions, selectedPermissions);
            // fix implied actions
            Set<Permission> finalGranted = getPermissionsWithImpliedActionsToGrant(getCurrentPermissions(), granted);

            for (Permission permission : replaced) {
                revoke(permission);
            }
            for (Permission permission : finalGranted) {
                grant(permission);
            }
        } else {
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
            Set<Permission> revoked = getRevokablePermissions(selectedPermissions);
            // fix implied actions
            Set<Permission> finalRevoked = getPermissionsWithImpliedActionsToRevoke(currentPermissions, revoked);

            for (Permission permission : finalRevoked) {
                revoke(permission);
            }
        }
        // reset permissions
        currentPermissions = getCurrentPermissions();
        currentPermissionRoot = getPermissionTree(currentPermissions, new HashSet<Permission>(), Marking.REMOVED);
        // build new tree
        newPermissionRoot = new DefaultTreeNode();
        // reset initial values
        selectedFields.clear();
        selectedActions.clear();
        selectedCollectionActions.clear();
        selectedObjects.clear();
        setConfirmed(true);
    }

    private Set<Permission> getRevokablePermissions(Set<Permission> selectedPermissions) {
        Set<Permission> revoked = new HashSet<Permission>();
        Set<Permission> granted = new HashSet<Permission>();
        for (Permission permission : selectedPermissions) {
            if (permissionDataAccess.isRevokable(currentPermissions, permission.getAction(), permission.getResource())) {
                revoked.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(currentPermissions, permission.getAction(), permission.getResource()));
            } // else {
              // handle the case when revoking entity field from entity
              // TODO difficult because it needs granting
              // }
        }
        return revoked;
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

    // wizard
    public String permissionWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("selectPermissions")) {
            processSelectedPermissions();
        } else {
            System.out.println(event);
        }
        return event.getNewStep();
    }

    // initialize bean
    public void setSelectedObjects(List<RowModel> selectedObjects) {
        this.selectedObjects = selectedObjects;
    }

    public void setSelectedFields(List<FieldModel> selectedFields) {
        this.selectedFields = selectedFields;
    }

    public void setSelectedSubject(IdHolder selectedSubject) {
        this.selectedSubject = selectedSubject;
    }

    public void setSelectedActions(List<EntityAction> selectedActions) {
        this.selectedActions = selectedActions;
    }

    public void setSelectedCollectionActions(List<EntityAction> selectedCollectionActions) {
        this.selectedCollectionActions = selectedCollectionActions;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public TreeNode getCurrentPermissionRoot() {
        return currentPermissionRoot;
    }

    public void setCurrentPermissionRoot(TreeNode currentPermissionRoot) {
        this.currentPermissionRoot = currentPermissionRoot;
    }

    public TreeNode getNewPermissionRoot() {
        return newPermissionRoot;
    }

    public void setNewPermissionRoot(TreeNode newPermissionRoot) {
        this.newPermissionRoot = newPermissionRoot;
    }

    public String getSelectedSubject() {
        return selectedSubject.toString();
    }

    public String getAction() {
        return action;
    }

    public List<FieldModel> getSelectedFields() {
        return selectedFields;
    }

    public List<RowModel> getSelectedObjects() {
        return selectedObjects;
    }

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedResourceNodes) {
        this.selectedResourceNodes = selectedResourceNodes;
    }

    public TreeNode getResourceRoot() {
        return resourceRoot;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedPermissionNodes = selectedPermissionNodes;
    }

}
