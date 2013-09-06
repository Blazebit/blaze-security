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
package com.blazebit.security.impl;

import com.blazebit.security.Action;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.service.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.service.RoleSecurityService;
import com.blazebit.security.SecurityActionException;
import com.blazebit.security.service.SecurityService;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.impl.utils.EntityUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author cuszk
 */
@Stateless
public class RoleSecurityServiceImpl implements RoleSecurityService {

    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private PermissionDataAccess permissionDataAccess;
    @PersistenceContext
    private EntityManager entityManager;
    @Inject
    private SecurityService securityService;

    @Override
    public boolean isGranted(Role role, Action action, Resource resource) {
        role = entityManager.find(role.getClass(), ((IdHolder) role).getId());
        for (Object _permission : role.getAllPermissions()) {
            Permission permission = (Permission) _permission;
            if (permission.getAction().matches(action) && permission.getResource().matches(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <R extends Role<R>> void grant(Subject<R> authorizer, Role role, Action action, Resource resource) {
        role = entityManager.find(role.getClass(), ((IdHolder) role).getId());
        if (!securityService.isGranted(authorizer, getGrantAction(), EntityUtils.getEntityObjectFieldFor(role))) {
            throw new SecurityException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!permissionDataAccess.isGrantable(role, action, resource)) {
            throw new SecurityActionException("Permission for " + role + ", " + action + "," + resource + " cannot be granted");
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenGranting(role, action, resource);
        for (Permission existingPermission : removablePermissions) {
            entityManager.remove(existingPermission);
        }
        Permission permission = permissionFactory.create(role, action, resource);
        entityManager.persist(permission);
        entityManager.flush();
        //propagate changes to users
        propagateGrantToSubjects(authorizer, role, action, resource);
    }

    private void propagateGrantToSubjects(Subject authorizer, Role root, Action action, Resource resource) {
        //grant permission to all users of this role
        Collection<Subject> subjects = root.getSubjects();
        for (Subject subject : subjects) {
            securityService.grant(authorizer, subject, action, resource);
        }
        if (!root.getRoles().isEmpty()) {
            Collection<Role> children = root.getRoles();
            for (Role child : children) {
                propagateGrantToSubjects(authorizer, child, action, resource);
            }
        }
    }

    @Override
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Role subject, Action action, Resource resource) {
        subject = entityManager.find(subject.getClass(), ((IdHolder) subject).getId());
        if (!securityService.isGranted(authorizer, getRevokeAction(), EntityUtils.getEntityObjectFieldFor(subject))) {
            throw new SecurityException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!permissionDataAccess.isRevokable(subject, action, resource)) {
            throw new SecurityActionException("Permission : " + subject + ", " + action + ", "
                    + resource + " cannot be revoked");
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenRevoking(subject, action, resource);
        for (Permission permission : removablePermissions) {
            entityManager.remove(permission);
        }
        entityManager.flush();
        propagateRevokeToSubjects(authorizer, subject, action, resource);  
    }

    private void propagateRevokeToSubjects(Subject authorizer, Role root, Action action, Resource resource) {
        //grant permission to all users of this role
        Collection<Subject> subjects = root.getSubjects();
        for (Subject subject : subjects) {
            securityService.revoke(authorizer, subject, action, resource);
        }
        if (!root.getRoles().isEmpty()) {
            Collection<Role> children = root.getRoles();
            for (Role child : children) {
                propagateRevokeToSubjects(authorizer, child, action, resource);
            }
        }
    }

    public Action getGrantAction() {
        return ActionUtils.getGrantAction();
    }

    public Action getRevokeAction() {
        return ActionUtils.getRevokeAction();
    }
}
