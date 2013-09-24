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

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupPermission;
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
    private List<PermissionModel> userPermissions = new ArrayList<PermissionModel>();
    private List<UserGroup> userGroups = new ArrayList<UserGroup>();
    private TreeNode[] selectedGroupNodes;
    private DefaultTreeNode groupRoot;
    private TreeNode[] selectedPermissionNodes;
    private DefaultTreeNode permissionRoot;
    private DefaultTreeNode permissionViewRoot;
    private Set<ResourceAction> selectedResourceActions = new HashSet<ResourceAction>();
    private Map<UserGroup, List<Permission>> groupPermissionsMap = new HashMap<UserGroup, List<Permission>>();
    private List<UserGroup> originalSelectedGroups = new ArrayList<UserGroup>();
    private UserGroup selectedGroup;
    private boolean permissionTreeView;
    private Set<UserGroup> addToGroups = new HashSet<UserGroup>();
    private Set<UserGroup> removeFromGroups = new HashSet<UserGroup>();
    private DefaultTreeNode currentPermissionTreeRoot;
    private DefaultTreeNode newPermissionTreeRoot;

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
        List<Permission> permissions = permissionManager.getAllPermissions(getSelectedUser());
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
        // node is selectable if user can be added to group
        childNode.setSelectable(roleService.canUserBeAddedToRole(getSelectedUser(), group));
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createNode(child, childNode);
        }
    }

    /**
     * 1 step
     */
    public void processSelectedGroups() {
        selectedResourceActions.clear();

        addToGroups.clear();
        removeFromGroups.clear();
        // build next tree

        permissionRoot = new DefaultTreeNode("root", null);
        newPermissionTreeRoot = new DefaultTreeNode("root", null);
        currentPermissionTreeRoot = new DefaultTreeNode("root", null);

        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode node : selectedGroupNodes) {
            GroupModel selectedGroupModel = (GroupModel) node.getData();
            if (roleService.canUserBeRemovedFromRole(getSelectedUser(), selectedGroupModel.getUserGroup())) {
                // user belongs to this group already => keep permissions from this group
            } else {
                if (roleService.canUserBeAddedToRole(getSelectedUser(), selectedGroupModel.getUserGroup())) {
                    addToGroups.add(selectedGroupModel.getUserGroup());
                    selectedGroups.add(selectedGroupModel.getUserGroup());
                    UserGroup parent = selectedGroupModel.getUserGroup().getParent();
                    while (parent != null) {
                        selectedGroups.add(parent);
                        parent = parent.getParent();
                    }
                }
            }
        }
        Set<Permission> allGroupPermissions = new HashSet<Permission>();
        for (UserGroup group : selectedGroups) {
            allGroupPermissions.addAll(permissionManager.getAllPermissions(group));
        }

        List<Permission> permissions = permissionManager.getAllPermissions(getSelectedUser());

        // markCurrentPermissions(currentPermissionTreeRoot);
        Set<Permission> toRemove = new HashSet<Permission>();
        for (Permission permission : permissions) {
            for (Permission groupPermission : allGroupPermissions) {
                Set<Permission> toRevoke = permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), groupPermission.getAction(), groupPermission.getResource());
                if (toRevoke.contains(permission)) {
                    toRemove.add(permission);
                }
            }
        }
        buildPermissionTree(permissions, toRemove, currentPermissionTreeRoot);

        permissions.addAll(allGroupPermissions);
        buildPermissionTree(permissions, newPermissionTreeRoot);
        // markNewPermissions(newPermissionTreeRoot, );

        // remove groups
        removeFromGroups = new HashSet<UserGroup>(userGroupService.getGroupsForUser(getSelectedUser()));
        removeFromGroups.removeAll(addToGroups);

    }

    /**
     * 1 step
     */
    public void selectGroupsToRemove() {
        selectedResourceActions.clear();
        originalSelectedGroups.clear();
        permissionRoot = new DefaultTreeNode("root", null);
        List<UserGroup> parentGroups = new ArrayList<UserGroup>();
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        for (TreeNode node : selectedGroupNodes) {
            GroupModel selectedGroup = (GroupModel) node.getData();
            originalSelectedGroups.add(selectedGroup.getUserGroup());
            selectedGroups.add(selectedGroup.getUserGroup());
            UserGroup parent = selectedGroup.getUserGroup().getParent();
            while (parent != null) {
                selectedGroups.add(parent);
                parent = parent.getParent();
            }
        }
        for (UserGroup group : selectedGroups) {
            if (group.getParent() == null) {
                parentGroups.add(group);
            }
        }
        selectedPermissionNodes = new TreeNode[parentGroups.size()];
        List<Permission> revokablePermissions = new ArrayList<Permission>();
        for (UserGroup parent : parentGroups) {
            // check if there is anything to revoke from user from the parent group permissions
            addPermission(parent, selectedGroups, revokablePermissions);
        }
        buildPermissionTree(revokablePermissions, permissionRoot, true, true, selectedPermissionNodes);
    }

    private void addPermission(UserGroup group, Set<UserGroup> selectedGroups, List<Permission> revokablePermissions) {
        List<Permission> permissions = permissionManager.getAllPermissions(group);
        for (Permission permission : permissions) {
            if (permissionDataAccess.isRevokable(getSelectedUser(), permission.getAction(), permission.getResource())) {
                revokablePermissions.add(permission);
            }
        }
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            if (selectedGroups.contains(child)) {
                addPermission(child, selectedGroups, revokablePermissions);
            }
        }
    }

    /**
     * helper to build tree
     * 
     * @param group
     * @param node
     */
    private void createGroupNodeWithPermissions(UserGroup group, Set<UserGroup> selectedGroups, DefaultTreeNode node) {
        List<UserGroup> children = userGroupService.getGroupsForGroup(group);
        // DefaultTreeNode childNode = new DefaultTreeNode(new ResourceModel(group.getName(), ResourceType.USERGROUP, group),
        // node);
        // selectedPermissionNodes[selectedPermissionNodes.length - 1] = childNode;
        // add permission tree for each node
        addPermissionTreeToNode(group, node);
        for (UserGroup child : children) {
            if (selectedGroups.contains(child)) {
                createGroupNodeWithPermissions(child, selectedGroups, node);
            }
        }
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
            // selectGroupsToRemove();
        } else {
            if (event.getOldStep().equals("groupPermissions")) {
                grantSelectedPermissions();
                // revokeSelectedPermissions();
            }
        }
        return event.getNewStep();
    }

    /**
     * helper to build tree
     * 
     * @param group
     * @param node
     */
    private void addPermissionTreeToNode(UserGroup group, DefaultTreeNode node) {
        List<Permission> permissions = permissionManager.getAllPermissions(group);
        groupPermissionsMap.put(group, permissions);

        Map<String, List<Permission>> permissionMap = groupPermissionsByEntity(permissions);
        for (String entity : permissionMap.keySet()) {

            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMap.get(entity));

            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new ResourceModel(entity, ResourceType.ENTITY, entityField), node);
            entityNode.setSelected(true);
            entityNode.setExpanded(true);

            for (Permission p : permissionGroup) {
                UserGroupPermission ugp = (UserGroupPermission) p;
                if (!StringUtils.isEmpty(ugp.getResource().getField())) {
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new ResourceModel(ugp.getResource().getField(), ResourceType.FIELD, ugp.getResource()), entityNode);
                    fieldNode.setSelected(true);
                    entityNode.setExpanded(true);
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(ugp.getAction().getActionName(), ResourceType.ACTION, ugp.getAction()), fieldNode);
                    actionNode.setSelected(true);
                } else {
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(ugp.getAction().getActionName(), ResourceType.ACTION, ugp.getAction()), entityNode);
                    actionNode.setSelected(true);
                }
            }
        }
    }

    /**
     * Grant button
     */
    public void grantSelectedPermissions() {
        processSelectedPermissions();
        Set<ResourceAction> notGrantable = new HashSet<ResourceAction>();

        for (ResourceAction resourceAction : selectedResourceActions) {
            if (!permissionDataAccess.isGrantable(getSelectedUser(), resourceAction.getAction(), resourceAction.getResource())) {
                notGrantable.add(resourceAction);
            } else {
                // mark conflicting user permissions
                Set<Permission> permissions = permissionDataAccess.getRevokablePermissionsWhenGranting(getSelectedUser(), resourceAction.getAction(), resourceAction.getResource());
                for (PermissionModel model : this.userPermissions) {
                    if (permissions.contains(model.getPermission())) {
                        model.setSelected(true);
                    }
                }
            }
        }
        selectedResourceActions.removeAll(notGrantable);
    }

    /**
     * Grant button
     */
    public void revokeSelectedPermissions() {
        processSelectedPermissions();
    }

    /**
     * confirm button
     */
    public void confirmGrantSelectedPermissions() {
        // grant permissions to user
        for (ResourceAction resourceAction : selectedResourceActions) {
            permissionService.grant(userSession.getUser(), getSelectedUser(), resourceAction.getAction(), resourceAction.getResource());
        }
        // reload permissions
        initUserPermissions();
        // add user to selected groups
        for (UserGroup userGroup : originalSelectedGroups) {
            roleService.addSubjectToRole(userSession.getUser(), getSelectedUser(), userGroup, false);
        }
        // reload groups
        initUserGroups();
    }

    /**
     * confirm button
     */
    public void confirmRevokeSelectedPermissions() {
        // grant permissions to user
        for (ResourceAction resourceAction : selectedResourceActions) {
            permissionService.revoke(userSession.getUser(), getSelectedUser(), resourceAction.getAction(), resourceAction.getResource());
        }
        // reload permissions
        initUserPermissions();
        // add user to selected groups
        for (UserGroup userGroup : originalSelectedGroups) {
            roleService.removeSubjectFromRole(userSession.getUser(), getSelectedUser(), userGroup, false);
        }
        // reload groups
        initUserGroups();
    }

    public List<ResourceAction> getSelectedResourceActions() {
        List<ResourceAction> ret = new ArrayList<ResourceAction>();
        ret.addAll(selectedResourceActions);
        Collections.sort(ret, new Comparator<ResourceAction>() {

            @Override
            public int compare(ResourceAction o1, ResourceAction o2) {
                return o1.getResource().getEntity().compareTo(o2.getResource().getEntity());
            }
        });
        return ret;
    }

    public void setSelectedGroup(UserGroup group) {
        this.selectedGroup = group;
        while (group != null) {
            List<Permission> permissions = permissionManager.getAllPermissions(group);
            for (Permission permission : permissions) {
                for (PermissionModel userPermissionModel : userPermissions) {
                    if (userPermissionModel.getPermission().equals(permission)) {
                        userPermissionModel.setSelected(true);
                    }
                }
            }
            group = group.getParent();
        }
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

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedPermissionNodes = selectedPermissionNodes;
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

    private void processSelectedPermissions() {
        selectedResourceActions.clear();
        if (selectedPermissionNodes.length > 1) {
            for (TreeNode permissionNode : selectedPermissionNodes) {
                ResourceModel permissionNodeData = (ResourceModel) permissionNode.getData();
                switch (permissionNodeData.getType()) {
                    case USERGROUP:
                        // it means that all of the groups permissions shall be granted to the user
                        List<Permission> permissions = groupPermissionsMap.get((UserGroup) permissionNodeData.getTarget());
                        for (Permission permission : permissions) {
                            selectedResourceActions.add(new ResourceAction((EntityField) permission.getResource(), (EntityAction) permission.getAction()));
                        }
                        break;
                    case ENTITY:
                        for (TreeNode childNode : permissionNode.getChildren()) {
                            ResourceModel childNodeModel = (ResourceModel) childNode.getData();
                            switch (childNodeModel.getType()) {
                                case FIELD:
                                    for (TreeNode actionNode : childNode.getChildren()) {
                                        ResourceModel actionNodeModel = (ResourceModel) actionNode.getData();
                                        selectedResourceActions.add(new ResourceAction((EntityField) childNodeModel.getTarget(), (EntityAction) actionNodeModel.getTarget()));
                                    }
                                    break;
                                case ACTION:
                                    ResourceModel actionNodeModel = (ResourceModel) childNode.getData();
                                    selectedResourceActions.add(new ResourceAction((EntityField) permissionNodeData.getTarget(), (EntityAction) actionNodeModel.getTarget()));
                                    break;
                            }
                        }
                        break;
                    case FIELD:
                        EntityField resource = (EntityField) permissionNodeData.getTarget();
                        for (TreeNode actionTreeNode : permissionNode.getChildren()) {
                            ResourceModel actionNodeData = (ResourceModel) actionTreeNode.getData();
                            selectedResourceActions.add(new ResourceAction(resource, (EntityAction) actionNodeData.getTarget()));
                        }
                        break;
                    case ACTION:
                        TreeNode parentNode = permissionNode.getParent();
                        ResourceModel parentPermissionNodeData = (ResourceModel) parentNode.getData();
                        switch (parentPermissionNodeData.getType()) {
                            case ENTITY:
                            case FIELD:
                                selectedResourceActions.add(new ResourceAction((EntityField) parentPermissionNodeData.getTarget(), (EntityAction) permissionNodeData.getTarget()));
                                break;
                        }
                        break;
                }
            }
        }
    }

    public DefaultTreeNode getCurrentPermissionTreeRoot() {
        return currentPermissionTreeRoot;
    }

    public DefaultTreeNode getNewPermissionTreeRoot() {
        return newPermissionTreeRoot;
    }
}
