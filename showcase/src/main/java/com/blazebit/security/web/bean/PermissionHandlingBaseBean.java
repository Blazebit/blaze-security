/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.deltaspike.core.util.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.web.bean.model.GroupModel;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
public class PermissionHandlingBaseBean {

    @Inject
    protected PermissionFactory permissionFactory;
    @Inject
    protected UserSession userSession;
    @Inject
    protected PermissionDataAccess permissionDataAccess;
    @Inject
    protected PermissionManager permissionManager;
    @Inject
    protected EntityFieldFactory entityFieldFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;

    protected boolean permissionExists(Permission permisison) {
        return permissionDataAccess.findPermission(userSession.getSelectedUser(), permisison.getAction(), permisison.getResource()) != null;
    }

    protected List<TreeNode> sortTreeNodesByType(TreeNode[] selectedTreeNodes) {
        List<TreeNode> sortedSelectedNodes = Arrays.asList(selectedTreeNodes);
        Collections.sort(sortedSelectedNodes, new Comparator<TreeNode>() {

            @Override
            public int compare(TreeNode o1, TreeNode o2) {
                NodeModel model1 = (NodeModel) o1.getData();
                NodeModel model2 = (NodeModel) o2.getData();
                if (model1.getType().ordinal() == model2.getType().ordinal()) {
                    return 0;
                } else {
                    return model1.getType().ordinal() < model2.getType().ordinal() ? -1 : 1;
                }
            }

        });
        return sortedSelectedNodes;
    }

    protected boolean contains(Collection<Permission> permissions, Permission permission) {
        return find(permissions, permission) != null;
    }

    protected Permission find(Collection<Permission> permissions, Permission permission) {
        for (Permission p : permissions) {
            AbstractPermission givenPermission = (AbstractPermission) p;
            if (givenPermission.getAction().equals(permission.getAction()) && givenPermission.getResource().equals(permission.getResource())) {
                return p;
            }
        }
        return null;
    }

    /**
     * removes given permission from a permission collection (collection contains user and usergroup permissions)
     * 
     * @param permissions
     * @param permission
     * @return
     */
    protected List<Permission> remove(List<Permission> permissions, Permission permission) {
        Permission found = find(permissions, permission);
        permissions.remove(found);
        return permissions;
    }

