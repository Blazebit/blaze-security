/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.PermissionUtils;
import com.blazebit.security.entity.EntityPermissionUtils;
import com.blazebit.security.model.Features;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.web.bean.base.ResourceGroupHandlingBaseBean;
import com.blazebit.security.web.bean.model.TreeNodeModel;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "groupResourcesBean")
public class GroupResourcesBean extends ResourceGroupHandlingBaseBean {

    private static final long serialVersionUID = 1L;

    private List<Permission> allPermissions = new ArrayList<Permission>();
    private List<Permission> groupPermissions = new ArrayList<Permission>();
    private List<Permission> groupDataPermissions = new ArrayList<Permission>();
    // wizard 1 step
    private TreeNode resourceRoot;
    private TreeNode[] selectedResourceNodes = new TreeNode[] {};
    // wizard 2 step
    private TreeNode newPermissionTreeRoot;
    private TreeNode currentPermissionTreeRoot;
    private TreeNode[] selectedGroupPermissionNodes = new TreeNode[] {};
    // wizard 3 step - propagation
    private DefaultTreeNode currentUserPermissionTreeRoot;
    private DefaultTreeNode newUserPermissionTreeRoot;
    private TreeNode[] selectedUserPermissionNodes = new TreeNode[] {};
    // permissionview
    private TreeNode permissionViewRoot;
    // group - initial revoke and replace. needed for rebuild and confirm.
    private Set<Permission> revokable;
    private Set<Permission> replacable;
    // result of granting and revoking for groups. propagate these!
    private Set<Permission> groupGranted;
    private Set<Permission> groupRevoked;

    public void init() {
        initPermissions();
        initPermissionTree();
        initResourceTree();
    }

    private void initResourceTree() {
        try {
            resourceRoot = getResourceTree(groupPermissions);
        } catch (ClassNotFoundException e) {
            System.err.println("Error in resource name provider!");
        }
    }

    private void initPermissions() {
        allPermissions = permissionManager.getPermissions(getSelectedGroup());
        groupPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
        groupDataPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(1);
    }

    private void initPermissionTree() {
        this.permissionViewRoot = initGroupPermissions(getSelectedGroup(), !isEnabled(Features.FIELD_LEVEL));
    }

