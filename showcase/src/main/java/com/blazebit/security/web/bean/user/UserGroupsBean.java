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
import javax.inject.Inject;

import org.apache.deltaspike.core.util.StringUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionHandlingBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.model.GroupModel;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "userGroupsBean")
public class UserGroupsBean extends PermissionHandlingBaseBean implements PermissionView, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    private RoleService roleService;

    @Inject
    private UserGroupService userGroupService;

    private List<Permission> currentUserPermissions = new ArrayList<Permission>();
    private List<UserGroup> currentUserGroups = new ArrayList<UserGroup>();
    // group tree
    private TreeNode[] selectedGroupNodes;
    private DefaultTreeNode groupRoot;
    // permissio view tree
    private DefaultTreeNode permissionViewRoot;
    // selected group to view group permissions
    private UserGroup selectedGroup;

    // after group selection
    private Set<UserGroup> addToGroups = new HashSet<UserGroup>();
    private Set<UserGroup> removeFromGroups = new HashSet<UserGroup>();

    // compare 2 permission trees
    private DefaultTreeNode currentPermissionTreeRoot;
    private DefaultTreeNode newPermissionTreeRoot;
    // select from the new permission tree
    private TreeNode[] selectedPermissionNodes;

    private Set<Permission> revokableWhenRemovingFromGroup = new HashSet<Permission>();

    public void init() {
        initUserGroups();
        initUserPermissions();
    }

    private void initUserPermissions() {
        currentUserPermissions = permissionManager.getAllPermissions(getSelectedUser());
        this.permissionViewRoot = new DefaultTreeNode("root", null);
        buildPermissionTree(currentUserPermissions, permissionViewRoot);

    }

    private void initUserGroups() {
        // inti groups
        this.currentUserGroups = userGroupService.getGroupsForUser(getSelectedUser());
        // init groups tree
        List<UserGroup> availableGroups = userGroupService.getAllParentGroups();
        this.groupRoot = new DefaultTreeNode();
        selectedGroupNodes = new TreeNode[] {};
        groupRoot.setExpanded(true);
        for (UserGroup group : availableGroups) {
            createNode(group, groupRoot);
        }
    }

    /**
     * helper to build tree
     * 
     * @param group
     * @param node
     */
    private void createNode(UserGroup group, DefaultTreeNode node) {
        DefaultTreeNode childNode = new DefaultTreeNode(new GroupModel(group, false, false), node);
        childNode.setExpanded(true);
        // node is selected if user belongs to group
        boolean selected = roleService.canUserBeRemovedFromRole(getSelectedUser(), group);
        childNode.setSelected(selected);
        if (selected) {
            if (selectedGroupNodes.length == 0) {
                selectedGroupNodes = new TreeNode[1];
                selectedGroupNodes[0] = childNode;
            } else {
                selectedGroupNodes[selectedGroupNodes.length - 1] = childNode;
            }
        }
        // node is selectable if user can be added to group.TODO reevaluate when uncheck, then its useful
        // childNode.setSelectable(roleService.canUserBeAddedToRole(getSelectedUser(), group));
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createNode(child, childNode);
        }
    }

    /**
     * wizard step: select groups, show permissions
     */
    public void processSelectedGroups() {
        selectedPermissionNodes = new TreeNode[] {};
        // collects selected groups with its parents
        Set<UserGroup> selectedGroupsWithParents = new HashSet<UserGroup>();
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode node : selectedGroupNodes) {
            GroupModel selectedGroupModel = (GroupModel) node.getData();
            selectedGroups.add(selectedGroupModel.getUserGroup());
            selectedGroupsWithParents.add(selectedGroupModel.getUserGroup());

            UserGroup parent = selectedGroupModel.getUserGroup().getParent();
            while (parent != null) {
                selectedGroupsWithParents.add(parent);
                parent = parent.getParent();
            }
        }
        // store addToGroup and removeFromGroup sets
        findAddAndRemoveGroups(selectedGroups);

        // permissions from the removed groups' parents have to be revoked too
        Set<UserGroup> removeFromGroupsWithParents = new HashSet<UserGroup>(removeFromGroups);
        for (UserGroup userGroup : removeFromGroups) {
            UserGroup parent = userGroup;
            while (parent != null) {
                removeFromGroupsWithParents.add(parent);
                parent = parent.getParent();
            }
        }
        removeFromGroupsWithParents.removeAll(selectedGroupsWithParents);
        // store revokable permissions from all removed groups and parentx
        revokableWhenRemovingFromGroup = new HashSet<Permission>();
        revokableWhenRemovingFromGroup = getRevokablePermissions(removeFromGroupsWithParents);

        // get all permissions of the selected groups and their parents
        Set<Permission> grantable = getAllGroupPermissions(selectedGroupsWithParents);
        // get all revokable permissions
        Set<Permission> revokable = new HashSet<Permission>(revokableWhenRemovingFromGroup);
        revokable.removeAll(grantable);

        // existing user permissions
        List<Permission> currentPermissions = new ArrayList<Permission>(currentUserPermissions);
        // current permission tree
        // mark removable permissions
        Set<Permission> revokeablePermissionsWhenGranting = getRevokablePermissionsWhenGranting(grantable);
        revokable.addAll(revokeablePermissionsWhenGranting);
        buildCurrentPermissionTree(currentPermissions, revokable);
        // new permission tree
        currentPermissions = removeAll(currentPermissions, revokeablePermissionsWhenGranting);
        currentPermissions.addAll(grantable);
        buildNewPermissionTree(currentPermissions, grantable, revokable);
    }

    /**
     * 
     * @param grantable
     * @return
     */
    private Set<Permission> getRevokablePermissionsWhenGranting(Set<Permission> grantable) {
        Set<Permission> replaceablePermissionWhenGranting = new HashSet<Permission>();
        for (Permission permission : currentUserPermissions) {
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
    private Set<Permission> getAllGroupPermissions(Set<UserGroup> selectedGroupsWithParents) {
        Set<Permission> grantable = new HashSet<Permission>();
        for (UserGroup selecteGroup : selectedGroupsWithParents) {
            List<Permission> groupPermissions = permissionManager.getAllPermissions(selecteGroup);
            for (Permission permission : groupPermissions) {
                // filter out grantable permissions
                if (permissionDataAccess.isGrantable(getSelectedUser(), permission.getAction(), permission.getResource())) {
                    grantable.add(permission);
                }
            }
        }
        // filter out redundant permissions
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        for (Permission permission : grantable) {
            EntityField entityField = (EntityField) permission.getResource();
            if (!entityField.isEmptyField()) {
                if (contains(grantable, permissionFactory.create(getSelectedUser(), permission.getAction(), entityFieldFactory.createResource(entityField.getEntity())))) {
                    redundantPermissions.add(permission);
                }
            }
        }
        grantable.removeAll(redundantPermissions);
        return grantable;
    }

    /**
     * 
     * @param selectedGroups
     */
    private void findAddAndRemoveGroups(Set<UserGroup> selectedGroups) {
        removeFromGroups = new HashSet<UserGroup>(currentUserGroups);
        removeFromGroups.removeAll(selectedGroups);

        addToGroups = new HashSet<UserGroup>();
        List<UserGroup> newCurrentGroups = new ArrayList<UserGroup>(currentUserGroups);
        newCurrentGroups.removeAll(removeFromGroups);
        for (UserGroup group : selectedGroups) {
            if (canUserBeAddedToRole(newCurrentGroups, group)) {
                addToGroups.add(group);
            }
        }
    }

    /**
     * 
     * @param removeFromGroupsWithParents
     * @return
     */
    private Set<Permission> getRevokablePermissions(Set<UserGroup> removeFromGroupsWithParents) {
        Set<Permission> revokable = new HashSet<Permission>();
        // collect permissions to be revoked from unselected groups and their parents
        for (UserGroup group : removeFromGroupsWithParents) {
            List<Permission> groupPermissions = permissionManager.getAllPermissions(group);
            for (Permission permission : groupPermissions) {
                // filter out revokable permissions
                if (permissionDataAccess.isRevokable(getSelectedUser(), permission.getAction(), permission.getResource())) {
                    revokable.add(permission);
                }
            }
        }
        return revokable;
    }

    /**
     * 
     * @param groups
     * @param group
     * @return
     */
    private boolean canUserBeAddedToRole(List<UserGroup> groups, UserGroup group) {
        for (UserGroup currentGroup : groups) {
            // subject cannot be added to the same role where he already belongs
            if (currentGroup.equals(group)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * @param permissions
     * @param markedPermissions
     */
    private void buildCurrentPermissionTree(List<Permission> permissions, Set<Permission> markedPermissions) {
        currentPermissionTreeRoot = new DefaultTreeNode();
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {

            List<Permission> permissionsByEntity = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), currentPermissionTreeRoot);
            entityNode.setExpanded(true);
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionsByEntity);
            for (Action action : permissionMapByAction.keySet()) {
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction, false), entityNode);
                actionNode.setExpanded(true);
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    // entity with field-> create node
                    if (!StringUtils.isEmpty(permission.getResource().getField())) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD, permission.getResource(),
                            contains(markedPermissions, permission)), actionNode);
                    } else {
                        // entity without field permission -> dont create node but mark action if permission is marked
                        ((NodeModel) actionNode.getData()).setMarked(contains(markedPermissions, permission));
                    }
                }
                markAndSelectParents(actionNode);
            }
            markAndSelectParents(entityNode);
        }
    }

    /**
     * 
     * @param permissions
     * @param selectedPermissions
     * @param notSelectedPermissions
     */
    private void buildNewPermissionTree(List<Permission> permissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions) {
        newPermissionTreeRoot = new DefaultTreeNode();
        // group permissions by entity
        Map<String, List<Permission>> permissionMapByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionMapByEntity.keySet()) {
            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMapByEntity.get(entity));
            // create entity node
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), newPermissionTreeRoot);
            entityNode.setExpanded(true);
            // group again by action
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionGroup);
            for (Action action : permissionMapByAction.keySet()) {
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                // create action node
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(entityAction.getActionName(), NodeModel.ResourceType.ACTION, entityAction, false), entityNode);
                actionNode.setExpanded(true);
                for (Permission _permission : permissionsByAction) {
                    AbstractPermission permission = (AbstractPermission) _permission;
                    // add entity fields if there are any
                    if (!permission.getResource().isEmptyField()) {
                        DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD, permission.getResource(),
                            selectedPermissions.contains(permission)), actionNode);
                        // mark and select permission on field level-> will be propagated upwards at the end
                        if (contains(selectedPermissions, permission)) {
                            fieldNode.setSelected(true);
                            addToSelectedPermissionNodes(fieldNode);
                        } else {
                            if (contains(notSelectedPermissions, permission)) {
                                fieldNode.setSelected(false);
                            } else {
                                fieldNode.setSelectable(false);
                            }
                        }
                    } else {
                        // mark and select permission on field level-> will be propagated upwards at the end
                        Permission entityPermission = permissionFactory.create(getSelectedUser(), entityAction, entityField);
                        if (contains(selectedPermissions, entityPermission)) {
                            ((NodeModel) actionNode.getData()).setMarked(true);
                            actionNode.setSelected(true);
                            addToSelectedPermissionNodes(actionNode);
                        } else {
                            if (contains(notSelectedPermissions, entityPermission)) {
                                actionNode.setSelected(false);
                            } else {
                                actionNode.setSelectable(false);
                            }
                        }
                    }
                }
                markAndSelectParents(actionNode);
            }
            markAndSelectParents(entityNode);
        }
    }

    /**
     * helper to mark parent nodes when child nodes are marked
     * 
     * @param node
     */
    private void markAndSelectParents(DefaultTreeNode node) {
        if (node.getChildCount() > 0) {
            boolean foundOneMarked = false;
            boolean foundOneUnSelected = false;
            boolean foundOneSelectable = false;
            for (TreeNode child : node.getChildren()) {
                if (child.isSelectable()) {
                    foundOneSelectable = true;
                }
                if (!child.isSelected()) {
                    foundOneUnSelected = true;
                }
                if (((NodeModel) child.getData()).isMarked()) {
                    foundOneMarked = true;
                }
            }
            node.setSelectable(foundOneSelectable);
            ((NodeModel) node.getData()).setMarked(foundOneMarked);
            if (!foundOneUnSelected) {
                addToSelectedPermissionNodes(node);
                node.setSelected(true);
            }
        }
    }

    /**
     * helper to mark node as selected
     * 
     * @param node
     */
    private void addToSelectedPermissionNodes(DefaultTreeNode node) {
        if (selectedPermissionNodes.length == 0) {
            selectedPermissionNodes = new TreeNode[1];
            selectedPermissionNodes[0] = node;
        }
        selectedPermissionNodes[selectedPermissionNodes.length - 1] = node;
    }

    /**
     * listener for select unselect permissons
     */
    public void rebuildCurrentPermissionTree() {
        Set<Permission> selectedPermissions = processSelectedPermissions(selectedPermissionNodes);
        List<Permission> currentPermissions = new ArrayList<Permission>(currentUserPermissions);
        Set<Permission> revokablePermissions = new HashSet<Permission>();

        for (Permission selectedPermission : selectedPermissions) {
            revokablePermissions.addAll(permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), selectedPermission.getAction(),
                                                                                                 selectedPermission.getResource()));
        }

        for (Permission permission : revokableWhenRemovingFromGroup) {
            if (!contains(selectedPermissions, permission)) {
                revokablePermissions.add(permission);
            }
        }
        currentPermissionTreeRoot = new DefaultTreeNode();
        buildCurrentPermissionTree(currentPermissions, revokablePermissions);

    }

    /**
     * confirm button
     */
    public void confirm() {
        Set<Permission> selectedPermissions = processSelectedPermissions(selectedPermissionNodes);
        for (Permission permission : selectedPermissions) {
            if (permissionDataAccess.findPermission(getSelectedUser(), permission.getAction(), permission.getResource()) == null) {
                permissionService.grant(userSession.getUser(), userSession.getSelectedUser(), permission.getAction(), permission.getResource());
            }
        }
        for (Permission permission : revokableWhenRemovingFromGroup) {
            if (!contains(selectedPermissions, permission)) {
                permissionService.revoke(userSession.getUser(), userSession.getSelectedUser(), permission.getAction(), permission.getResource());
            }
        }
        for (UserGroup group : removeFromGroups) {
            roleService.removeSubjectFromRole(getSelectedUser(), group);
        }
        // clear
        selectedGroupNodes = new TreeNode[] {};
        for (UserGroup group : addToGroups) {
            roleService.addSubjectToRole(getSelectedUser(), group);
        }

        initUserGroups();
        initUserPermissions();

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
        return event.getNewStep();
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

    @Override
    public DefaultTreeNode getPermissionViewRoot() {
        return permissionViewRoot;
    }

    public User getSelectedUser() {
        return userSession.getSelectedUser();
    }

    public UserGroup getSelectedGroup() {
        return selectedGroup;
    }

    public DefaultTreeNode getCurrentPermissionTreeRoot() {
        return currentPermissionTreeRoot;
    }

    public DefaultTreeNode getNewPermissionTreeRoot() {
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

    public List<Permission> getSelectedGroupPermissions() {
        if (selectedGroup != null) {
            return permissionManager.getAllPermissions(selectedGroup);
        } else {
            return new ArrayList<Permission>();
        }
    }
}
