package com.blazebit.security.web.bean;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.model.GroupModel;
import com.blazebit.security.web.service.impl.UserGroupService;

public abstract class GroupHandlerBaseBean extends PermissionHandlingBaseBean {

    @Inject
    private UserGroupService userGroupService;

    public DefaultTreeNode getGroupTree(Collection<UserGroup> selectedGroups, TreeNode[] selectedGroupNodes) {
        List<UserGroup> availableGroups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
        DefaultTreeNode root = new DefaultTreeNode();
        for (UserGroup group : availableGroups) {
            // only those groups are shown which can be granted by the logged in user
            if (isAuthorizedResource(ActionConstants.GRANT, entityFieldFactory.createResource(group)))
                createNode(group, root, selectedGroups, selectedGroupNodes);
        }
        return root;
    }

    /**
     * helper to build tree
     * 
     * @param group
     * @param node
     */
    private void createNode(UserGroup group, DefaultTreeNode node, Collection<UserGroup> selectedUserGroups, TreeNode[] selectedGroupNodes) {
        DefaultTreeNode childNode = new DefaultTreeNode(new GroupModel(group, false, false), node);
        childNode.setExpanded(true);
        // node is selected if user belongs to group
        childNode.setSelected(selectedUserGroups.contains(group));
        selectedGroupNodes = addNodeToSelectedNodes(childNode, selectedGroupNodes);
        for (UserGroup child : userGroupService.getGroupsForGroup(group)) {
            createNode(child, childNode, selectedUserGroups, selectedGroupNodes);
        }
    }

}