    /**
     * removes given permissions from a permission collection (collection contains user and usergroup permissions)
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    protected List<Permission> removeAll(List<Permission> permissions, Collection<Permission> permissionsToRemove) {
        for (Permission permission : permissionsToRemove) {
            Permission found = find(permissions, permission);
            permissions.remove(found);
        }
        return permissions;
    }

    public Set<Permission> processSelectedPermissions(TreeNode[] selectedPermissionNodes) {
        Set<Permission> ret = new HashSet<Permission>();
        List<TreeNode> sortedSelectedNodes = sortTreeNodesByType(selectedPermissionNodes);
        for (TreeNode permissionNode : sortedSelectedNodes) {
            NodeModel permissionNodeData = (NodeModel) permissionNode.getData();
            switch (permissionNodeData.getType()) {
                case ENTITY:
                    for (TreeNode actionNode : permissionNode.getChildren()) {
                        NodeModel actionNodeData = (NodeModel) actionNode.getData();
                        if (actionNode.getChildCount() == 0) {
                            ret
                                .add(permissionFactory.create(userSession.getSelectedUser(), (EntityAction) actionNodeData.getTarget(),
                                                              (EntityField) permissionNodeData.getTarget()));
                        } else {
                            for (TreeNode fieldNode : actionNode.getChildren()) {
                                NodeModel fieldNodeData = (NodeModel) fieldNode.getData();
                                ret
                                    .add(permissionFactory.create(userSession.getSelectedUser(), (EntityAction) actionNodeData.getTarget(), (EntityField) fieldNodeData.getTarget()));
                            }
                        }
                    }
                    break;
                case ACTION:
                    TreeNode entityNode = permissionNode.getParent();
                    NodeModel entityNodeModel = (NodeModel) entityNode.getData();
                    Permission actionPermission = permissionFactory.create(userSession.getSelectedUser(), (EntityAction) permissionNodeData.getTarget(),
                                                                           (EntityField) entityNodeModel.getTarget());
                    if (permissionNode.getChildCount() == 0) {
                        if (!contains(ret, actionPermission)) {
                            ret.add(permissionFactory.create(userSession.getUser(), (EntityAction) permissionNodeData.getTarget(), (EntityField) entityNodeModel.getTarget()));
                        }
                    }
                    break;
                case FIELD:
                    TreeNode actionNode = permissionNode.getParent();
                    NodeModel actionNodeData = (NodeModel) actionNode.getData();

                    TreeNode actionEntityNode = actionNode.getParent();
                    NodeModel actionEntityNodeModel = (NodeModel) actionEntityNode.getData();
                    Permission fieldPermission = permissionFactory.create(userSession.getSelectedUser(), (EntityAction) actionNodeData.getTarget(),
                                                                          (EntityField) actionEntityNodeModel.getTarget());
                    if (!contains(ret, fieldPermission)) {
                        ret.add(permissionFactory.create(userSession.getSelectedUser(), (EntityAction) actionNodeData.getTarget(), ((EntityField) permissionNodeData.getTarget())));
                    }
                    break;
            }
        }
        return ret;
    }

    protected void buildPermissionTree(List<Permission> permissions, TreeNode permissionRoot) {
        buildPermissionTree(permissions, permissionRoot, true, false, null);
    }

    protected void buildPermissionTree(List<Permission> permissions, Set<Permission> marked, TreeNode permissionRoot) {
        buildPermissionTree(permissions, marked, permissionRoot, true, false, null);
    }

    protected void buildPermissionTree(List<Permission> permissions, TreeNode permissionRoot, boolean expand, boolean select, TreeNode[] selectedNodes) {
        buildPermissionTree(permissions, new HashSet<Permission>(), permissionRoot, expand, select, selectedNodes);
    }

    /**
     * 
     * @param permissions
     * @param permissionRoot
     */
    protected void buildPermissionTree(List<Permission> permissions, Set<Permission> markedPermissions, TreeNode permissionRoot, boolean expand, boolean select, TreeNode[] selectedNodes) {
        Map<String, List<Permission>> permissionMap = groupPermissionsByEntity(permissions);

        for (String entity : permissionMap.keySet()) {

            List<Permission> permissionGroup = new ArrayList<Permission>(permissionMap.get(entity));
            EntityField entityField = new EntityField(entity, "");
            DefaultTreeNode entityNode = new DefaultTreeNode(new NodeModel(entity, NodeModel.ResourceType.ENTITY, entityField), permissionRoot);
            entityNode.setExpanded(expand);
            entityNode.setSelected(select);
            if (selectedNodes != null) {
                selectedNodes[selectedNodes.length - 1] = entityNode;
            }

            AbstractPermission permission = null;
            for (Permission _permission : permissionGroup) {
                if (_permission instanceof UserGroupPermission) {
                    permission = (UserGroupPermission) _permission;
                } else {
                    if (_permission instanceof UserPermission) {
                        permission = (UserPermission) _permission;
                    }
                }
                if (!StringUtils.isEmpty(permission.getResource().getField())) {
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new NodeModel(permission.getResource().getField(), NodeModel.ResourceType.FIELD,
                        permission.getResource(), markedPermissions.contains(permission)), entityNode);
                    fieldNode.setExpanded(expand);
                    fieldNode.setSelected(select);
                    if (selectedNodes != null) {
                        selectedNodes[selectedNodes.length - 1] = fieldNode;
                    }
                    DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(permission.getAction().getActionName(), NodeModel.ResourceType.ACTION,
                        permission.getAction(), markedPermissions.contains(permission)), fieldNode);
                    actionNode.setSelected(select);
                    if (selectedNodes != null) {
                        selectedNodes[selectedNodes.length - 1] = actionNode;
                    }
                } else {
                    DefaultTreeNode actionNode = new DefaultTreeNode(new NodeModel(permission.getAction().getActionName(), NodeModel.ResourceType.ACTION,
                        permission.getAction(), markedPermissions.contains(permission)), entityNode);
                    actionNode.setSelected(select);
                    if (selectedNodes != null) {
                        selectedNodes[selectedNodes.length - 1] = actionNode;
                    }
                }
            }

        }
    }

    protected Map<String, List<Permission>> groupPermissionsByEntity(Collection<Permission> permissions) {
        Map<String, List<Permission>> ret = new HashMap<String, List<Permission>>();
        List<Permission> group;
        for (Permission p : permissions) {
            String entityName = ((EntityField) p.getResource()).getEntity();
            if (ret.containsKey(entityName)) {
                group = ret.get(entityName);
            } else {
                group = new ArrayList<Permission>();

            }
            group.add(p);
            ret.put(entityName, group);
        }
        for (String entityName : ret.keySet()) {
            group = ret.get(entityName);
            Collections.sort(group, new Comparator<Permission>() {

                @Override
                public int compare(Permission o1, Permission o2) {
                    return ((AbstractPermission) o1).getResource().getField().compareTo(((AbstractPermission) o2).getResource().getField());
                }
            });
            ret.put(entityName, group);

        }
        return ret;
    }

    protected Map<Action, List<Permission>> groupPermissionsByAction(Collection<Permission> permissions) {
        Map<Action, List<Permission>> ret = new HashMap<Action, List<Permission>>();
        List<Permission> group;
        for (Permission p : permissions) {
            EntityAction entityAction = (EntityAction) p.getAction();
            if (ret.containsKey(entityAction)) {
                group = ret.get(entityAction);
            } else {
                group = new ArrayList<Permission>();

            }
            group.add(p);
            ret.put(entityAction, group);
        }
        for (Action entityAction : ret.keySet()) {
            group = ret.get(entityAction);
            Collections.sort(group, new Comparator<Permission>() {

                @Override
                public int compare(Permission o1, Permission o2) {
                    return ((AbstractPermission) o1).getResource().getField().compareTo(((AbstractPermission) o2).getResource().getField());
                }
            });
            ret.put(entityAction, group);

        }
        return ret;
    }

    @Inject
    private UserGroupService userGroupService;

    protected void buildGroupTree(List<UserGroup> userGroups, TreeNode groupRoot) {
        buildGroupTree(userGroups, groupRoot, true);
    }

    /**
     * 
     * @param permissions
     * @param permissionRoot
     */
    protected void buildGroupTree(List<UserGroup> userGroups, TreeNode groupRoot, boolean expand) {
        List<UserGroup> parentGroups = userGroupService.getAllParentGroups();
        for (UserGroup parent : parentGroups) {
            createGroupNode(parent, userGroups, groupRoot, expand);
        }
    }

    private void createGroupNode(UserGroup group, List<UserGroup> allowedGroups, TreeNode node, boolean expand) {
        DefaultTreeNode childNode = new DefaultTreeNode(new GroupModel(group, allowedGroups.contains(group), false), node);
        childNode.setExpanded(expand);
        for (UserGroup ug : userGroupService.getGroupsForGroup(group)) {
            createGroupNode(ug, allowedGroups, childNode, expand);
        }
    }
}
