package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.bean.model.NodeModel.ResourceType;
import com.blazebit.security.web.bean.model.RowModel;

@Named
@ViewScoped
@Stateless
public class PermissionManager extends PermissionHandlingBaseBean {

    @Inject
    private PermissionDataAccess permissionDataAccess;

    @Inject
    private com.blazebit.security.PermissionManager permissionManagerService;

    public IdHolder selectedSubject;
    private List<RowModel> selectedObjects = new ArrayList<RowModel>();
    private List<String> selectedFields = new ArrayList<String>();
    private List<EntityAction> selectedActions = new ArrayList<EntityAction>();

    private DefaultTreeNode root;

    private TreeNode[] selectedNodes = new TreeNode[] {};

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    @PostConstruct
    public void init() {
        root = new DefaultTreeNode();
        for (RowModel selectedObject : selectedObjects) {
            Resource entityObjectResource = entityFieldFactory.createResource(selectedObject.getEntity().getClass(), selectedObject.getEntity().getId());
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(selectedObject.getFieldSummary(), ResourceType.ENTITY, entityObjectResource), root);
            entityNode.setExpanded(true);
            for (EntityAction action : selectedActions) {
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(action.getActionName(), ResourceType.ACTION, action), entityNode);
                actionNode.setExpanded(true);
                if (selectedSubject instanceof Subject) {
                    if (permissionDataAccess.findPermission((Subject) selectedSubject, action, entityObjectResource) != null) {
                        actionNode.setSelected(true);
                        addNodeToSelectedNodes(actionNode, selectedNodes);
                    }
                } else {
                    if (permissionDataAccess.findPermission((Role) selectedSubject, action, entityObjectResource) != null) {
                        actionNode.setSelected(true);
                        addNodeToSelectedNodes(actionNode, selectedNodes);
                    }
                }
                for (String field : selectedFields) {
                    Resource entityObjectFieldResource = entityFieldFactory.createResource(selectedObject.getEntity().getClass(), field, selectedObject.getEntity().getId());
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(field, ResourceType.FIELD, entityObjectFieldResource), actionNode);
                    if (selectedSubject instanceof Subject) {
                        if (permissionDataAccess.findPermission((Subject) selectedSubject, action, entityObjectFieldResource) != null) {
                            fieldNode.setSelected(true);
                            addNodeToSelectedNodes(fieldNode, selectedNodes);
                        }
                    } else {
                        if (permissionDataAccess.findPermission((Role) selectedSubject, action, entityObjectFieldResource) != null) {
                            fieldNode.setSelected(true);
                            addNodeToSelectedNodes(fieldNode, selectedNodes);
                        }
                    }
                }
            }
        }
    }

    public void confirm() {
        Set<Permission> selectedPermissions = processSelectedPermissions(selectedNodes, false);
        List<Permission> currentPermissions;
        if (selectedSubject instanceof Subject) {
            currentPermissions = permissionManagerService.getPermissions((Subject) selectedSubject);
            for (Permission p : selectedPermissions) {
                if (!contains(currentPermissions, p)) {
                    // added
                    permissionService.grant(userSession.getUser(), (Subject) selectedSubject, p.getAction(), p.getResource());
                }

            }
            for (Permission p : currentPermissions) {
                if (!contains(selectedPermissions, p)) {
                    // removed
                    permissionService.revoke(userSession.getUser(), (Subject) selectedSubject, p.getAction(), p.getResource());
                }

            }
        } else {
            currentPermissions = permissionManagerService.getPermissions((Role) selectedSubject);
            for (Permission p : selectedPermissions) {
                if (!contains(currentPermissions, p)) {
                    // added
                    permissionService.grant(userSession.getUser(), (Role) selectedSubject, p.getAction(), p.getResource());
                }

            }
            for (Permission p : currentPermissions) {
                if (!contains(selectedPermissions, p)) {
                    // removed
                    permissionService.revoke(userSession.getUser(), (Role) selectedSubject, p.getAction(), p.getResource());
                }

            }
        }

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

    public Object getSelectedSubject() {
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

    public DefaultTreeNode getRoot() {
        return root;
    }

    public void setRoot(DefaultTreeNode root) {
        this.root = root;
    }

}
