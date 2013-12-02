package com.blazebit.security.impl.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.service.PermissionHandlingImpl;
import com.blazebit.security.impl.service.resource.EntityFieldResourceHandlingUtils;
import com.blazebit.security.service.api.RoleManager;

public class GroupPermissionHandlingUtils {

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private PermissionHandlingImpl permissionHandling;

    @Inject
    private RoleManager roleManager;

    @Inject
    private EntityFieldResourceHandlingUtils resourceHandling;

    public List<Set<UserGroup>> getAddedAndRemovedUserGroups(User user, Set<UserGroup> selectedGroups) {
        List<Set<UserGroup>> ret = new ArrayList<Set<UserGroup>>();
        Set<UserGroup> added = new HashSet<UserGroup>();
        Set<UserGroup> removed = new HashSet<UserGroup>();
        @SuppressWarnings("unchecked")
        List<UserGroup> groupsForUser = (List<UserGroup>) roleManager.getSubjectRoles(user);
        // List<UserGroup> groupsForUser = userGroupService.getGroupsForUser(user);
        for (UserGroup group : selectedGroups) {

            if (!groupsForUser.contains(group)) {
                added.add(group);
            }
        }

        for (UserGroup group : groupsForUser) {
            if (!selectedGroups.contains(group)) {
                removed.add(group);
            }
        }
        ret.add(added);
        ret.add(removed);
        return ret;
    }

    public Set<Permission> getGroupPermissions(Set<UserGroup> addedGroups) {
        return getGroupPermissions(addedGroups, true);
    }

    public Set<Permission> getGroupPermissions(Set<UserGroup> groups, boolean inherit) {

        Set<Permission> ret = new HashSet<Permission>();
        for (UserGroup group : groups) {
            ret.addAll(permissionManager.getPermissions(group));
            if (inherit) {
                UserGroup parent = group.getParent();
                while (parent != null) {
                    ret.addAll(permissionManager.getPermissions(parent));
                    parent = parent.getParent();
                }
            }
        }
        return permissionHandling.getNormalizedPermissions(ret);
    }

    public Set<Permission> getGroupPermissions(Set<UserGroup> groups, boolean inherit, boolean fieldLevel) {

        Set<Permission> ret = new HashSet<Permission>();
        for (UserGroup group : groups) {
            ret.addAll(!fieldLevel ? permissionManager.getPermissions(group) : permissionHandling.getParentPermissions(permissionManager.getPermissions(group)));
            if (inherit) {
                UserGroup parent = group.getParent();
                while (parent != null) {
                    ret.addAll(!fieldLevel ? permissionManager.getPermissions(group) : permissionHandling.getParentPermissions(permissionManager.getPermissions(group)));
                    parent = parent.getParent();
                }
            }
        }
        return permissionHandling.getNormalizedPermissions(ret);
    }

}
