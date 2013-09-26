/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.web.bean.PermissionView;
import com.blazebit.security.web.bean.PermissionViewUtils;
import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.bean.model.GroupModel;
import com.blazebit.security.web.bean.model.PermissionModel;
import com.blazebit.security.web.bean.model.ResourceAction;
import com.blazebit.security.web.bean.model.ResourceModel;
import com.blazebit.security.web.bean.model.ResourceModel.ResourceType;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "userGroupsBean")
public class UserGroupsBean extends PermissionViewUtils implements PermissionView, Serializable {

    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private RoleService roleService;
    @Inject
    private PermissionService permissionService;
    @Inject
    private UserSession userSession;
    @Inject
    private PermissionDataAccess permissionDataAccess;
    @Inject
    private PermissionManager permissionManager;
    @Inject
    private EntityFieldFactory entityFieldFactory;
    private List<PermissionModel> userPermissions = new ArrayList<PermissionModel>();
    private List<UserGroup> userGroups = new ArrayList<UserGroup>();
    private TreeNode[] selectedGroupNodes;
    private DefaultTreeNode groupRoot;

    private DefaultTreeNode permissionRoot;
    private DefaultTreeNode permissionViewRoot;

    private UserGroup selectedGroup;

    private boolean permissionTreeView;
    private Set<UserGroup> addToGroups = new HashSet<UserGroup>();
    private Set<UserGroup> removeFromGroups = new HashSet<UserGroup>();
    private DefaultTreeNode currentPermissionTreeRoot;
    private DefaultTreeNode newPermissionTreeRoot;
    private TreeNode[] selectedPermissionNodes;
    private Set<ResourceAction> selectedResourceActions = new HashSet<ResourceAction>();
    private List<Permission> permissions = new ArrayList<Permission>();

    public void init() {
        if (getSelectedUser() != null) {
            // init groups
            initUserGroups();
            // init permissions
            initUserPermissions();
        }
    }

    private void initUserPermissions() {
        this.userPermissions.clear();
        permissions = permissionManager.getAllPermissions(getSelectedUser());
        for (Permission p : permissions) {
            this.userPermissions.add(new PermissionModel(p, false));
        }

        this.permissionViewRoot = new DefaultTreeNode("root", null);
        buildPermissionTree(permissions, permissionViewRoot);
        this.permissionTreeView = true;
    }

