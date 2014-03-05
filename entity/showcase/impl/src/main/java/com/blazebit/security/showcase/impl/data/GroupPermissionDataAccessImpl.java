package com.blazebit.security.showcase.impl.data;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.blazebit.security.data.PermissionHandling;
import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.showcase.data.GroupPermissionDataAccess;

public class GroupPermissionDataAccessImpl implements GroupPermissionDataAccess {

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private PermissionHandling permissionHandling;

    @Override
    public Set<Permission> getGroupPermissions(Set<UserGroup> groups) {
        return getGroupPermissions(groups, true);
    }

    @Override
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
