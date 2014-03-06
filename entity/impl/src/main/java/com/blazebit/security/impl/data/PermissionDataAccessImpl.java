/*
 * Copyright 2013 Blazebit.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.blazebit.security.impl.data;

import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.blazebit.security.PermissionUtils;
import com.blazebit.security.data.PermissionDataAccess;
import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * 
 * @author cuszk
 */
@Stateless
public class PermissionDataAccessImpl implements PermissionDataAccess {

    @Inject
    private PermissionManager permissionManager;

    @Override
    public boolean isRevokable(Subject subject, Action action, Resource resource) {
        return !getRevokeImpliedPermissions(subject, action, resource).isEmpty();
    }

    @Override
    public boolean isRevokable(Role role, Action action, Resource resource) {
        return !getRevokeImpliedPermissions(role, action, resource).isEmpty();
    }

    @Override
    public Set<Permission> getRevokeImpliedPermissions(Subject subject, Action action, Resource resource) {
        PermissionUtils.checkParameters(subject);
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return PermissionUtils.getRevokeImpliedPermissions(permissions, action, resource);
    }

    @Override
    public Set<Permission> getRevokeImpliedPermissions(Role role, Action action, Resource resource) {
        PermissionUtils.checkParameters(role);
        List<Permission> permissions = permissionManager.getPermissions(role);
        return PermissionUtils.getRevokeImpliedPermissions(permissions, action, resource);
    }

    @Override
    public boolean isGrantable(Subject subject, Action action, Resource resource) {
        PermissionUtils.checkParameters(subject);
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return PermissionUtils.isGrantable(permissions, action, resource);
    }

    @Override
    public boolean isGrantable(Role role, Action action, Resource resource) {
        PermissionUtils.checkParameters(role);
        List<Permission> permissions = permissionManager.getPermissions(role);
        return PermissionUtils.isGrantable(permissions, action, resource);
    }

    @Override
    public Set<Permission> getImpliedBy(Subject subject, Action action, Resource resource) {
        PermissionUtils.checkParameters(subject);
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return PermissionUtils.getImpliedBy(permissions, action, resource);
    }

    @Override
    public Set<Permission> getGrantImpliedPermissions(Subject subject, Action action, Resource resource) {
        PermissionUtils.checkParameters(subject);
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return PermissionUtils.getReplaceablePermissions(permissions, action, resource);
    }

    @Override
    public Set<Permission> getGrantImpliedPermissions(Role role, Action action, Resource resource) {
        PermissionUtils.checkParameters(role);
        List<Permission> permissions = permissionManager.getPermissions(role);
        return PermissionUtils.getReplaceablePermissions(permissions, action, resource);
    }

    @Override
    public Permission findPermission(Subject subject, Action action, Resource resource) {
        PermissionUtils.checkParameters(subject);
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return PermissionUtils.findPermission(permissions, action, resource);
    }

    @Override
    public Permission findPermission(Role role, Action action, Resource resource) {
        PermissionUtils.checkParameters(role);
        List<Permission> permissions = permissionManager.getPermissions(role);
        return PermissionUtils.findPermission(permissions, action, resource);
    }
}