/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.user;

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
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.bean.base.ResourceHandlingBaseBean;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "userResourcesBean")
public class UserResourcesBean extends ResourceHandlingBaseBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private TreeNode[] selectedPermissionNodes = new TreeNode[] {};
    private TreeNode[] selectedResourceNodes = new TreeNode[] {};

    private TreeNode newPermissionTreeRoot = new DefaultTreeNode();
    private TreeNode currentPermissionTreeRoot = new DefaultTreeNode();

    private List<Permission> userPermissions = new ArrayList<Permission>();
    private List<Permission> userDataPermissions = new ArrayList<Permission>();
    private List<Permission> allPermissions = new ArrayList<Permission>();

    private DefaultTreeNode resourceRoot;

    private TreeNode permissionViewRoot;

    private Set<Permission> replaced = new HashSet<Permission>();
    private Set<Permission> revokable = new HashSet<Permission>();

    public void init() {
        initPermissions();
        initPermissionTree();
        initResourceTree();
    }

    private String filter;

    public void filterTree() {
        try {
            resourceRoot = getResourceTree(filter);
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
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
        List<List<Permission>> all = resourceUtils.getSeparatedPermissionsByResource(allPermissions);
        userPermissions = all.get(0);
        userDataPermissions = all.get(1);
    }

    private void initPermissionTree() {
        this.permissionViewRoot = new DefaultTreeNode();
        permissionViewRoot = getImmutablePermissionTree(userPermissions, userDataPermissions, !isEnabled(Company.FIELD_LEVEL));
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
        selectedPermissionNodes = new TreeNode[] {};
        // read selected resources
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        if (!isEnabled(Company.FIELD_LEVEL)) {
            // if field is level is not enabled but the user has field level permissions, these need to be marked as selected,
            // otherwise it would be taken as revoked
            selectedPermissions.addAll(permissionHandling.getSeparatedParentAndChildPermissions(userPermissions).get(1));
        }
        // check what has been revoked
        List<Set<Permission>> revoke = permissionHandling.getRevokableFromSelected(userPermissions, selectedPermissions);
        revokable = revoke.get(0);
        dialogBean.setNotRevoked(revoke.get(1));

        // remove revoked permissions from current permission list so we can check what can be granted after revoking
        // check what has been granted
        List<Set<Permission>> grant = permissionHandling.getGrantable(permissionHandling.removeAll(userPermissions, revokable), selectedPermissions);
        Set<Permission> granted = grant.get(0);
        dialogBean.setNotGranted(grant.get(1));

        replaced = permissionHandling.getReplacedByGranting(allPermissions, granted);

        currentPermissionTreeRoot = buildCurrentPermissionTree(userPermissions, userDataPermissions, granted, revokable, replaced, !isEnabled(Company.FIELD_LEVEL));
        newPermissionTreeRoot = buildNewPermissionTree(userPermissions, userDataPermissions, granted, revokable, replaced, !isEnabled(Company.FIELD_LEVEL),
                                                       isEnabled(Company.USER_LEVEL));
    }

    /**
     * wizard step 2: confirm button when adding permissions to user
     * 
     */
    public void confirmPermissions() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        executeRevokeAndGrant(getSelectedUser(), allPermissions, selectedPermissions, revokable, replaced);
        init();
    }

    public void rebuildCurrentPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        rebuildCurrentPermissionTree();
    }

    public void rebuildCurrentPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
        rebuildCurrentPermissionTree();
    }

    /**
     * changes after wizard step1, before confirm. listener for select unselect permissons in the new permission tree
     */
    public void rebuildCurrentPermissionTree() {
        // current selected permissions
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);
        currentPermissionTreeRoot = rebuildCurrentTree(allPermissions, selectedPermissions, revokable, replaced, !isEnabled(Company.FIELD_LEVEL));
    }

    public DefaultTreeNode getResourceRoot() {
        return resourceRoot;
    }

    public User getSelectedUser() {
        return userSession.getSelectedUser();
    }

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

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

}