    private void initUserGroups() {
        // inti groups
        this.userGroups = userGroupService.getGroupsForUser(getSelectedUser());
        // init groups tree
        List<UserGroup> availableGroups = userGroupService.getAllParentGroups();
        this.groupRoot = new DefaultTreeNode("", null);
        selectedGroupNodes = new TreeNode[1];
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
        childNode.setSelected(roleService.canUserBeRemovedFromRole(getSelectedUser(), group));
        selectedGroupNodes[selectedGroupNodes.length - 1] = childNode;
        // node is selectable if user can be added to group
        // childNode.setSelectable(roleService.canUserBeAddedToRole(getSelectedUser(), group));
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createNode(child, childNode);
        }
    }

    private Set<Permission> revokable = new HashSet<Permission>();

    /**
     * 1 step
     */
    public void processSelectedGroups() {

        addToGroups.clear();
        removeFromGroups.clear();

        // build next tree

        permissionRoot = new DefaultTreeNode("root", null);
        newPermissionTreeRoot = new DefaultTreeNode("root", null);
        currentPermissionTreeRoot = new DefaultTreeNode("root", null);

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
            if (roleService.canUserBeAddedToRole(getSelectedUser(), selectedGroupModel.getUserGroup())) {
                addToGroups.add(selectedGroupModel.getUserGroup());
            }
        }
        // remove groups
        removeFromGroups = new HashSet<UserGroup>(userGroupService.getGroupsForUser(getSelectedUser()));
        removeFromGroups.removeAll(selectedGroups);
        Set<UserGroup> removeFromGroupsWithParents = new HashSet<UserGroup>(removeFromGroups);
        for (UserGroup userGroup : removeFromGroups) {
            UserGroup parent = userGroup;
            while (parent != null) {
                removeFromGroupsWithParents.add(parent);
                parent = parent.getParent();
            }
        }
        removeFromGroupsWithParents.removeAll(selectedGroupsWithParents);

        for (UserGroup group : removeFromGroupsWithParents) {
            List<Permission> groupPermissions = permissionManager.getAllPermissions(group);
            for (Permission permission : groupPermissions) {
                // filter out revokable permissions
                if (permissionDataAccess.isRevokable(getSelectedUser(), permission.getAction(), permission.getResource())) {
                    revokable.add(permission);
                }
            }
        }

        // get all permissions of the groups and their parents
        Set<Permission> grantable = new HashSet<Permission>();
        Set<Permission> newGroupPermisions = new HashSet<Permission>();
        for (UserGroup selecteGroup : selectedGroupsWithParents) {
            List<Permission> groupPermissions = permissionManager.getAllPermissions(selecteGroup);
            for (Permission permission : groupPermissions) {
                // filter out grantable permissions
                if (permissionDataAccess.isGrantable(getSelectedUser(), permission.getAction(), permission.getResource())) {
                    grantable.add(permission);
                }
            }
        }
        revokable.removeAll(grantable);

        // existing user permissions
        List<Permission> currentPermissions = permissions;
        // mark removable permissions
        Set<Permission> removablePermissions = new HashSet<Permission>();
        for (Permission permission : currentPermissions) {
            for (Permission groupPermission : grantable) {
                Set<Permission> toRevoke = permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), groupPermission.getAction(), groupPermission.getResource());
                if (toRevoke.contains(permission)) {
                    removablePermissions.add(permission);
                }
            }
        }
        // current permission tree
        buildCurrentPermissionTree(currentPermissions, removablePermissions, currentPermissionTreeRoot);

        // new permission tree
        currentPermissions.removeAll(removablePermissions);
        currentPermissions.addAll(grantable);
        selectedPermissionNodes = new TreeNode[1];
        buildNewPermissionTree(currentPermissions, grantable, revokable, newPermissionTreeRoot, selectedPermissionNodes);
    }

    protected void buildCurrentPermissionTree(List<Permission> permissions, Set<Permission> markedPermissions, TreeNode permissionRoot) {

        Map<String, List<Permission>> permissionMap = groupPermissionsByEntity(permissions);

        for (String entity : permissionMap.keySet()) {

            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMap.get(entity));
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new ResourceModel(entity, ResourceModel.ResourceType.ENTITY, entityField), permissionRoot);
            entityNode.setExpanded(true);

            boolean markedAtLeastOne = false;
            for (Permission _permission : permissionGroup) {
                AbstractPermission permission = (AbstractPermission) _permission;

                if (!StringUtils.isEmpty(permission.getResource().getField())) {
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new ResourceModel(permission.getResource().getField(), ResourceModel.ResourceType.FIELD,
                        permission.getResource(), markedPermissions.contains(permission)), entityNode);
                    fieldNode.setExpanded(true);

                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(permission.getAction().getActionName(), ResourceModel.ResourceType.ACTION,
                        permission.getAction(), markedPermissions.contains(permission)), fieldNode);

                } else {
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(permission.getAction().getActionName(), ResourceModel.ResourceType.ACTION,
                        permission.getAction(), markedPermissions.contains(permission)), entityNode);

                }
            }

            if (markedAtLeastOne) {
                ((ResourceModel) entityNode.getData()).setMarked(markedAtLeastOne);
            }
        }
    }

    /**
     * 
     * @param permissions
     * @param selectedPermissions
     * @param notSelectedPermissions
     * @param permissionRoot
     * @param selectedPermissionNodes
     */
    protected void buildNewPermissionTree(List<Permission> permissions, Set<Permission> selectedPermissions, Set<Permission> notSelectedPermissions, TreeNode permissionRoot, TreeNode[] selectedPermissionNodes) {

        Map<String, List<Permission>> permissionMap = groupPermissionsByEntity(permissions);

        for (String entity : permissionMap.keySet()) {

            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMap.get(entity));
            EntityField entityField = (EntityField) entityFieldFactory.createResource(entity);
            DefaultTreeNode entityNode = new DefaultTreeNode(new ResourceModel(entity, ResourceModel.ResourceType.ENTITY, entityField), permissionRoot);
            entityNode.setExpanded(true);

            boolean markedAtLeastOne = false;
            for (Permission _permission : permissionGroup) {
                AbstractPermission permission = (AbstractPermission) _permission;

                if (!StringUtils.isEmpty(permission.getResource().getField())) {
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new ResourceModel(permission.getResource().getField(), ResourceModel.ResourceType.FIELD,
                        permission.getResource(), selectedPermissions.contains(permission)), entityNode);
                    fieldNode.setExpanded(true);
                    if (contains(selectedPermissions, permission)) {
                        fieldNode.setSelected(true);
                        selectedPermissionNodes[selectedPermissionNodes.length - 1] = fieldNode;
                        markedAtLeastOne = true;
                    } else {
                        if (contains(notSelectedPermissions, permission)) {
                            fieldNode.setSelected(false);
                        } else {
                            fieldNode.setSelectable(false);
                        }
                    }
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(permission.getAction().getActionName(), ResourceModel.ResourceType.ACTION,
                        permission.getAction(), selectedPermissions.contains(permission)), fieldNode);
                    if (contains(selectedPermissions, permission)) {
                        actionNode.setSelected(true);
                        selectedPermissionNodes[selectedPermissionNodes.length - 1] = actionNode;
                        markedAtLeastOne = true;
                    } else {
                        if (contains(notSelectedPermissions, permission)) {
                            actionNode.setSelected(false);
                        } else {
                            actionNode.setSelectable(false);
                        }
                    }
                } else {
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(permission.getAction().getActionName(), ResourceModel.ResourceType.ACTION,
                        permission.getAction(), selectedPermissions.contains(permission)), entityNode);
                    if (contains(selectedPermissions, permission)) {
                        actionNode.setSelected(true);
                        selectedPermissionNodes[selectedPermissionNodes.length - 1] = actionNode;
                        markedAtLeastOne = true;
                    } else {
                        if (contains(notSelectedPermissions, permission)) {
                            actionNode.setSelected(false);
                        } else {
                            actionNode.setSelectable(false);
                        }
                    }
                }
            }

            if (markedAtLeastOne) {
                entityNode.setSelected(true);
                selectedPermissionNodes[selectedPermissionNodes.length - 1] = entityNode;
                ((ResourceModel) entityNode.getData()).setMarked(markedAtLeastOne);
            } else {
                // entityNode.setSelectable(false);
                boolean found = false;
                for (TreeNode children : entityNode.getChildren()) {
                    if (children.isSelectable()) {
                        found = true;
                        break;
                    }
                }
                entityNode.setSelectable(found);
            }

        }
    }

    private boolean contains(Set<Permission> permissions, AbstractPermission permission) {
        for (Permission p : permissions) {
            AbstractPermission givenPermission = (AbstractPermission) p;
            if (givenPermission.getAction().equals(permission.getAction()) && givenPermission.getResource().equals(permission.getResource())) {
                return true;
            }
        }
        return false;
    }

    public void rebuildCurrentPermissionTree() {
        List<Permission> currentPermissions = permissions;
        processSelectedPermissions();
        Set<Permission> removablePermissions = new HashSet<Permission>();
        for (Permission permission : currentPermissions) {
            for (ResourceAction resourceAction : selectedResourceActions) {
                Set<Permission> toRevoke = permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), resourceAction.getAction(), resourceAction.getResource());
                if (toRevoke.contains(permission)) {
                    removablePermissions.add(permission);
                }
            }
        }
        currentPermissionTreeRoot = new DefaultTreeNode();
        buildCurrentPermissionTree(currentPermissions, removablePermissions, currentPermissionTreeRoot);

    }

    public void confirm() {
        processSelectedPermissions();
        for (ResourceAction resourceAction : selectedResourceActions) {
            if (permissionDataAccess.findPermission(getSelectedUser(), resourceAction.getAction(), resourceAction.getResource()) == null) {
                permissionService.grant(userSession.getUser(), userSession.getSelectedUser(), resourceAction.getAction(), resourceAction.getResource());
            }
        }
        for (Permission permission : revokable) {
            if (!selectedResourceActions.contains(new ResourceAction((EntityField) permission.getResource(), (EntityAction) permission.getAction()))) {
                permissionService.revoke(userSession.getUser(), userSession.getSelectedUser(), permission.getAction(), permission.getResource());
            }
        }

        // clear
        selectedResourceActions.clear();
        selectedGroupNodes = new TreeNode[0];
        selectedPermissionNodes = new TreeNode[0];
        newPermissionTreeRoot = new DefaultTreeNode();
        currentPermissionTreeRoot = new DefaultTreeNode();
        revokable.clear();
        for (UserGroup group : addToGroups) {
            roleService.addSubjectToRole(getSelectedUser(), group);
        }
        for (UserGroup group : removeFromGroups) {
            roleService.removeSubjectFromRole(getSelectedUser(), group);
        }
        initUserGroups();
        initUserPermissions();

    }

    private void processSelectedPermissions() {
        selectedResourceActions.clear();
        if (selectedPermissionNodes != null) {
            for (TreeNode permissionNode : selectedPermissionNodes) {
                if (permissionNode != null && permissionNode.isSelectable()) {
                    selectedResourceActions.addAll(processPermissionNode(permissionNode));
                }
            }
        }
    }

    private Set<ResourceAction> processPermissionNode(TreeNode permissionNode) {
        Set<ResourceAction> ret = new HashSet<ResourceAction>();
        ResourceModel permissionNodeData = (ResourceModel) permissionNode.getData();
        switch (permissionNodeData.getType()) {
            case ENTITY:// skip
                // for (TreeNode childNode : permissionNode.getChildren()) {
                // if (childNode.isSelectable()) {
                // ResourceModel childNodeModel = (ResourceModel) childNode.getData();
                // switch (childNodeModel.getType()) {
                // case FIELD:
                // for (TreeNode fieldNode : childNode.getChildren()) {
                // if (fieldNode.isSelectable()) {
                // ResourceModel fieldNodeModel = (ResourceModel) fieldNode.getData();
                // ret.add(new ResourceAction((EntityField) childNodeModel.getTarget(), (EntityAction)
                // fieldNodeModel.getTarget()));
                // }
                // }
                // break;
                // case ACTION:
                //
                // ResourceModel actionNodeModel = (ResourceModel) childNode.getData();
                // ret.add(new ResourceAction((EntityField) permissionNodeData.getTarget(), (EntityAction)
                // actionNodeModel.getTarget()));
                // break;
                // }
                // }
                // }
                break;
            case FIELD:
                EntityField resource = (EntityField) permissionNodeData.getTarget();
                for (TreeNode fieldNode : permissionNode.getChildren()) {
                    if (fieldNode.isSelectable()) {
                        ResourceModel fieldNodeModel = (ResourceModel) fieldNode.getData();
                        ret.add(new ResourceAction(resource, (EntityAction) fieldNodeModel.getTarget()));
                    }
                }
                break;
            case ACTION:
                TreeNode parentNode = permissionNode.getParent();
                ResourceModel parentPermissionNodeData = (ResourceModel) parentNode.getData();
                switch (parentPermissionNodeData.getType()) {
                    case ENTITY:
                    case FIELD:
                        ret.add(new ResourceAction((EntityField) parentPermissionNodeData.getTarget(), (EntityAction) permissionNodeData.getTarget()));
                        break;
                }
                break;
        }
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
        } else {
            if (event.getOldStep().equals("groupPermissions")) {
                // confirm();
            }
        }
        return event.getNewStep();
    }

    @Override
    public List<PermissionModel> getPermissions() {
        return userPermissions;
    }

    @Override
    public String getPermissionHeader() {
        return "Permissions for " + (getSelectedUser() != null ? getSelectedUser().getUsername() : "");
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
        return userGroups;
    }

    public DefaultTreeNode getPermissionRoot() {
        return permissionRoot;
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

    @Override
    public boolean isShowPermissionTreeView() {
        return permissionTreeView;
    }

    @Override
    public void setShowPermissionTreeView(boolean set) {
        this.permissionTreeView = set;
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
