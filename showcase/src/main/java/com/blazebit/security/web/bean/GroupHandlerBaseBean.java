package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.model.UserGroupModel;
import com.blazebit.security.web.service.api.UserGroupService;

public abstract class GroupHandlerBaseBean extends PermissionTreeHandlingBaseBean {

    @Inject
    private UserGroupService userGroupService;

    /**
     * Builds a group tree for the selected groups
     * 
     * @param selectedGroups
     * @return root of a group tree
     */
    protected DefaultTreeNode buildGroupTree(List<UserGroup> selectedGroups) {
        DefaultTreeNode root = new DefaultTreeNode("root", null);
        List<UserGroup> rootParents = new ArrayList<UserGroup>();
        List<UserGroup> passedParents = new ArrayList<UserGroup>();
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
    private void createGroupNode(UserGroup group, List<UserGroup> parentGroups, List<UserGroup> userGroups, TreeNode node) {
        if (parentGroups.contains(group)) {
            DefaultTreeNode childNode = new DefaultTreeNode(new UserGroupModel(group, userGroups.contains(group), false), node);
            childNode.setExpanded(true);

            for (UserGroup ug : userGroupService.getGroupsForGroup(group)) {
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
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createNode(child, childNode, selectedUserGroups);
        }
    }

}
