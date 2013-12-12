package com.blazebit.security.impl.service.resource;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.service.PermissionHandlingImpl;

public class GroupPermissionHandling {

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private PermissionHandlingImpl permissionHandling;

    public Set<Permission> getGroupPermissions(Set<UserGroup> groups) {
        return getGroupPermissions(groups, true);
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

}