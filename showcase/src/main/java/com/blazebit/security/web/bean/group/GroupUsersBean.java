/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.group;

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
import com.blazebit.security.web.bean.model.PermissionModel;
import com.blazebit.security.web.bean.model.ResourceAction;
import com.blazebit.security.web.bean.model.ResourceModel;
import com.blazebit.security.web.bean.model.UserModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "groupUsersBean")
@ViewScoped
public class GroupUsersBean extends PermissionViewUtils implements PermissionView, Serializable {

    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private RoleService roleService;
    @Inject
    private PermissionService permissionService;
    @Inject
    private UserContext userContext;
    @Inject
    private UserSession userSession;
    @Inject
    private PermissionDataAccess permissionDataAccess;
    private List<PermissionModel> groupPermissions = new ArrayList<PermissionModel>();
    private TreeNode[] selectedPermissionNodes;
    private DefaultTreeNode permissionRoot;
    private List<UserModel> users = new ArrayList<UserModel>();
    private Set<ResourceAction> selectedResourceActions = new HashSet<ResourceAction>();
    private Map<UserGroup, List<Permission>> groupPermissionsMap = new HashMap<UserGroup, List<Permission>>();

    private List<User> selectedUsers = new ArrayList<User>();
    @Inject
    private PermissionManager permissionManager;

    public GroupUsersBean() {
    }

    public void init() {
        initUsers();
    }

    private void initUsers() {
        this.users.clear();
        List<User> existingUsers = userGroupService.getUsersFor(getSelectedGroup());
        for (User user : userService.findUsers()) {
            if (!existingUsers.contains(user)) {
                this.users.add(new UserModel(user, false));
            }
        }
    }

    @Override
    public List<PermissionModel> getPermissions() {
        return groupPermissions;
    }

    @Override
    public String getPermissionHeader() {
        return "UserGroup - " + (getSelectedGroup() != null ? getSelectedGroup().getName() : "");
    }

    private UserGroup getSelectedGroup() {
        return userSession.getSelectedUserGroup();
    }

    public String userWizardListener(FlowEvent event) {
        if (event.getOldStep().equals("users")) {
            selectUsers();
        } else {
            if (event.getOldStep().equals("groupPermissions")) {
                grantSelectedPermissions();
            }
        }
        return event.getNewStep();
    }

    private void selectUsers() {
        for (UserModel userModel : users) {
            if (userModel.isSelected()) {
                selectedUsers.add(userModel.getUser());
            }
        }
        permissionRoot = new DefaultTreeNode("root", null);
        List<UserGroup> groupList = new ArrayList<UserGroup>();
        groupList.add(getSelectedGroup());
        UserGroup parent = getSelectedGroup().getParent();
        while (parent != null) {
            groupList.add(0, parent);
            parent = parent.getParent();
        }
        DefaultTreeNode childNode = permissionRoot;
        for (UserGroup group : groupList) {
            groupPermissionsMap.put(group, permissionManager.getAllPermissions(group));
            childNode = new DefaultTreeNode(new ResourceModel(group.getName(), ResourceModel.ResourceType.USERGROUP, group), childNode);
            childNode.setExpanded(true);
            childNode.setSelected(true);
            // add permission tree for each node
            addPermissionTreeToNode(group, childNode);
        }
    }

    private void addPermissionTreeToNode(UserGroup group, DefaultTreeNode node) {
        List<Permission> permissions = permissionManager.getAllPermissions(group);

        Map<String, List<Permission>> index = groupPermissionsByEntity(permissions);
        for (String entity : index.keySet()) {

            List<Permission> permissionGroup = new ArrayList<Permission>(index.get(entity));
            Collections.sort(permissionGroup, new Comparator<Permission>() {

                @Override
                public int compare(Permission o1, Permission o2) {
                    return ((UserGroupPermission) o1).getResource().getField().compareTo(((UserGroupPermission) o2).getResource().getField());
                }
            });
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new ResourceModel(entity, ResourceModel.ResourceType.ENTITY, entityField), node);
            entityNode.setExpanded(true);
            entityNode.setSelected(true);

            for (Permission p : permissionGroup) {
                UserGroupPermission ugp = (UserGroupPermission) p;
                if (!StringUtils.isEmpty(ugp.getResource().getField())) {
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new ResourceModel(ugp.getResource().getField(), ResourceModel.ResourceType.FIELD, ugp.getResource()),
                        entityNode);
                    fieldNode.setExpanded(true);
                    fieldNode.setSelected(true);
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(ugp.getAction().getActionName(), ResourceModel.ResourceType.ACTION, ugp.getAction()),
                        fieldNode);
                    actionNode.setSelected(true);
                } else {
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(ugp.getAction().getActionName(), ResourceModel.ResourceType.ACTION, ugp.getAction()),
                        entityNode);
                    actionNode.setSelected(true);
                }
            }
        }

    }

    /**
     * Grant button
     */
    public void grantSelectedPermissions() {
        selectedResourceActions.clear();
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

    public TreeNode[] getSelectedPermissionNodes() {
        return selectedPermissionNodes;
    }

    public void setSelectedPermissionNodes(TreeNode[] selectedPermissionNodes) {
        this.selectedPermissionNodes = selectedPermissionNodes;
    }

    public List<UserModel> getUsers() {
        return users;
    }

    public DefaultTreeNode getPermissionRoot() {
        return permissionRoot;
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

    public void setSelectedResourceActions(Set<ResourceAction> selectedResourceActions) {
        this.selectedResourceActions = selectedResourceActions;
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    public void confirmGrantSelectedPermissions() {
        for (User user : selectedUsers) {
            roleService.addSubjectToRole(userContext.getUser(), user, getSelectedGroup(), false);
            for (ResourceAction resourceAction : selectedResourceActions) {
                permissionService.grant(userContext.getUser(), user, resourceAction.getAction(), resourceAction.getResource());
            }
        }
    }

    @Override
    public boolean isShowPermissionTreeView() {
        return true;
    }

    @Override
    public void setShowPermissionTreeView(boolean set) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools |
                                                                       // Templates.
    }

    @Override
    public TreeNode getPermissionViewRoot() {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools |
                                                                       // Templates.
    }
}
