/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl.service;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserDataPermissionId;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupDataPermissionId;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserGroupPermissionId;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.model.UserPermissionId;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author cuszk
 */
@Stateless
public class PermissionFactoryImpl implements PermissionFactory {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public <R extends Role<R>, P extends Permission> P create(Subject<R> subject, Action action, Resource resource) {
        if (resource instanceof EntityObjectField) {
            UserDataPermission permission = new UserDataPermission();
            permission.setId(new UserDataPermissionId((User) subject, (EntityObjectField) resource, (EntityAction) action));
            entityManager.persist(permission);
            entityManager.flush();
            return (P) permission;
        } else {
            if (resource instanceof EntityField) {
                UserPermission permission = new UserPermission();
                permission.setId(new UserPermissionId((User) subject, (EntityField) resource, (EntityAction) action));
                entityManager.persist(permission);
                entityManager.flush();
                return (P) permission;
            } else {
                throw new IllegalArgumentException("Not supported resource type, subject type or action type");
            }
        }

    }

    @Override
    public <R extends Role<R>, P extends Permission> P create(Role<R> role, Action action, Resource resource) {
        if (resource instanceof EntityObjectField) {
            UserGroupDataPermission permission = new UserGroupDataPermission();
            permission.setId(new UserGroupDataPermissionId((UserGroup) role, (EntityObjectField) resource, (EntityAction) action));
            entityManager.persist(permission);
            entityManager.flush();
            return (P) permission;
        } else {
            if (resource instanceof EntityField) {
                UserGroupPermission permission = new UserGroupPermission();
                permission.setId(new UserGroupPermissionId((UserGroup) role, (EntityField) resource, (EntityAction) action));
                entityManager.persist(permission);
                entityManager.flush();
                return (P) permission;

            } else {
                throw new IllegalArgumentException("Not supported resource type, role type or action type");
            }
        }
    }
}
