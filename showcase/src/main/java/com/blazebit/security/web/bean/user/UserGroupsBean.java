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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.deltaspike.core.util.StringUtils;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.web.bean.GroupHandlerBaseBean;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.util.Constants;

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
    private Set<Permission> notRevokableWhenRemovingFromGroup = new HashSet<Permission>();

    private Map<UserGroup, List<Permission>> groupPermissionsMap = new HashMap<UserGroup, List<Permission>>();
    private String groupWizardStep;

    private List<Permission> currentUserDataPermissions;

    private Set<Permission> notGrantableWhenAddingToGroup = new HashSet<Permission>();

    public void init() {
        initUserGroups();
        initUserPermissions();
        initWizard();
    }

    private void initWizard() {
        groupWizardStep = "groups";

    }

    private void initUserPermissions() {
        currentUserPermissions = filterPermissions(permissionManager.getPermissions(getSelectedUser())).get(0);
        currentUserDataPermissions = filterPermissions(permissionManager.getPermissions(getSelectedUser())).get(1);
        this.permissionViewRoot = new DefaultTreeNode("root", null);
        getPermissionTree(permissionManager.getPermissions(getSelectedUser()), permissionViewRoot);

    }

    private void initUserGroups() {
        this.currentUserGroups = userGroupService.getGroupsForUser(getSelectedUser());
        selectedGroupNodes = new TreeNode[] {};
        this.groupRoot = getGroupTree(currentUserGroups, selectedGroupNodes);
    }

    /**
     * wizard step: select groups, show permissions
     */
    public void processSelectedGroups() {
        selectedPermissionNodes = new TreeNode[] {};
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
        // store revokable permissions from all removed groups and parent
        revokableWhenRemovingFromGroup = new HashSet<Permission>();
        revokableWhenRemovingFromGroup = getRevokablePermissions(removeFromGroupsWithParents).get(0);
        notRevokableWhenRemovingFromGroup = getRevokablePermissions(removeFromGroupsWithParents).get(1);

        // get all permissions of the selected groups and their parents
        Set<Permission> grantable = getGrantablePermissions(selectedGroupsWithParents, revokableWhenRemovingFromGroup).get(0);
        notGrantableWhenAddingToGroup = getGrantablePermissions(addToGroups, revokableWhenRemovingFromGroup).get(1);
        // get all revokable permissions
        Set<Permission> revokable = new HashSet<Permission>(revokableWhenRemovingFromGroup);
        revokable.removeAll(grantable);

        // current permission tree
        // mark removable permissions
        Set<Permission> revokeablePermissionsWhenGranting = getRevokablePermissionsWhenGranting(grantable);
        revokable.addAll(revokeablePermissionsWhenGranting);
        List<Permission> all = new ArrayList<Permission>(currentUserPermissions);
        all.addAll(currentUserDataPermissions);
        buildCurrentPermissionTree(all, revokable);

        // new permission tree
        // existing user permissions
        List<Permission> currentPermissions = new ArrayList<Permission>(currentUserPermissions);
        currentPermissions = (List<Permission>) removeAll(currentPermissions, revokeablePermissionsWhenGranting);
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
    private List<Set<Permission>> getGrantablePermissions(Set<UserGroup> selectedGroupsWithParents, Set<Permission> revokable) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        groupPermissionsMap = new HashMap<UserGroup, List<Permission>>();
        Set<Permission> grantable = new HashSet<Permission>();
        Set<Permission> notGrantable = new HashSet<Permission>();
        for (UserGroup selecteGroup : selectedGroupsWithParents) {
            List<Permission> groupPermissions = permissionManager.getPermissions(selecteGroup);
            List<Permission> currentPermissions = permissionManager.getPermissions(getSelectedUser());
            currentPermissions = (List<Permission>) removeAll(currentPermissions, revokable);
            for (Permission permission : groupPermissions) {
                // filter out grantable permissions
                if (permissionDataAccess.isGrantable(currentPermissions, getSelectedUser(), permission.getAction(), permission.getResource())
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
        Set<Permission> redundantPermissions = getRedundantPermissions(grantable);
        grantable.removeAll(redundantPermissions);
        ret.add(grantable);
        ret.add(notGrantable);
        return ret;
    }

    private Set<Permission> getRedundantPermissions(Set<Permission> grantable) {
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        for (Permission permission : grantable) {
            EntityField entityField = (EntityField) permission.getResource();
            if (!entityField.isEmptyField()) {
                if (contains(grantable, permissionFactory.create(permission.getAction(), entityFieldFactory.createResource(entityField.getEntity())))) {
                    redundantPermissions.add(permission);
                }
            }
        }
        return redundantPermissions;
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
    private List<Set<Permission>> getRevokablePermissions(Set<UserGroup> removeFromGroupsWithParents) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> revokable = new HashSet<Permission>();
        //
        Set<Permission> notRevokable = new HashSet<Permission>();
        // collect permissions to be revoked from unselected groups and their parents
        for (UserGroup group : removeFromGroupsWithParents) {
            List<Permission> groupPermissions = permissionManager.getPermissions(group);
            for (Permission permission : groupPermissions) {
                // filter out revokable permissions
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
                    if (!StringUtils.isEmpty(((EntityField) permission.getResource()).getField())) {

                        DefaultTreeNode fieldNode = new DefaultTreeNode(new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
                            permission.getResource(), Marking.NONE), actionNode);
                        if (permission.getResource() instanceof EntityObjectField) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.BLUE);
                            ((TreeNodeModel) fieldNode.getData()).setTooltip(Constants.CONTAINS_OBJECTS);
                        }
                        if (contains(markedPermissions, permission)) {
                            ((TreeNodeModel) fieldNode.getData()).setMarking(Marking.RED);
                        }
                    } else {
                        if (permission.getResource() instanceof EntityObjectField) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.BLUE);
                            ((TreeNodeModel) actionNode.getData()).setTooltip(Constants.CONTAINS_OBJECTS);
                        }
                        // entity without field permission -> dont create node but mark action if permission is marked
                        if (contains(markedPermissions, permission)) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.RED);
                        }

                    }
                }
                selectedPermissionNodes = ArrayUtils.addAll(selectedPermissionNodes, propagateNodePropertiesUpwards(actionNode));
            }
            selectedPermissionNodes = ArrayUtils.addAll(selectedPermissionNodes, propagateNodePropertiesUpwards(entityNode));
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
            DefaultTreeNode entityNode = new DefaultTreeNode(new TreeNodeModel(entity, TreeNodeModel.ResourceType.ENTITY, entityField), newPermissionTreeRoot);
            entityNode.setExpanded(true);
            // group again by action
            Map<Action, List<Permission>> permissionMapByAction = groupPermissionsByAction(permissionGroup);
            for (Action action : permissionMapByAction.keySet()) {
                List<Permission> permissionsByAction = permissionMapByAction.get(action);
                // create action node
                EntityAction entityAction = (EntityAction) action;
                DefaultTreeNode actionNode = new DefaultTreeNode(new TreeNodeModel(entityAction.getActionName(), TreeNodeModel.ResourceType.ACTION, entityAction), entityNode);
                actionNode.setExpanded(true);
                for (Permission permission : permissionsByAction) {

                    // add entity fields if there are any
                    if (!((EntityField) permission.getResource()).isEmptyField()) {
                        TreeNodeModel fieldNodeModel = new TreeNodeModel(((EntityField) permission.getResource()).getField(), TreeNodeModel.ResourceType.FIELD,
                            permission.getResource(), selectedPermissions.contains(permission) ? Marking.GREEN : Marking.NONE);
                        DefaultTreeNode fieldNode = new DefaultTreeNode(fieldNodeModel, actionNode);
                        Set<UserGroup> userGroup = findGroupForPermission(permission);
                        if (!userGroup.isEmpty()) {
                            StringBuilder tooltip = new StringBuilder("Belongs to ");
                            for (UserGroup ug : userGroup) {
                                tooltip.append(ug).append(",");
                            }
                            if (tooltip.lastIndexOf(",") != -1)
                                tooltip.deleteCharAt(tooltip.lastIndexOf(","));
                            fieldNodeModel.setTooltip(tooltip.toString());
                        }
                        // mark and select permission on field level-> will be propagated upwards at the end
                        if (contains(selectedPermissions, permission)) {
                            fieldNode.setSelected(true);
                            fieldNode.setSelectable(isGranted(ActionConstants.GRANT, entityFieldFactory.createResource(UserPermission.class)));
                        } else {
                            if (contains(notSelectedPermissions, permission)) {
                                fieldNode.setSelected(false);
                                fieldNode.setSelectable(isGranted(ActionConstants.REVOKE, entityFieldFactory.createResource(UserPermission.class)));
                            } else {
                                fieldNode.setSelectable(false);
                            }
                        }
                    } else {
                        // mark and select permission on field level
                        Permission entityPermission = permissionFactory.create(entityAction, entityField);
                        Set<UserGroup> userGroupForEntityPermission = findGroupForPermission(entityPermission);
                        StringBuilder tooltip = new StringBuilder("Belongs to ");
                        if (!userGroupForEntityPermission.isEmpty()) {
                            for (UserGroup ug : userGroupForEntityPermission) {
                                tooltip.append(ug.getName()).append(",");
                            }
                        }
                        if (tooltip.lastIndexOf(",") != -1)
                            tooltip.deleteCharAt(tooltip.lastIndexOf(","));
                        TreeNodeModel actionNodeModel = ((TreeNodeModel) actionNode.getData());
                        actionNodeModel.setTooltip(tooltip.toString());

                        if (contains(selectedPermissions, entityPermission, false)) {
                            ((TreeNodeModel) actionNode.getData()).setMarking(Marking.GREEN);
                            actionNode.setSelected(true);
                            actionNode.setSelectable(isGranted(ActionConstants.GRANT, entityField)
                                && isGranted(ActionConstants.GRANT, entityFieldFactory.createResource(UserPermission.class)));
                            if (entityPermission.getResource() instanceof EntityObjectField) {
                                actionNodeModel.setTooltip("Contains object reference");
                            }
                        } else {
                            if (contains(notSelectedPermissions, entityPermission)) {
                                actionNode.setSelected(false);
                                actionNode.setSelectable(isGranted(ActionConstants.REVOKE, entityFieldFactory.createResource(UserPermission.class)));
                            } else {
                                actionNode.setSelectable(false);
                            }
                        }
                    }
                }
                selectedPermissionNodes = ArrayUtils.addAll(selectedPermissionNodes, propagateNodePropertiesUpwards(actionNode));
            }
            selectedPermissionNodes = ArrayUtils.addAll(selectedPermissionNodes, propagateNodePropertiesUpwards(entityNode));
        }
    }

    private Set<UserGroup> findGroupForPermission(Permission permission) {
        Set<UserGroup> ret = new HashSet<UserGroup>();
        for (UserGroup group : groupPermissionsMap.keySet()) {
            if (contains(groupPermissionsMap.get(group), permission)) {
                ret.add(group);
            }
        }
        return ret;
    }

    /**
     * listener for select unselect permissons
     */
    public void rebuildCurrentPermissionTree() {
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes, false);
        List<Permission> currentPermissions = new ArrayList<Permission>(currentUserPermissions);
        currentPermissions.addAll(currentUserDataPermissions);

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
        Set<Permission> selectedPermissions = getSelectedPermissions(selectedPermissionNodes, false);
        for (Permission permission : revokableWhenRemovingFromGroup) {
            if (!contains(selectedPermissions, permission)) {
                permissionService.revoke(userSession.getUser(), userSession.getSelectedUser(), permission.getAction(), permission.getResource());
            }
        }
        for (Permission permission : selectedPermissions) {
            if (permissionDataAccess.findPermission(getSelectedUser(), permission.getAction(), permission.getResource()) == null) {
                permissionService.grant(userSession.getUser(), userSession.getSelectedUser(), permission.getAction(), permission.getResource());
            }
        }

        for (UserGroup group : removeFromGroups) {
            roleService.removeSubjectFromRole(getSelectedUser(), group);
        }
        // clear
        for (UserGroup group : addToGroups) {
            roleService.addSubjectToRole(getSelectedUser(), group);
        }

        init();
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

    @Override
    public DefaultTreeNode getPermissionViewRoot() {
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

    public List<Permission> getNotRevokableWhenRemovingFromGroup() {
        return new ArrayList<Permission>(notRevokableWhenRemovingFromGroup);
    }

    public List<Permission> getNotGrantable() {
        return new ArrayList<Permission>(notGrantableWhenAddingToGroup);
    }

}
