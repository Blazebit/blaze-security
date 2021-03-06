package com.blazebit.security.web.bean.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.PermissionUtils;
import com.blazebit.security.entity.EntityPermissionUtils;
import com.blazebit.security.model.Features;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.PermissionChangeSet;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.showcase.data.UserGroupDataAccess;
import com.blazebit.security.showcase.service.UserGroupService;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;

public abstract class ResourceGroupHandlingBaseBean extends ResourceHandlingBaseBean {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    protected UserGroupService userGroupService;

    @Inject
    protected UserGroupDataAccess userGroupDataAccess;

    protected Map<User, Set<Permission>> revokables = new HashMap<User, Set<Permission>>();
    protected Map<User, Set<Permission>> replacables = new HashMap<User, Set<Permission>>();

    protected DefaultTreeNode initGroupPermissions(UserGroup selectedUserGroup, boolean hideFieldLevel) {
        DefaultTreeNode root = new DefaultTreeNode("root", null);
        DefaultTreeNode groupNode = root;

        if (isEnabled(Features.GROUP_HIERARCHY)) {
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
                List<Permission> permissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
                List<Permission> dataPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(1);
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
            List<Permission> permissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
            List<Permission> dataPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(1);
            getImmutablePermissionTree(groupNode, permissions, dataPermissions, hideFieldLevel);
        }
        return root;
    }

    protected void prepareUserPropagationView(UserGroup group, Set<Permission> granted, Set<Permission> revoked, TreeNode currentUserPermissionTreeRoot, TreeNode newUserPermissionTreeRoot, TreeNode[] selectedUserPermissionNodes) {
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(group);

        revokables.clear();
        replacables.clear();
        Map<UserGroup, Set<Permission>> grantedMap = new HashMap<UserGroup, Set<Permission>>();
        grantedMap.put(group, granted);
        Map<UserGroup, Set<Permission>> revokedMap = new HashMap<UserGroup, Set<Permission>>();
        revokedMap.put(group, revoked);

        prepareUserPropagationView(selectedGroups, grantedMap, revokedMap, currentUserPermissionTreeRoot, newUserPermissionTreeRoot, selectedUserPermissionNodes);
    }

    protected void prepareUserPropagationView(Set<UserGroup> selectedGroups, Map<UserGroup, Set<Permission>> granted, Map<UserGroup, Set<Permission>> revoked, TreeNode currentUserPermissionTreeRoot, TreeNode newUserPermissionTreeRoot, TreeNode[] selectedUserPermissionNodes) {
        // show user propagation view
        buildUserPermissionTrees(currentUserPermissionTreeRoot, newUserPermissionTreeRoot, userGroupDataAccess.collectUsers(selectedGroups, isEnabled(Features.GROUP_HIERARCHY)),
                                 granted, revoked, selectedUserPermissionNodes);
    }

    private void buildUserPermissionTrees(TreeNode currentUserPermissionTreeRoot, TreeNode newUserPermissionTreeRoot, List<User> users, Map<UserGroup, Set<Permission>> grantedGroupPermissions, Map<UserGroup, Set<Permission>> revokedPermissions, TreeNode[] selectedUserPermissionNodes) {
        for (User user : users) {

            List<Permission> allPermissions = permissionManager.getPermissions(user);
            List<Permission> userPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(0);
            List<Permission> userDataPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(allPermissions).get(1);

            List<UserGroup> groupsOfUser = userGroupDataAccess.getGroupsForUser(user);

            Set<Permission> permissionsToGrant = new HashSet<Permission>();
            for (UserGroup userGroup : groupsOfUser) {
                if (grantedGroupPermissions.containsKey(userGroup)) {
                    permissionsToGrant.addAll(grantedGroupPermissions.get(userGroup));
                }
            }
            // normalize all permissions
            permissionsToGrant = permissionHandling.getNormalizedPermissions(permissionsToGrant);

            Set<Permission> permissionsToRevoke = new HashSet<Permission>();
            for (UserGroup userGroup : groupsOfUser) {
                if (revokedPermissions.containsKey(userGroup)) {
                    permissionsToRevoke.addAll(revokedPermissions.get(userGroup));
                }
            }
            // remove conflicts from remove (dont revoke something that is also granted)
            permissionsToRevoke = permissionHandling.eliminateRevokeConflicts(permissionsToGrant, permissionsToRevoke);

            DefaultTreeNode userNode = new DefaultTreeNode(new TreeNodeModel(user.getUsername(), ResourceType.USER, user), currentUserPermissionTreeRoot);
            userNode.setExpanded(true);
            userNode.setSelectable(false);
            createPermissionTree(TreeType.CURRENT, user, userNode, userPermissions, userDataPermissions, permissionsToGrant, permissionsToRevoke);

            DefaultTreeNode newUserNode = new DefaultTreeNode(new TreeNodeModel(user.getUsername(), ResourceType.USER, user), newUserPermissionTreeRoot);
            newUserNode.setExpanded(true);
            newUserNode.setSelectable(false);
            createPermissionTree(TreeType.NEW, user, newUserNode, userPermissions, userDataPermissions, permissionsToGrant, permissionsToRevoke);
            selectedUserPermissionNodes = (TreeNode[]) ArrayUtils.addAll(selectedUserPermissionNodes, getSelectedNodes(newUserNode.getChildren()).toArray());
        }
    }

    private enum TreeType {
        CURRENT,
        NEW;
    }

    private void createPermissionTree(TreeType type, User user, DefaultTreeNode userNode, List<Permission> userPermissions, List<Permission> userDataPermissions, Set<Permission> grantedPermissions, Set<Permission> revokedPermissions) {
        // get permissions which can be revoked from the user
        PermissionChangeSet revokeChangeSet = permissionHandling.getRevokableFromRevoked(userPermissions, revokedPermissions, true);
        Set<Permission> revoked = revokeChangeSet.getRevokes();
        revokables.put(user, revoked);
        dialogBean.setNotRevoked(revokeChangeSet.getUnaffected());

        // get permissions which can be granted to the user
        List<Set<Permission>> grant = permissionHandling.getGrantable(PermissionUtils.removeAll(userPermissions, revoked), grantedPermissions);
        Set<Permission> grantable = grant.get(0);
        dialogBean.setNotGranted(grant.get(1));

        Set<Permission> additionalGranted = revokeChangeSet.getGrants();
        grantable.addAll(additionalGranted);
        grantable = permissionHandling.getNormalizedPermissions(grantable);

        // current permission tree
        Set<Permission> replaced = permissionHandling.getReplacedByGranting(concat(userPermissions, userDataPermissions), grantable);
        replacables.put(user, replaced);
        Set<Permission> removable = new HashSet<Permission>();
        removable.addAll(replaced);
        removable.addAll(revoked);

        switch (type) {
            case CURRENT:
                buildCurrentPermissionTree(userNode, userPermissions, userDataPermissions, revoked, replaced, !isEnabled(Features.FIELD_LEVEL));
                break;
            case NEW:
                buildNewPermissionTree(userNode, userPermissions, userDataPermissions, grantable, revoked, replaced, !isEnabled(Features.FIELD_LEVEL),
                                       isEnabled(Features.USER_LEVEL), true);
                break;

        }

    }
}
