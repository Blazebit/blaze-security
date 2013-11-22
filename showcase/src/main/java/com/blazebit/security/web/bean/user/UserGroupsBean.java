/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.GroupHandlerBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.api.UserGroupService;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "userGroupsBean")
public class UserGroupsBean extends GroupHandlerBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    private RoleService roleService;

    @Inject
    private UserGroupService userGroupService;

    private List<Permission> userPermissions = new ArrayList<Permission>();
    private List<Permission> userDataPermissions = new ArrayList<Permission>();
    private List<Permission> allPermissions = new ArrayList<Permission>();

    private List<UserGroup> currentUserGroups = new ArrayList<UserGroup>();
    // group tree
    private TreeNode[] selectedGroupNodes = new TreeNode[] {};
    private DefaultTreeNode groupRoot = new DefaultTreeNode();
    // permissio view tree
    private TreeNode permissionViewRoot = new DefaultTreeNode();
    // selected group to view group permissions
    private UserGroup selectedGroup;

    // after group selection
    private Set<UserGroup> addedGroups = new HashSet<UserGroup>();
    private Set<UserGroup> removedGroups = new HashSet<UserGroup>();

    // compare 2 permission trees
    private TreeNode currentPermissionTreeRoot;
    private TreeNode newPermissionTreeRoot;
    // select from the new permission tree
    private TreeNode[] selectedPermissionNodes = new TreeNode[] {};

    private Set<Permission> revokableWhenRemovingFromGroup = new HashSet<Permission>();

    private Map<UserGroup, List<Permission>> groupPermissionsMap = new HashMap<UserGroup, List<Permission>>();
    private String groupWizardStep;

    public void init() {
        initUserGroups();
        initUserPermissions();
    }

    private void initUserPermissions() {
        allPermissions = permissionManager.getPermissions(getSelectedUser());
        List<List<Permission>> permissions = permissionHandlingUtils.filterPermissions(allPermissions);
        userPermissions = permissions.get(0);
        userDataPermissions = permissions.get(1);
        this.permissionViewRoot = getPermissionTree(userPermissions, userDataPermissions);
    }

    private void initUserGroups() {
        this.currentUserGroups = userGroupService.getGroupsForUser(getSelectedUser());
        selectedGroupNodes = new TreeNode[] {};
        List<UserGroup> parentGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
        this.groupRoot = getGroupTree(parentGroups, currentUserGroups);
    }

    /**
     * TODO nicer wizard step: select groups, show permissions
     */
    public void processSelectedGroups() {
        // collects selected groups with their parents
        Set<UserGroup> selectedGroupsWithParents = new HashSet<UserGroup>();
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode node : selectedGroupNodes) {
            UserGroupModel selectedGroupModel = (UserGroupModel) node.getData();
            selectedGroups.add(selectedGroupModel.getUserGroup());
            selectedGroupsWithParents.add(selectedGroupModel.getUserGroup());

            UserGroup parent = selectedGroupModel.getUserGroup().getParent();
            while (parent != null) {
                selectedGroupsWithParents.add(parent);
                parent = parent.getParent();
            }
        }
        // store addToGroup and removeFromGroup sets
        removedGroups = getRemovedGroups(selectedGroups);
        addedGroups = getAddedGroups(selectedGroups);

        // permissions from the removed groups' parents have to be revoked
        Set<UserGroup> removeFromGroupsWithParents = getRemovedGroupsWithParents(removedGroups);
        // the permissions of the parents that will be added now dont have to be revoked
        removeFromGroupsWithParents.removeAll(selectedGroupsWithParents);
        // store revokable permissions from all removed groups and parents
        List<Set<Permission>> permissionsToRevoke = getPermissionsToRevoke(removeFromGroupsWithParents);
        revokableWhenRemovingFromGroup = permissionsToRevoke.get(0);
        super.setNotRevoked(permissionsToRevoke.get(1));

        // get all permissions of the selected groups and their parents
        Set<Permission> grantable = getGrantablePermissions(selectedGroupsWithParents, revokableWhenRemovingFromGroup).get(0);
        super.setNotGranted(getGrantablePermissions(addedGroups, revokableWhenRemovingFromGroup).get(1));
        // get all revokable permissions
        Set<Permission> revokable = new HashSet<Permission>(revokableWhenRemovingFromGroup);
        revokable.removeAll(grantable);

        // current permission tree
        Set<Permission> revokeablePermissionsWhenGranting = getRevokablePermissionsWhenGranting(grantable);
        revokable.addAll(revokeablePermissionsWhenGranting);
        currentPermissionTreeRoot = getPermissionTree(userPermissions, userDataPermissions, revokable, Marking.REMOVED);

        // new permission tree
        List<Permission> currentPermissions = new ArrayList<Permission>(userPermissions);
        currentPermissions = new ArrayList<Permission>(permissionHandlingUtils.removeAll(currentPermissions, revokeablePermissionsWhenGranting));
        currentPermissions.addAll(grantable);

        if (userSession.getSelectedCompany().isUserLevelEnabled()) {
            newPermissionTreeRoot = getSelectablePermissionTree(currentPermissions, new ArrayList<Permission>(), grantable, revokable, Marking.NEW, Marking.REMOVED);
        } else {
            currentPermissions = (List<Permission>) permissionHandlingUtils.removeAll(currentPermissions, revokable);
            newPermissionTreeRoot = getPermissionTree(currentPermissions, userDataPermissions, grantable, Marking.NEW);
        }
    }

    /**
     * 
     * @param grantable
     * @return
     */
    private Set<Permission> getRevokablePermissionsWhenGranting(Set<Permission> grantable) {
        Set<Permission> replaceablePermissionWhenGranting = new HashSet<Permission>();
        for (Permission permission : userPermissions) {
            for (Permission groupPermission : grantable) {
                Set<Permission> toRevoke = permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), groupPermission.getAction(), groupPermission.getResource());
                if (toRevoke.contains(permission)) {
                    replaceablePermissionWhenGranting.add(permission);
                }
            }
        }
        return replaceablePermissionWhenGranting;
    }

    /**
     * 
     * @param selectedGroupsWithParents
     * @return
     */
    private List<Set<Permission>> getGrantablePermissions(Set<UserGroup> selectedGroupsWithParents, Set<Permission> revokable) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        groupPermissionsMap = new HashMap<UserGroup, List<Permission>>();
        Set<Permission> grantable = new HashSet<Permission>();
        Set<Permission> notGrantable = new HashSet<Permission>();
        for (UserGroup selecteGroup : selectedGroupsWithParents) {
            List<Permission> groupPermissions = permissionManager.getPermissions(selecteGroup);
            List<Permission> currentPermissions = permissionManager.getPermissions(getSelectedUser());
            currentPermissions = new ArrayList(permissionHandlingUtils.removeAll(currentPermissions, revokable));
            for (Permission permission : groupPermissions) {
                // filter out grantable permissions
                if (permissionDataAccess.isGrantable(currentPermissions, permission.getAction(), permission.getResource())
                    && isGranted(ActionConstants.GRANT, permission.getResource())) {
                    grantable.add(permission);
                    List<Permission> temp;
                    if (groupPermissionsMap.containsKey(selecteGroup)) {
                        temp = groupPermissionsMap.get(selecteGroup);
                    } else {
                        temp = new ArrayList<Permission>();
                    }
                    temp.add(permission);
                    groupPermissionsMap.put(selecteGroup, temp);
                } else {
                    notGrantable.add(permission);
                }
            }
        }
        // filter out redundant permissions
        grantable = permissionHandlingUtils.getNormalizedPermissions(grantable);

        ret.add(grantable);
        ret.add(notGrantable);
        return ret;
    }

    /**
     * 
     * @param removeFromGroupsWithParents
     * @return
     */
    private List<Set<Permission>> getPermissionsToRevoke(Set<UserGroup> removeFromGroups) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> revokable = new HashSet<Permission>();
        Set<Permission> notRevokable = new HashSet<Permission>();

        for (UserGroup group : removeFromGroups) {
            List<Permission> groupPermissions = permissionManager.getPermissions(group);
            for (Permission permission : groupPermissions) {

                if (permissionDataAccess.isRevokable(getSelectedUser(), permission.getAction(), permission.getResource())
                    && isGranted(ActionConstants.REVOKE, permission.getResource())) {
                    revokable.add(permission);
                } else {
                    notRevokable.add(permission);
                }
            }
        }
        ret.add(revokable);
        ret.add(notRevokable);
        return ret;
    }

    /**
     * listener for select unselect permissons in the new permission tree
     */
    public void rebuildCurrentPermissionTree() {
        // TODOD
    }

    /**
     * confirm button
     */
    public void confirm() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes);

        Set<Permission> revoked = permissionHandlingUtils.getRevokable(userPermissions, selectedPermissions).get(0);
        Set<Permission> granted = permissionHandlingUtils.getGrantable(permissionHandlingUtils.removeAll(userPermissions, revoked), selectedPermissions).get(0);

        performRevokeAndGrant(getSelectedUser(), allPermissions, revoked, granted);

        for (UserGroup group : removedGroups) {
            roleService.removeSubjectFromRole(getSelectedUser(), group);
        }
        for (UserGroup group : addedGroups) {
            roleService.addSubjectToRole(getSelectedUser(), group);
        }
        init();
    }

    /**
     * green(newly selected) groups
     * 
     * @param selectedGroups
     */
    private Set<UserGroup> getAddedGroups(Set<UserGroup> selectedGroups) {
        Set<UserGroup> ret = new HashSet<UserGroup>();
        // Set<UserGroup> current = new HashSet<UserGroup>(currentUserGroups);
        // current.removeAll(removeFromGroups);
        for (UserGroup group : selectedGroups) {
            if (!currentUserGroups.contains(group)) {
                ret.add(group);
            }
        }
        return ret;
    }

    /**
     * red (just removed) groups with their parents
     * 
     * @param removeFromGroups
     * @return
     */
    private Set<UserGroup> getRemovedGroupsWithParents(Set<UserGroup> removeFromGroups) {
        Set<UserGroup> removeFromGroupsWithParents = new HashSet<UserGroup>(removeFromGroups);
        for (UserGroup userGroup : removeFromGroups) {
            UserGroup parent = userGroup;
            while (parent != null) {
                removeFromGroupsWithParents.add(parent);
                parent = parent.getParent();
            }
        }
        return removeFromGroupsWithParents;
    }

    /**
     * red (just removed) groups
     * 
     * @param selectedGroups
     * @return
     */
    private Set<UserGroup> getRemovedGroups(Set<UserGroup> selectedGroups) {
        Set<UserGroup> ret = new HashSet<UserGroup>(currentUserGroups);
        ret.removeAll(selectedGroups);
        return ret;
    }

    /**
     * wizard listener
     * 
     * @param event
     * @return
     */
    public String groupWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("groups")) {
            processSelectedGroups();
        }
        if (event.getOldStep().equals("groupPermissions") && event.getNewStep().equals("groups")) {
            // reset
        }
        return event.getNewStep();
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        return permissionViewRoot;
    }

    public TreeNode[] getSelectedGroupNodes() {
        return selectedGroupNodes;
    }

    public void setSelectedGroupNodes(TreeNode[] selectedGroupNodes) {
        this.selectedGroupNodes = selectedGroupNodes;
    }

    public DefaultTreeNode getGroupRoot() {
        return groupRoot;
    }

    public List<UserGroup> getUserGroups() {
        return currentUserGroups;
    }

    public User getSelectedUser() {
        return userSession.getSelectedUser();
    }

    public UserGroup getSelectedGroup() {
        return selectedGroup;
    }

    public TreeNode getCurrentPermissionTreeRoot() {
        return currentPermissionTreeRoot;
    }

    public TreeNode getNewPermissionTreeRoot() {
        return newPermissionTreeRoot;
    }

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissions) {
        this.selectedPermissionNodes = selectedPermissions;
    }

    public void setSelectedGroup(UserGroup selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    // dialog
    public List<Permission> getSelectedGroupPermissions() {
        if (selectedGroup != null) {
            return permissionManager.getPermissions(selectedGroup);
        } else {
            return new ArrayList<Permission>();
        }
    }

    public String getGroupWizardStep() {
        return groupWizardStep;
    }

    public void setGroupWizardStep(String groupWizardStep) {
        this.groupWizardStep = groupWizardStep;
    }

}
