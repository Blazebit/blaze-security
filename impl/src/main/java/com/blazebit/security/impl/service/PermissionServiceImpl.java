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
package com.blazebit.security.impl.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionActionException;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.RoleManager;
import com.blazebit.security.Subject;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;

/**
 * 
 * @author cuszk
 */
@Stateless
public class PermissionServiceImpl implements PermissionService {

    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private EntityFieldFactory entityFieldFactory;
    @Inject
    private ActionFactory actionFactory;

    @Inject
    private PermissionDataAccess permissionDataAccess;

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private RoleManager roleManager;

    @Override
    public <R extends Role<R>> boolean isGranted(Subject<R> subject, Action action, Resource resource) {
        Subject _subject = permissionManager.reloadSubjectWithPermissions(subject);
        List<Permission> permissions = new ArrayList<Permission>(_subject.getAllPermissions());
        // permissionManager.getAllPermissions(subject);
        // TODO alternative to fetch all the permissions. problem with getAllPermissions method that it invokes a flush before
        // the
        // query. When this is invoked from the flush interceptor it causes to invoke the flush interceptor again and again.
        // See forum: https://hibernate.atlassian.net/browse/HB-1480, https://forum.hibernate.org/viewtopic.php?t=955313
        for (Permission permission : permissions) {
            if (permission.getAction().implies(action) && permission.getResource().implies(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <R extends Role<R>> void grant(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource) throws PermissionException, PermissionActionException {
        if (!isGranted(authorizer, getGrantAction(), entityFieldFactory.createResource(subject))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + action + " to " + subject);
        }
        // TODO currently grant action to action not checked
        // if (!isGranted(authorizer, getGrantAction(), entityFieldFactory.createResource(action))) {
        // throw new PermissionException(authorizer + " is not allowed to grant " + getGrantAction() + " to " + action);
        // }
        if (!isGranted(authorizer, getCreateAction(), entityFieldFactory.createResource(UserPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getDeleteAction(), entityFieldFactory.createResource(UserPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getCreateAction(), entityFieldFactory.createResource(UserDataPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getDeleteAction(), entityFieldFactory.createResource(UserDataPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getGrantAction(), resource)) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!permissionDataAccess.isGrantable(subject, action, resource)) {
            throw new PermissionActionException("Permission for " + subject + ", " + action + "," + resource + " cannot be granted");
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenGranting(subject, action, resource);
        for (Permission existingPermission : removablePermissions) {
            permissionManager.remove(existingPermission);
        }
        Permission permission = permissionFactory.create(subject, action, resource);
        permissionManager.save(permission);
        permissionManager.flush();
    }

    @Override
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource) throws PermissionException, PermissionActionException {
        if (!isGranted(authorizer, getRevokeAction(), entityFieldFactory.createResource(subject))) {
            throw new PermissionException(authorizer + " is not allowed to revoke from " + subject);
        }
        if (!isGranted(authorizer, getRevokeAction(), resource)) {
            throw new PermissionException(authorizer + " is not allowed to revoke " + resource);
        }
        if (!isGranted(authorizer, getCreateAction(), entityFieldFactory.createResource(UserPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getDeleteAction(), entityFieldFactory.createResource(UserDataPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getCreateAction(), entityFieldFactory.createResource(UserDataPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getDeleteAction(), entityFieldFactory.createResource(UserPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        // if (!isGranted(authorizer, getRevokeAction(), entityFieldFactory.createResource(getRevokeAction()))) {
        // throw new PermissionException(authorizer + " is not allowed to " + getRevokeAction() + " to " + getRevokeAction());
        // }
        if (!permissionDataAccess.isRevokable(subject, action, resource)) {
            throw new PermissionActionException("Permission : " + subject + ", " + action + ", " + resource + " cannot be revoked");
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenRevoking(subject, action, resource);
        for (Permission permission : removablePermissions) {
            permissionManager.remove(permission);
        }
        permissionManager.flush();
    }

    @Override
    public boolean isGranted(Role role, Action action, Resource resource) {
        for (Permission permission : permissionManager.getAllPermissions(role)) {
            if (permission.getAction().implies(action) && permission.getResource().implies(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <R extends Role<R>> void grant(Subject<R> authorizer, Role role, Action action, Resource resource) throws PermissionException, PermissionActionException {
        grant(authorizer, role, action, resource, false);
    }

    @Override
    public <R extends Role<R>> void grant(Subject<R> authorizer, Role role, Action action, Resource resource, boolean propagateToUsers) throws PermissionException,
        PermissionActionException {
        if (!isGranted(authorizer, getGrantAction(), entityFieldFactory.createResource(role))) {
            throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!permissionDataAccess.isGrantable(role, action, resource)) {
            throw new PermissionActionException("Permission for " + role + ", " + action + "," + resource + " cannot be granted");
        }
        if (!isGranted(authorizer, getCreateAction(), entityFieldFactory.createResource(UserGroupPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getDeleteAction(), entityFieldFactory.createResource(UserGroupPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getCreateAction(), entityFieldFactory.createResource(UserGroupDataPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        if (!isGranted(authorizer, getDeleteAction(), entityFieldFactory.createResource(UserGroupDataPermission.class))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + resource);
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenGranting(role, action, resource);
        for (Permission existingPermission : removablePermissions) {
            permissionManager.remove(existingPermission);
        }
        Permission permission = permissionFactory.create(role, action, resource);
        permissionManager.save(permission);
        permissionManager.flush();
        // propagate changes to users
        if (propagateToUsers) {
            propagateGrantToSubjects(authorizer, role, action, resource);
        }
    }

    private void propagateGrantToSubjects(Subject authorizer, Role root, Action action, Resource resource) {
        // grant permission to all users of this role
        Collection<Subject> subjects = roleManager.getSubjects(root);
        for (Subject subject : subjects) {
            grant(authorizer, subject, action, resource);
        }
        List<Role> children = roleManager.getRoles(root);
        if (!children.isEmpty()) {
            for (Role child : children) {
                propagateGrantToSubjects(authorizer, child, action, resource);
            }
        }
    }

    @Override
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Role role, Action action, Resource resource) throws PermissionException, PermissionActionException {
        revoke(authorizer, role, action, resource, false);
    }

    @Override
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Role role, Action action, Resource resource, boolean propagateToUsers) throws PermissionException,
        PermissionActionException {
        if (!isGranted(authorizer, getRevokeAction(), entityFieldFactory.createResource(role))) {
            throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!isGranted(authorizer, getRevokeAction(), entityFieldFactory.createResource(getRevokeAction()))) {
            throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!isGranted(authorizer, getRevokeAction(), resource)) {
            throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!permissionDataAccess.isRevokable(role, action, resource)) {
            throw new PermissionActionException("Permission : " + role + ", " + action + ", " + resource + " cannot be revoked");
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenRevoking(role, action, resource);
        for (Permission permission : removablePermissions) {
            permissionManager.remove(permission);
        }
        permissionManager.flush();
        propagateRevokeToSubjects(authorizer, role, action, resource);
    }

    private void propagateRevokeToSubjects(Subject authorizer, Role root, Action action, Resource resource) {
        // revoke permission from all users of this role
        Collection<Subject> subjects = roleManager.getSubjects(root);
        for (Subject subject : subjects) {
            revoke(authorizer, subject, action, resource);
        }
        Collection<Role> children = roleManager.getRoles(root);
        if (!children.isEmpty()) {
            for (Role child : children) {
                propagateRevokeToSubjects(authorizer, child, action, resource);
            }
        }
    }

    @Override
    public <R extends Role<R>> Collection<Action> getAllowedActions(Subject<R> subject, Resource resource) throws PermissionException, PermissionActionException {
        Set<Action> actions = new HashSet<Action>();

        for (Permission permission : permissionManager.getAllPermissions(subject)) {
            if (permission.getResource().implies(resource)) {
                actions.add(permission.getAction());
            }
        }
        return actions;
    }

    @Override
    public Action getGrantAction() {
        return actionFactory.createAction(ActionConstants.GRANT);
    }

    @Override
    public Action getCreateAction() {
        return actionFactory.createAction(ActionConstants.CREATE);
    }

    @Override
    public Action getDeleteAction() {
        return actionFactory.createAction(ActionConstants.DELETE);
    }

    @Override
    public Action getRevokeAction() {
        return actionFactory.createAction(ActionConstants.REVOKE);
    }

}