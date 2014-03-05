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
package com.blazebit.security.integration.core;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.EntityAction;
import com.blazebit.security.model.EntityField;
import com.blazebit.security.model.EntityObjectField;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserDataPermission;
import com.blazebit.security.model.UserDataPermissionId;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.model.UserGroupDataPermission;
import com.blazebit.security.model.UserGroupDataPermissionId;
import com.blazebit.security.model.UserGroupPermission;
import com.blazebit.security.model.UserGroupPermissionId;
import com.blazebit.security.model.UserPermission;
import com.blazebit.security.model.UserPermissionId;
import com.blazebit.security.spi.PermissionFactory;

/**
 * 
 * @author cuszk
 * 
 */
public class PermissionFactoryImpl implements PermissionFactory {

    @Override
    public Permission create(Action action, Resource resource) {
        // TODO: Think of something better here, returning user permission is a very bad idea!!
        checkParameters(action, resource);
        if (resource instanceof EntityObjectField) {
            UserDataPermission permission = new UserDataPermission();
            permission.setId(new UserDataPermissionId(null, (EntityObjectField) resource, (EntityAction) action));
            return permission;
        } else if (resource instanceof EntityField) {
            UserPermission permission = new UserPermission();
            permission.setId(new UserPermissionId(null, (EntityField) resource, (EntityAction) action));
            return permission;
        } else {
            throw new IllegalArgumentException("Not supported resource type!!");
        }

    }

    @Override
    public Permission create(Subject subject, Action action, Resource resource) {
        checkParameters(subject, action, resource);
        if (resource instanceof EntityObjectField) {
            UserDataPermission permission = new UserDataPermission();
            permission.setId(new UserDataPermissionId((User) subject, (EntityObjectField) resource, (EntityAction) action));
            return permission;
        } else if (resource instanceof EntityField) {
            UserPermission permission = new UserPermission();
            permission.setId(new UserPermissionId((User) subject, (EntityField) resource, (EntityAction) action));
            return permission;
        } else {
            throw new IllegalArgumentException("Not supported resource type!!");
        }
    }

    @Override
    public Permission create(Role role, Action action, Resource resource) {
        checkParameters(role, action, resource);
        if (resource instanceof EntityObjectField) {
            UserGroupDataPermission permission = new UserGroupDataPermission();
            permission.setId(new UserGroupDataPermissionId((UserGroup) role, (EntityObjectField) resource,
                (EntityAction) action));
            return permission;
        } else if (resource instanceof EntityField) {
            UserGroupPermission permission = new UserGroupPermission();
            permission.setId(new UserGroupPermissionId((UserGroup) role, (EntityField) resource, (EntityAction) action));
            return permission;
        } else {
            throw new IllegalArgumentException("Not supported resource type!!");
        }
    }

    private static void checkParameters(Subject subject, Action action, Resource resource) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null!");
        }
        checkParameters(action, resource);
    }

    private static void checkParameters(Role role, Action action, Resource resource) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        checkParameters(action, resource);
    }

    private static void checkParameters(Action action, Resource resource) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null!");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null!");
        }
    }

}
