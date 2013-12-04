package com.blazebit.security.web.bean.base;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.primefaces.model.DefaultTreeNode;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.service.resource.GroupPermissionHandling;
import com.blazebit.security.impl.service.resource.UserGroupDataAccess;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.service.api.UserGroupService;

public abstract class ResourceGroupHandlingBaseBean extends ResourceHandlingBaseBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    protected UserGroupService userGroupService;

    @Inject
    protected UserGroupDataAccess userGroupDataAccess;

    @Inject
    protected GroupPermissionHandling groupPermissionHandling;

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

}
