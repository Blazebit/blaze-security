/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.ResourceHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;

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
    private TreeNode[] selectedResourceNodes = new TreeNode[] {};

    private TreeNode newPermissionTreeRoot;
    private TreeNode currentPermissionTreeRoot;

    private List<Permission> userPermissions = new ArrayList<Permission>();
    private List<Permission> userDataPermissions = new ArrayList<Permission>();

    private DefaultTreeNode resourceRoot;

    private TreeNode permissionViewRoot;

    public void init() {
        initPermissions();
        try {
            resourceRoot = getResourceTree(getCurrentPermissions());
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
    }

    private void initPermissions() {
        List<List<Permission>> allPermissions = filterPermissions(permissionManager.getPermissions(getSelectedUser()));
        userPermissions = allPermissions.get(0);
        userDataPermissions = allPermissions.get(1);
        this.permissionViewRoot = new DefaultTreeNode();
        permissionViewRoot = getPermissionTree(permissionManager.getPermissions(getSelectedUser()));

    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            processSelectedResources();
        }

        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    public void processSelectedResources() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        List<Set<Permission>> revoke = getRevokedPermissions(getCurrentPermissions(), selectedPermissions);
        Set<Permission> revoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));
        List<Set<Permission>> grant = getGrantedPermission(getCurrentPermissions(), selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        Set<Permission> replaced = getReplacedPermissions(getCurrentPermissions(), granted);
        // modify current user permissions based on resource selection
        List<Permission> currentUserPermissions = new ArrayList<Permission>(getCurrentPermissions());
        // current permission tree
        Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
        removedPermissions.addAll(replaced);
        currentPermissionTreeRoot = getPermissionTree(currentUserPermissions, removedPermissions, Marking.REMOVED);
        // new permission tree without the replaced but with the granted + revoked ones, marked properly
        currentUserPermissions.removeAll(replaced);
        currentUserPermissions.addAll(granted);
        newPermissionTreeRoot = getSelectablePermissionTree(currentUserPermissions, granted, revoked, Marking.NEW, Marking.REMOVED);
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmPermissions() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = getReplacedPermissions(getCurrentPermissions(), selectedResourcePermissions);
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        selectedPermissions.addAll(previouslyReplaced);
        Set<Permission> granted = getGrantedPermission(getCurrentPermissions(), selectedPermissions).get(0);
        Set<Permission> replaced = getReplacedPermissions(getCurrentPermissions(), granted);

        Set<Permission> revoked = getRevokedPermissions(getCurrentPermissions(), selectedPermissions).get(0);
        Set<Permission> finalGranted = getPermissionsWithImpliedActionsToGrant(getCurrentPermissions(), granted);
        Set<Permission> finalRevoked = getPermissionsWithImpliedActionsToRevoke(getCurrentPermissions(), revoked);

        for (Permission permission : finalRevoked) {
            permissionService.revoke(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        }
        for (Permission permission : replaced) {
            permissionService.revoke(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        }
        for (Permission permission : finalGranted) {
            permissionService.grant(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        }
        init();
    }

    /**
     * listener for select unselect permissons in the new permission tree
     */
    public void rebuildCurrentPermissionTree() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = getReplacedPermissions(getCurrentPermissions(), selectedResourcePermissions);

        // current selected permissions
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        selectedPermissions.addAll(previouslyReplaced);

        Set<Permission> granted = getGrantedPermission(getCurrentPermissions(), selectedPermissions).get(0);
        Set<Permission> replaced = getReplacedPermissions(getCurrentPermissions(), granted);
        Set<Permission> revoked = getRevokedPermissions(getCurrentPermissions(), selectedPermissions).get(0);

        Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
        removedPermissions.addAll(replaced);
        // current permission tree
        currentPermissionTreeRoot = getPermissionTree(getCurrentPermissions(), removedPermissions, Marking.REMOVED);

    }

    public DefaultTreeNode getResourceRoot() {
        return resourceRoot;
    }

    public User getSelectedUser() {
        return userSession.getSelectedUser();
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionViewRoot;
    }

    public TreeNode getNewPermissionTreeRoot() {
        return newPermissionTreeRoot;
    }

    public void setNewPermissionTreeRoot(DefaultTreeNode newPermissionTreeRoot) {
        this.newPermissionTreeRoot = newPermissionTreeRoot;
    }

    public TreeNode getCurrentPermissionTreeRoot() {
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

    public List<Permission> getCurrentPermissions() {
        List<Permission> all = new ArrayList<Permission>(userPermissions);
        all.addAll(userDataPermissions);
        return all;
    }

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedResourceNodes) {
        this.selectedResourceNodes = selectedResourceNodes;
    }

}