    public String resourceWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("resources")) {
            processSelectedResources();
        } else {
            if (event.getOldStep().equals("permissions") && !event.getNewStep().equals("resources")) {
                processGroupPermissions();
            }
        }
        return event.getNewStep();
    }

    /**
     * wizard step 1
     */
    public void processSelectedResources() {
        // get selected permissions
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedResourceNodes);
        if (!isEnabled(Features.FIELD_LEVEL)) {
            // if field is level is not enabled but the user has field level permissions, these need to be marked as selected,
            // otherwise it would be taken as revoked
            selectedPermissions.addAll(PermissionUtils.getSeparatedParentAndChildPermissions(groupPermissions).get(1));
        }
        // get revoked permissions
        List<Set<Permission>> revoke = permissionHandling.getRevokableFromSelected(groupPermissions, selectedPermissions);
        revokable = revoke.get(0);
        dialogBean.setNotRevoked(revoke.get(1));
        // get granted permissions
        List<Set<Permission>> grant = permissionHandling.getGrantable(PermissionUtils.removeAll(groupPermissions, revokable), selectedPermissions);
        Set<Permission> granted = grant.get(0);
        dialogBean.setNotGranted(grant.get(1));

        // get replaced permissions
        replacable = permissionHandling.getReplacedByGranting(allPermissions, granted);
        // build trees
        currentPermissionTreeRoot = buildCurrentPermissionTree(groupPermissions, groupDataPermissions, granted, revokable, replacable, !isEnabled(Features.FIELD_LEVEL));
        newPermissionTreeRoot = buildNewPermissionTree(groupPermissions, groupDataPermissions, granted, revokable, replacable, !isEnabled(Features.FIELD_LEVEL), true);
    }

    public void rebuildCurrentGroupPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        rebuildCurrentGroupPermissionTree();
    }

    public void rebuildCurrentGroupPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
        rebuildCurrentGroupPermissionTree();
    }

    public void rebuildCurrentGroupPermissionTree() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        currentPermissionTreeRoot = rebuildCurrentTree(allPermissions, selectedPermissions, revokable, replacable, !isEnabled(Features.FIELD_LEVEL));
    }

    /**
     * confirm button when adding permissions to user
     * 
     */
    public void processGroupPermissions() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedGroupPermissionNodes);
        List<Set<Permission>> result = executeRevokeAndGrant(getSelectedGroup(), groupPermissions, selectedPermissions, revokable, replacable, true);
        groupGranted = result.get(1);
        groupRevoked = result.get(0);
        currentUserPermissionTreeRoot = new DefaultTreeNode();
        newUserPermissionTreeRoot = new DefaultTreeNode();
        //show user view
        prepareUserPropagationView(getSelectedGroup(), groupGranted, groupRevoked, currentUserPermissionTreeRoot, newUserPermissionTreeRoot, selectedUserPermissionNodes);
    }

    public void rebuildCurrentUserPermissionTreeSelect(org.primefaces.event.NodeSelectEvent event) {
        rebuildCurrentUserPermissionTree();
    }

    public void rebuildCurrentUserPermissionTreeUnselect(org.primefaces.event.NodeUnselectEvent event) {
        rebuildCurrentUserPermissionTree();
    }

    public void rebuildCurrentUserPermissionTree() {
        for (TreeNode userNode : currentUserPermissionTreeRoot.getChildren()) {
            User user = (User) ((TreeNodeModel) userNode.getData()).getTarget();
            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            List<Permission> userPermissions = permissionManager.getPermissions(user);
            userNode.getChildren().clear();
            rebuildCurrentTree(userNode, userPermissions, selectedPermissions, revokables.get(user), replacables.get(user), !isEnabled(Features.FIELD_LEVEL));
        }
    }

    // confirm button for users
    public void confirmPermissions() {
        // confirm groups
        revokeAndGrant(getSelectedGroup(), groupRevoked, groupGranted, false);
        // iterate through users
        for (TreeNode userNode : newUserPermissionTreeRoot.getChildren()) {
            TreeNodeModel userNodeModel = (TreeNodeModel) userNode.getData();
            User user = (User) userNodeModel.getTarget();

            List<Permission> allPermissions = permissionManager.getPermissions(user);
            //List<Permission> userPermissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(0);

            Set<Permission> selectedPermissions = getSelectedPermissions(selectedUserPermissionNodes, userNode);
            // confirm users
            executeRevokeAndGrant(user, allPermissions, selectedPermissions, revokables.get(user), replacables.get(user));

        }
        // reset
        init();
    }

    public TreeNode getResourceRoot() {
        return resourceRoot;
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

    public TreeNode[] getSelectedResourceNodes() {
        return selectedResourceNodes;
    }

    public void setSelectedResourceNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedResourceNodes = selectedPermissionNodes;
    }

    public TreeNode getCurrentUserPermissionTreeRoot() {
        return currentUserPermissionTreeRoot;
    }

    public TreeNode getNewUserPermissionTreeRoot() {
        return newUserPermissionTreeRoot;
    }

    public TreeNode[] getSelectedUserPermissionNodes() {
        return selectedUserPermissionNodes;
    }

    public void setSelectedUserPermissionNodes(TreeNode[] selectedUserPermissionNodes) {
        this.selectedUserPermissionNodes = selectedUserPermissionNodes;
    }

    public TreeNode[] getSelectedGroupPermissionNodes() {
        return selectedGroupPermissionNodes;
    }

    public void setSelectedGroupPermissionNodes(TreeNode[] selectedGroupPermissionNodes) {
        this.selectedGroupPermissionNodes = selectedGroupPermissionNodes;
    }

    public UserGroup getSelectedGroup() {
        return userSession.getSelectedUserGroup();
    }

}
