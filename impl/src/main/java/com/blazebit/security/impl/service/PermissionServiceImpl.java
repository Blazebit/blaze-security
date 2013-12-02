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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.deltaspike.core.util.ServiceUtils;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionActionException;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Resource;
import com.blazebit.security.ResourceFactory;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.service.api.PropertyDataAccess;
import com.blazebit.security.spi.ActionImplicationProvider;

/**
 * 
 * @author cuszk
 */
@Stateless
public class PermissionServiceImpl extends PermissionCheckBase implements PermissionService {

    @Inject
    private PermissionFactory permissionFactory;

    @Inject
    private ResourceFactory resourceFactory;
    @Inject
    private ActionFactory actionFactory;

    @Inject
    private PermissionDataAccess permissionDataAccess;

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private PermissionHandlingImpl permissionHandlingUtils;

    @Inject
    private PropertyDataAccess propertyDataAccess;

    private List<ActionImplicationProvider> actionImplicationProviders;

    @PostConstruct
    private void init() {
        actionImplicationProviders = ServiceUtils.loadServiceImplementations(ActionImplicationProvider.class);
    }

    @Override
    public boolean isGranted(Subject subject, Action action, Resource resource) {
        checkParameters(subject, action, resource);
        List<Permission> permissions = permissionManager.getPermissions(subject);
        for (Permission permission : permissions) {
            if (permission.getAction().implies(action) && permission.getResource().implies(resource)) {
                return true;
            }
        }
        // if implying permission not found, try with implying action
        for (ActionImplicationProvider provider : actionImplicationProviders) {
            List<Action> actions = provider.getActionsWhichImply(action, Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.FIELD_LEVEL)));
            for (Action implyAction : actions) {
                for (Permission permission : permissions) {
                    if (permission.getAction().implies(implyAction) && permission.getResource().implies(resource)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void grant(Subject authorizer, Subject subject, Action action, Resource resource) throws PermissionException, PermissionActionException {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        checkParameters(subject, action, resource);
        if (!isGranted(authorizer, getGrantAction(), resourceFactory.createResource(subject))) {
            throw new PermissionException(authorizer + " is not allowed to grant " + action + " to " + subject);
        }
        // TODO currently grant action to action not checked
        // if (!isGranted(authorizer, getGrantAction(), entityFieldFactory.createResource(action))) {
        // throw new PermissionException(authorizer + " is not allowed to grant " + getGrantAction() + " to " + action);
        // }
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
    public void revoke(Subject authorizer, Subject subject, Action action, Resource resource) throws PermissionException, PermissionActionException {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        checkParameters(subject, action, resource);
        if (!isGranted(authorizer, getRevokeAction(), resourceFactory.createResource(subject))) {
            throw new PermissionException(authorizer + " is not allowed to revoke from " + subject);
        }
        // TODO currently revoke action to action not checked
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
        checkParameters(role, action, resource);
        List<Permission> permissions = permissionManager.getPermissions(role);
        for (Permission permission : permissions) {
            if (permission.getAction().implies(action) && permission.getResource().implies(resource)) {
                return true;
            }
        }
        // if implying permission not found, try with implying action
        for (ActionImplicationProvider provider : actionImplicationProviders) {
            List<Action> actions = provider.getActionsWhichImply(action);
            for (Action implyAction : actions) {
                for (Permission permission : permissions) {
                    if (permission.getAction().implies(implyAction) && permission.getResource().implies(resource)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void grant(Subject authorizer, Role role, Action action, Resource resource) throws PermissionException, PermissionActionException {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        checkParameters(role, action, resource);
        grant(authorizer, role, action, resource, false);
    }

    @Override
    public void grant(Subject authorizer, Role role, Action action, Resource resource, boolean propagateToUsers) throws PermissionException, PermissionActionException {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        checkParameters(role, action, resource);
        if (!isGranted(authorizer, getGrantAction(), resourceFactory.createResource(role))) {
            throw new PermissionException(authorizer + " is not allowed to " + getGrantAction() + " to " + resource);
        }
        // TODO currently grant action to action not checked
        // if (!isGranted(authorizer, getGrantAction(), resourceFactory.createResource(getGrantAction()))) {
        // throw new PermissionException(authorizer + " is not allowed to grant " + action + " to " + resource);
        // }
        if (!permissionDataAccess.isGrantable(role, action, resource)) {
            throw new PermissionActionException("Permission for " + role + ", " + action + "," + resource + " cannot be granted");
        }

        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenGranting(role, action, resource);
        for (Permission existingPermission : removablePermissions) {
            permissionManager.remove(existingPermission);
        }
        Permission permission = permissionFactory.create(role, action, resource);
        permissionManager.save(permission);
        // propagate changes to users
        if (propagateToUsers) {
            propagateGrantToSubjects(authorizer, role, action, resource);
        }
        permissionManager.flush();
    }

    private void propagateGrantToSubjects(Subject authorizer, Role root, Action action, Resource resource) {
        // grant permission to all users of this role
        Collection<Subject> subjects = root.getSubjects();
        for (Subject subject : subjects) {
            // take care of merging permissions with the existing permissions

            // first: check if permission to be granted is grantable to subject
            Set<Permission> toBeGranted = new HashSet<Permission>();
            toBeGranted.add(permissionFactory.create(subject, action, resource));
            List<Permission> currentPermissions = permissionManager.getPermissions(subject);
            // second: check if permission can be merged with existing ones
            Set<Permission> grant = permissionHandlingUtils.getGrantable(currentPermissions, toBeGranted).get(0);
            List<Set<Permission>> revokeAndGrant = permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, new HashSet<Permission>(), grant);
            Set<Permission> revoke = revokeAndGrant.get(0);
            grant = revokeAndGrant.get(1);
            // merging might need revoke and grant operations
            for (Permission permission : revoke) {
                revoke(authorizer, subject, permission.getAction(), permission.getResource());
            }
            for (Permission permission : grant) {
                grant(authorizer, subject, permission.getAction(), permission.getResource());
            }

        }
        Collection<Role> children = root.getRoles();
        if (!children.isEmpty()) {
            for (Role child : children) {
                propagateGrantToSubjects(authorizer, child, action, resource);
            }
        }
    }

    @Override
    public void revoke(Subject authorizer, Role role, Action action, Resource resource) throws PermissionException, PermissionActionException {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        checkParameters(role, action, resource);
        revoke(authorizer, role, action, resource, false);
    }

    @Override
    public void revoke(Subject authorizer, Role role, Action action, Resource resource, boolean propagateToUsers) throws PermissionException, PermissionActionException {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        checkParameters(role, action, resource);
        if (!isGranted(authorizer, getRevokeAction(), resourceFactory.createResource(role))) {
            throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        // TODO currently revoke action to action not checked
        // if (!isGranted(authorizer, getRevokeAction(), resourceFactory.createResource(getRevokeAction()))) {
        // throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        // }
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
        // propagate changes to users
        if (propagateToUsers) {
            propagateRevokeToSubjects(authorizer, role, action, resource);
        }
        permissionManager.flush();
    }

    private void propagateRevokeToSubjects(Subject authorizer, Role root, Action action, Resource resource) {
        // revoke permission from all users of this role
        Collection<Subject> subjects = root.getSubjects();
        for (Subject subject : subjects) {
            // take care of merging permissions with the existing permissions

            // first: check if permission to be granted is grantable to subject
            Set<Permission> toBeRevoked = new HashSet<Permission>();
            toBeRevoked.add(permissionFactory.create(subject, action, resource));
            List<Permission> currentPermissions = permissionManager.getPermissions(subject);
            // second: check if permission can be merged with existing ones
            Set<Permission> revoke = permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, toBeRevoked, true).get(0);
            Set<Permission> grant = permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, toBeRevoked, true).get(2);
            // merging might need revoke and grant operations
            for (Permission permission : revoke) {
                revoke(authorizer, subject, permission.getAction(), permission.getResource());
            }
            for (Permission permission : grant) {
                grant(authorizer, subject, permission.getAction(), permission.getResource());
            }
        }
        Collection<Role> children = root.getRoles();
        if (!children.isEmpty()) {
            for (Role child : children) {
                propagateRevokeToSubjects(authorizer, child, action, resource);
            }
        }
    }

    // @Override
    // public Collection<Action> getAllowedActions(Subject subject, Resource resource) throws PermissionException,
    // PermissionActionException {
    // Set<Action> actions = new HashSet<Action>();
    //
    // for (Permission permission : permissionManager.getPermissions(subject)) {
    // if (permission.getResource().implies(resource)) {
    // actions.add(permission.getAction());
    // }
    // }
    // return actions;
    // }

    private Action getGrantAction() {
        return actionFactory.createAction(ActionConstants.GRANT);
    }

    private Action getRevokeAction() {
        return actionFactory.createAction(ActionConstants.REVOKE);
    }

}
