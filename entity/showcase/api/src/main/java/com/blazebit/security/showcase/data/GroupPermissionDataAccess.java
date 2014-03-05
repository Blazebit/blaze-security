package com.blazebit.security.showcase.data;

import java.util.Set;

import com.blazebit.security.model.Permission;
import com.blazebit.security.model.UserGroup;

public interface GroupPermissionDataAccess {

    public Set<Permission> getGroupPermissions(Set<UserGroup> groups);

    public Set<Permission> getGroupPermissions(Set<UserGroup> groups, boolean inherit);

}