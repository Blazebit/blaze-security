package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.utils.GroupPermissionHandlingUtils;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.UserGroupDataAccess;
import com.blazebit.security.web.service.api.UserGroupService;

public abstract class GroupHandlerBaseBean extends PermissionTreeHandlingBaseBean {

    @Inject
    protected UserGroupService userGroupService;

    @Inject
    protected UserGroupDataAccess userGroupDataAccess;

    @Inject
    protected GroupPermissionHandlingUtils groupPermissionHandlingUtils;

    protected DefaultTreeNode initGroupPermissions(UserGroup selectedUserGroup, boolean hideFieldLevel) {
        DefaultTreeNode root = new DefaultTreeNode("root", null);
        DefaultTreeNode groupNode = root;

        if (isEnabled(Company.GROUP_HIERARCHY)) {
            List<UserGroup> parents = new ArrayList<UserGroup>();
            UserGroup parent = selectedUserGroup.getParent();
            parents.add(selectedUserGroup);
            while (parent != null) {
                parents.add(0, parent);
                parent = parent.getParent();
            }

            for (UserGroup group : parents) {
                groupNode = new DefaultTreeNode(new TreeNodeModel(group.getName(), ResourceType.USERGROUP, group), groupNode);
                groupNode.setExpanded(true);
                groupNode.setSelectable(false);
                List<Permission> allPermissions = permissionManager.getPermissions(group);
                List<Permission> permissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
                List<Permission> dataPermissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(1);
                getImmutablePermissionTree(groupNode, permissions, dataPermissions, hideFieldLevel);
                if (selectedUserGroup.equals(group)) {
                    ((TreeNodeModel) groupNode.getData()).setMarking(Marking.SELECTED);
                }
            }
        } else {
            groupNode = new DefaultTreeNode(new TreeNodeModel(selectedUserGroup.getName(), ResourceType.USERGROUP, selectedUserGroup), groupNode);
            groupNode.setExpanded(true);
            groupNode.setSelectable(false);

            List<Permission> allPermissions = permissionManager.getPermissions(selectedUserGroup);
            List<Permission> permissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
            List<Permission> dataPermissions = resourceUtils.getSeparatedPermissionsByResource(allPermissions).get(1);
            getImmutablePermissionTree(groupNode, permissions, dataPermissions, hideFieldLevel);
        }
        return root;
    }


    /**
     * Builds a group tree for the selected groups
     * 
     * @param selectedGroups
     * @return root of a group tree
     */
    protected DefaultTreeNode buildGroupTree(List<UserGroup> selectedGroups) {
        DefaultTreeNode root = new DefaultTreeNode("root", null);
        Set<UserGroup> rootParents = new HashSet<UserGroup>();
        Set<UserGroup> passedParents = new HashSet<UserGroup>();
        for (UserGroup userGroup : selectedGroups) {
            UserGroup parent = userGroup.getParent();
            passedParents.add(userGroup);
            if (parent == null) {
                rootParents.add(userGroup);
            } else {
                while (parent != null) {
                    if (parent.getParent() == null) {
                        rootParents.add(parent);
                    }
                    passedParents.add(parent);
                    parent = parent.getParent();
                }
            }

        }
        List<UserGroup> groups = new ArrayList<UserGroup>(rootParents);
        Collections.sort(groups, new Comparator<UserGroup>() {

            @Override
            public int compare(UserGroup o1, UserGroup o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (UserGroup parent : rootParents) {
            createGroupNode(parent, passedParents, selectedGroups, root);

        }
        return root;
    }

    /**
     * helper recursive function to iterate through the child groups of a group and decide if it should be displayed or not
     * 
     * @param group
     * @param parentGroups
     * @param userGroups
     * @param node
     */
    private void createGroupNode(UserGroup group, Collection<UserGroup> parentGroups, Collection<UserGroup> userGroups, TreeNode node) {
        if (parentGroups.contains(group)) {
            DefaultTreeNode childNode = new DefaultTreeNode(new UserGroupModel(group, userGroups.contains(group), false), node);
            childNode.setExpanded(true);

            for (UserGroup ug : userGroupDataAccess.getGroupsForGroup(group)) {
                createGroupNode(ug, parentGroups, userGroups, childNode);
            }
        }

    }

    /**
     * creates a selectable group tree with all the existing child groups
     * 
     * @param parentGroups
     * @param selectedGroups
     * @param selectedGroupNodes
     * @return
     */
    public DefaultTreeNode getGroupTree(List<UserGroup> parentGroups, Collection<UserGroup> selectedGroups) {
        DefaultTreeNode root = new DefaultTreeNode();
        for (UserGroup group : parentGroups) {
            createNode(group, root, selectedGroups);
        }
        return root;
    }

    /**
     * helper to build tree
     * 
     * @param group
     * @param node
     */
    private void createNode(UserGroup group, DefaultTreeNode node, Collection<UserGroup> selectedUserGroups) {
        DefaultTreeNode childNode = new DefaultTreeNode(new UserGroupModel(group, false, false), node);
        childNode.setExpanded(true);
        if (selectedUserGroups.contains(group)) {
            // node is selected if user belongs to group
            childNode.setSelected(true);
            // node is not allowed to be unselected if it is not permitted be revoked
            if (!isGranted(ActionConstants.REVOKE, resourceFactory.createResource(group))) {
                childNode.setSelectable(false);
            }
        } else {
            // node is not allowed to be selected if it is not permitted be granted
            if (!isGranted(ActionConstants.GRANT, resourceFactory.createResource(group))) {
                childNode.setSelectable(false);
            }
        }
        for (UserGroup child : userGroupDataAccess.getGroupsForGroup(group)) {
            createNode(child, childNode, selectedUserGroups);
        }
    }

}
