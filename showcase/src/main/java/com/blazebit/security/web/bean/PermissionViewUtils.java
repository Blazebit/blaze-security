/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
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
import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.AbstractPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.web.bean.model.GroupModel;
import com.blazebit.security.web.bean.model.ResourceModel;
import com.blazebit.security.web.service.impl.UserGroupService;

/**
 * 
 * @author cuszk
 */
public class PermissionViewUtils {

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
            DefaultTreeNode entityNode = new DefaultTreeNode(new ResourceModel(entity, ResourceModel.ResourceType.ENTITY, entityField), permissionRoot);
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
                    DefaultTreeNode fieldNode = new DefaultTreeNode(new ResourceModel(permission.getResource().getField(), ResourceModel.ResourceType.FIELD,
                        permission.getResource(), markedPermissions.contains(permission)), entityNode);
                    fieldNode.setExpanded(expand);
                    fieldNode.setSelected(select);
                    if (selectedNodes != null) {
                        selectedNodes[selectedNodes.length - 1] = fieldNode;
                    }
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(permission.getAction().getActionName(), ResourceModel.ResourceType.ACTION,
                        permission.getAction(), markedPermissions.contains(permission)), fieldNode);
                    actionNode.setSelected(select);
                    if (selectedNodes != null) {
                        selectedNodes[selectedNodes.length - 1] = actionNode;
                    }
                } else {
                    DefaultTreeNode actionNode = new DefaultTreeNode(new ResourceModel(permission.getAction().getActionName(), ResourceModel.ResourceType.ACTION,
                        permission.getAction(), markedPermissions.contains(permission)), entityNode);
                    actionNode.setSelected(select);
                    if (selectedNodes != null) {
                        selectedNodes[selectedNodes.length - 1] = actionNode;
                    }
                }
            }

        }
    }

    protected Map<String, List<Permission>> groupPermissionsByEntity(List<Permission> permissions) {
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

    protected Map<Action, List<Permission>> groupPermissionsByAction(List<Permission> permissions) {
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
