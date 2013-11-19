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
    private List<Permission> allPermissions = new ArrayList<Permission>();

    private DefaultTreeNode resourceRoot;

    private TreeNode permissionViewRoot;

    public void init() {
        initPermissions();
        initPermissionTree();
        initResourceTree();
    }

    private void initResourceTree() {
        try {
            resourceRoot = getResourceTree(userPermissions);
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
    }

    private void initPermissions() {
        allPermissions = permissionManager.getPermissions(getSelectedUser());
        List<List<Permission>> all = filterPermissions(allPermissions);
        userPermissions = all.get(0);
        userDataPermissions = all.get(1);
    }

    private void initPermissionTree() {
        this.permissionViewRoot = new DefaultTreeNode();
        permissionViewRoot = getPermissionTree(userPermissions, userDataPermissions);
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
        //read selected resources
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        //add automatically object permissions because they could not have been unselected
        selectedPermissions.addAll(userDataPermissions);
        //check what has been revoked
        List<Set<Permission>> revoke = getRevokedPermissions(allPermissions, selectedPermissions);
        Set<Permission> revoked = revoke.get(0);
        super.setNotRevoked(revoke.get(1));
        //check what has been granted
        List<Set<Permission>> grant = getGrantedPermission(allPermissions, selectedPermissions);
        Set<Permission> granted = grant.get(0);
        super.setNotGranted(grant.get(1));
        //replaced
        Set<Permission> replaced = getReplacedPermissions(allPermissions, selectedPermissions);

        // current permission tree
        Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
        removedPermissions.addAll(replaced);
        currentPermissionTreeRoot = getPermissionTree(userPermissions, userDataPermissions, removedPermissions, Marking.REMOVED);

        // modify current user permissions based on resource selection
        List<Permission> currentUserPermissions = new ArrayList<Permission>(userPermissions);
        
        // new permission tree without the replaced but with the granted + revoked ones, marked properly
        currentUserPermissions.removeAll(replaced);
        currentUserPermissions.addAll(granted);
        newPermissionTreeRoot = getSelectablePermissionTree(currentUserPermissions, userDataPermissions, granted, revoked, Marking.NEW, Marking.REMOVED);
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void confirmPermissions() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = getReplacedPermissions(allPermissions, selectedResourcePermissions);
        
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        selectedPermissions.addAll(previouslyReplaced);
        
        Set<Permission> granted = getGrantedPermission(allPermissions, selectedPermissions).get(0);
        // Set<Permission> replaced = getReplacedPermissions(userPermissions, granted);
        Set<Permission> revoked = getRevokedPermissions(allPermissions, selectedPermissions).get(0);

        Set<Permission> finalGranted = grantImpliedPermissions(allPermissions, granted);
        Set<Permission> finalRevoked = revokeImpliedPermissions(allPermissions, revoked);

        for (Permission permission : finalRevoked) {
            permissionService.revoke(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        }
        // for (Permission permission : replaced) {
        // permissionService.revoke(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        // }
        for (Permission permission : finalGranted) {
            permissionService.grant(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        }

        // TODO normalize -> needed after implied action permissions. here or in service layer?
        // initPermissions();
        // Set<Permission> grant = normalizePermissions(userPermissions).get(0);
        // Set<Permission> revoke = normalizePermissions(userPermissions).get(1);
        //
        // for (Permission permission : revoke) {
        // permissionService.revoke(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        // }
        // for (Permission permission : grant) {
        // permissionService.grant(userSession.getUser(), getSelectedUser(), permission.getAction(), permission.getResource());
        // }
        init();
    }

    /**
     * listener for select unselect permissons in the new permission tree
     */
    public void rebuildCurrentPermissionTree() {
        Set<Permission> selectedResourcePermissions = getSelectedPermissions(selectedResourceNodes);
        Set<Permission> previouslyReplaced = getReplacedPermissions(allPermissions, selectedResourcePermissions);

        // current selected permissions
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        selectedPermissions.addAll(previouslyReplaced);

        Set<Permission> granted = getGrantedPermission(allPermissions, selectedPermissions).get(0);
        Set<Permission> replaced = getReplacedPermissions(allPermissions, granted);
        Set<Permission> revoked = getRevokedPermissions(allPermissions, selectedPermissions).get(0);

        Set<Permission> removedPermissions = new HashSet<Permission>(revoked);
        removedPermissions.addAll(replaced);
        // current permission tree
        currentPermissionTreeRoot = getPermissionTree(userPermissions, userDataPermissions, removedPermissions, Marking.REMOVED);

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

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedResourceNodes) {
        this.selectedResourceNodes = selectedResourceNodes;
    }

}
