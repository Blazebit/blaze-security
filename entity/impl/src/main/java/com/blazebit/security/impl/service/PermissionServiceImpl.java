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

import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.blazebit.security.PermissionUtils;
import com.blazebit.security.data.PermissionDataAccess;
import com.blazebit.security.data.PermissionHandling;
import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.entity.UserContext;
import com.blazebit.security.exception.PermissionActionException;
import com.blazebit.security.exception.PermissionException;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.PermissionChangeSet;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;
import com.blazebit.security.service.PermissionService;
import com.blazebit.security.spi.ActionFactory;
import com.blazebit.security.spi.ActionImplicationProvider;
import com.blazebit.security.spi.PermissionFactory;
import com.blazebit.security.spi.ResourceFactory;

/**
 * 
 * @author cuszk
 */
@Stateless
public class PermissionServiceImpl implements PermissionService {
    
    private static final boolean DEFAULT_PROPAGATE_TO_SUBJECTS = false;
    private static final boolean DEFAULT_FORCE_REVOKE = false;

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
    private PermissionHandling permissionHandling;

    @Inject
    private Instance<ActionImplicationProvider> actionImplicationProviders;

    @Inject
    private UserContext userContext;

    @Override
    public boolean isGranted(Action action, Resource resource) {
        return isGranted(userContext.getUser(), action, resource);
    }

    @Override
    public boolean isGranted(Subject subject, Action action, Resource resource) {
        PermissionUtils.checkParameters(subject, action, resource);
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return isGranted(permissions, action, resource);
    }

    @Override
    public boolean isGranted(Role role, Action action, Resource resource) {
        PermissionUtils.checkParameters(role, action, resource);
        List<Permission> permissions = permissionManager.getPermissions(role);
        return isGranted(permissions, action, resource);
    }

    private boolean isGranted(List<Permission> permissions, Action action, Resource resource) {
        if (PermissionUtils.checkParameters(permissions)) {
            return false;
        }

        Set<Action> actions = new HashSet<Action>();
        actions.add(action);

        for (ActionImplicationProvider provider : actionImplicationProviders) {
            actions.addAll(provider.getActionsWhichImply(action));
        }

        return PermissionUtils.impliedByAny(permissions, actions, resource);
    }

    @Override
    public void grant(Subject authorizer, Subject subject, Permission permission) {
        PermissionUtils.checkParameters(permission);
        grant(authorizer, subject, permission.getAction(), permission.getResource());
    }

    @Override
    public void grant(Subject authorizer, Subject subject, Set<Permission> permissions) {
        if (PermissionUtils.checkParameters(permissions)) {
            return;
        }
        for (Permission permission : permissions) {
            grant(authorizer, subject, permission.getAction(), permission.getResource());
        }
    }

    @Override
    public void grant(Subject authorizer, Role role, Permission permission) {
        grant(authorizer, role, permission, DEFAULT_PROPAGATE_TO_SUBJECTS);
    }

    @Override
    public void grant(Subject authorizer, Role role, Set<Permission> permissions) {
        grant(authorizer, role, permissions, DEFAULT_PROPAGATE_TO_SUBJECTS);
    }

    @Override
    public void grant(Subject authorizer, Role role, Permission permission, boolean propagateToSubjects) {
        PermissionUtils.checkParameters(permission);
        grant(authorizer, role, permission.getAction(), permission.getResource(), propagateToSubjects);
    }

    @Override
    public void grant(Subject authorizer, Role role, Set<Permission> permissions, boolean propagateToSubjects) {
        if (PermissionUtils.checkParameters(permissions)) {
            return;
        }
        for (Permission permission : permissions) {
            grant(authorizer, role, permission.getAction(), permission.getResource(), propagateToSubjects);
        }
    }

    @Override
    public void grant(Subject authorizer, Subject subject, Action action, Resource resource) {
        // Check parameters
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        if (authorizer.equals(subject)) {
            throw new IllegalArgumentException("Authorizer and subject cannot be the same");
        }
        PermissionUtils.checkParameters(subject, action, resource);

        // Check if authorizer is allowed to grant
        if (!isGranted(authorizer, getGrantAction(), resourceFactory.createResource(subject))) {
            throw new PermissionException(authorizer + " is not allowed to grant anything to the subject " + subject);
        }
        if (!isGranted(authorizer, getGrantAction(), resourceFactory.createResource(action))) {
            throw new PermissionException(authorizer + " is not allowed to grant the action " + action + " to anyone");
        }
        if (!isGranted(authorizer, getGrantAction(), resource)) {
            throw new PermissionException(authorizer + " is not allowed to grant the resource " + resource + " to anyone");
        }
        
        // Check if the permission can be applied to the subject
        if (!permissionDataAccess.isGrantable(subject, action, resource)) {
            throw new PermissionActionException("Permission for " + subject + ", " + action + "," + resource + " cannot be granted");
        }
        
        // Remove redundant becoming permissions
        Set<Permission> removablePermissions = permissionDataAccess.getGrantImpliedPermissions(subject, action, resource);
        for (Permission existingPermission : removablePermissions) {
            permissionManager.remove(existingPermission);
        }
        
        // Save new permission
        Permission permission = permissionFactory.create(subject, action, resource);
        permissionManager.save(permission);
    }

    @Override
    public void grant(Subject authorizer, Role role, Action action, Resource resource) {
        grant(authorizer, role, action, resource, DEFAULT_PROPAGATE_TO_SUBJECTS);
    }

    @Override
    public void grant(Subject authorizer, Role role, Action action, Resource resource, boolean propagateToSubjects) {
        // Check parameters
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        PermissionUtils.checkParameters(role, action, resource);

        // Check if authorizer is allowed to grant
        if (!isGranted(authorizer, getGrantAction(), resourceFactory.createResource(role))) {
            throw new PermissionException(authorizer + " is not allowed to grant anything to the role " + role);
        }
        if (!isGranted(authorizer, getGrantAction(), resourceFactory.createResource(action))) {
            throw new PermissionException(authorizer + " is not allowed to grant the action " + action + " to anyone");
        }
        if (!isGranted(authorizer, getGrantAction(), resource)) {
            throw new PermissionException(authorizer + " is not allowed to grant the resource " + resource + " to anyone");
        }

        // Check if the permission can be applied to the role
        if (!permissionDataAccess.isGrantable(role, action, resource)) {
            throw new PermissionActionException("Permission for " + role + ", " + action + "," + resource + " cannot be granted");
        }

        // Remove redundant becoming permissions
        Set<Permission> removablePermissions = permissionDataAccess.getGrantImpliedPermissions(role, action, resource);
        for (Permission existingPermission : removablePermissions) {
            permissionManager.remove(existingPermission);
        }

        // Save new permission
        Permission permission = permissionFactory.create(role, action, resource);
        permissionManager.save(permission);
        
        // Propagate changes to subjects
        if (propagateToSubjects) {
            propagateGrantToSubjects(authorizer, role, action, resource);
        }
    }

    @Override
    public void revoke(Subject authorizer, Subject subject, Permission permission) {
        PermissionUtils.checkParameters(permission);
        revoke(authorizer, subject, permission.getAction(), permission.getResource());
    }

    @Override
    public void revoke(Subject authorizer, Subject subject, Set<Permission> permissions) {
        if (PermissionUtils.checkParameters(permissions)) {
            return;
        }
        for (Permission permission : permissions) {
            revoke(authorizer, subject, permission.getAction(), permission.getResource());
        }
    }

    @Override
    public void revoke(Subject authorizer, Role role, Permission permission) {
        revoke(authorizer, role, permission, DEFAULT_PROPAGATE_TO_SUBJECTS);
    }

    @Override
    public void revoke(Subject authorizer, Role role, Set<Permission> permissions) {
        revoke(authorizer, role, permissions, DEFAULT_PROPAGATE_TO_SUBJECTS);
    }

    @Override
    public void revoke(Subject authorizer, Role role, Permission permission, boolean propagateToSubjects) {
        PermissionUtils.checkParameters(permission);
        revoke(authorizer, role, permission.getAction(), permission.getResource(), propagateToSubjects);
    }

    @Override
    public void revoke(Subject authorizer, Role role, Set<Permission> permissions, boolean propagateToSubjects) {
        if (PermissionUtils.checkParameters(permissions)) {
            return;
        }
        for (Permission permission : permissions) {
            revoke(authorizer, role, permission.getAction(), permission.getResource(), propagateToSubjects);
        }
    }

    @Override
    public void revoke(Subject authorizer, Subject subject, Action action, Resource resource) {
        revoke(authorizer, subject, action, resource, DEFAULT_FORCE_REVOKE);
    }

    @Override
    public void revoke(Subject authorizer, Subject subject, Action action, Resource resource, boolean force) {
        // Check parameters
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        if (authorizer.equals(subject)) {
            throw new IllegalArgumentException("Authorizer and subject cannot be the same");
        }
        PermissionUtils.checkParameters(subject, action, resource);

         // Check if authorizer is allowed to revoke
         if (!isGranted(authorizer, getRevokeAction(), resourceFactory.createResource(subject))) {
             throw new PermissionException(authorizer + " is not allowed to revoke anything from the subject " + subject);
         }
         if (!isGranted(authorizer, getRevokeAction(), resourceFactory.createResource(action))) {
             throw new PermissionException(authorizer + " is not allowed to revoke the action " + action + " from anyone");
         }
         if (!isGranted(authorizer, getRevokeAction(), resource)) {
             throw new PermissionException(authorizer + " is not allowed to revoke the resource " + resource + " from anyone");
         }
        
        // Check if the permission can be removed from the subject
        if (!permissionDataAccess.isRevokable(subject, action, resource)) {
            if (!force) {
                throw new PermissionActionException("Permission : " + subject + ", " + action + ", " + resource + " cannot be revoked");
            }
            
            // TODO: Needs review, use PermissionChangeSet
            
            // give it another try by revoking implying resources and granting permissions
            Set<Permission> toBeRevoked = new HashSet<Permission>();
            toBeRevoked.add(permissionFactory.create(subject, action, resource));
            List<Permission> currentPermissions = permissionManager.getPermissions(subject);
            PermissionChangeSet changeSet = permissionHandling.getRevokableFromRevoked(currentPermissions, toBeRevoked, true);

            if (changeSet.getRevokes().isEmpty()) {
                throw new PermissionActionException("Permission : " + subject + ", " + action + ", " + resource + " cannot be revoked");
            } else {
                revokeAndGrant(authorizer, subject, changeSet.getRevokes(), changeSet.getGrants());
            }
        } else {
            // Remove redundant becoming permissions along with the intended permission
            Set<Permission> removablePermissions = permissionDataAccess.getRevokeImpliedPermissions(subject, action, resource);
            for (Permission permission : removablePermissions) {
                permissionManager.remove(permission);
            }
        }
    }

    @Override
    public void revoke(Subject authorizer, Role role, Action action, Resource resource) {
        revoke(authorizer, role, action, resource, DEFAULT_PROPAGATE_TO_SUBJECTS);
    }

    @Override
    public void revoke(Subject authorizer, Role role, Action action, Resource resource, boolean propagateToSubjects) {
        revoke(authorizer, role, action, resource, propagateToSubjects, DEFAULT_FORCE_REVOKE);
    }

    @Override
    public void revoke(Subject authorizer, Role role, Action action, Resource resource, boolean propagateToSubjects, boolean force) {
        // Check parameters
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        PermissionUtils.checkParameters(role, action, resource);

        // Check if authorizer is allowed to revoke
        if (!isGranted(authorizer, getRevokeAction(), resourceFactory.createResource(role))) {
            throw new PermissionException(authorizer + " is not allowed to revoke anything from the role " + role);
        }
        if (!isGranted(authorizer, getRevokeAction(), resourceFactory.createResource(action))) {
            throw new PermissionException(authorizer + " is not allowed to revoke the action " + action + " from anyone");
        }
        if (!isGranted(authorizer, getRevokeAction(), resource)) {
            throw new PermissionException(authorizer + " is not allowed to revoke the resource " + resource + " from anyone");
        }
        
        // Check if the permission can be removed from the role
        if (!permissionDataAccess.isRevokable(role, action, resource)) {
            if (!force) {
                throw new PermissionActionException("Permission : " + role + ", " + action + ", " + resource + " cannot be revoked");
            }
            
            // TODO: Needs review, use PermissionChangeSet

            // give it another try by revoking implying resources and granting permissions
            Set<Permission> toBeRevoked = new HashSet<Permission>();
            toBeRevoked.add(permissionFactory.create(role, action, resource));
            List<Permission> currentPermissions = permissionManager.getPermissions(role);
            PermissionChangeSet changeSet = permissionHandling.getRevokableFromRevoked(currentPermissions, toBeRevoked, true);

            if (!changeSet.getRevokes().isEmpty()) {
                revokeAndGrant(authorizer, role, changeSet.getRevokes(), changeSet.getGrants());
            } else {
                throw new PermissionActionException("Permission : " + role + ", " + action + ", " + resource
                    + " cannot be revoked");
            }
        } else {
            // Remove redundant becoming permissions along with the intended permission
            Set<Permission> removablePermissions = permissionDataAccess.getRevokeImpliedPermissions(role, action, resource);
            for (Permission permission : removablePermissions) {
                permissionManager.remove(permission);
            }
        }
        
        // Propagate changes to subjects
        if (propagateToSubjects) {
            propagateRevokeToSubjects(authorizer, role, action, resource, force);
        }
    }
    
    // TODO: Start next review here

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
            // TODO: Use permission change set
            Set<Permission> grant = permissionHandling.getGrantable(currentPermissions, toBeGranted).get(0);
            List<Set<Permission>> revokeAndGrant = permissionHandling.getRevokedAndGrantedAfterMerge(currentPermissions,
                                                                                                     new HashSet<Permission>(),
                                                                                                     grant);
            Set<Permission> revoke = revokeAndGrant.get(0);
            grant = revokeAndGrant.get(1);
            // merging might need revoke and grant operations
            revokeAndGrant(authorizer, subject, revoke, grant);
        }
        Collection<Role> children = root.getRoles();
        if (!children.isEmpty()) {
            for (Role child : children) {
                propagateGrantToSubjects(authorizer, child, action, resource);
            }
        }
    }

    private void propagateRevokeToSubjects(Subject authorizer, Role root, Action action, Resource resource, boolean force) {
        // revoke permission from all users of this role
        Collection<Subject> subjects = root.getSubjects();
        for (Subject subject : subjects) {
            // take care of merging permissions with the existing permissions

            // first: check if permission to be granted is grantable to subject
            Set<Permission> toBeRevoked = new HashSet<Permission>();
            toBeRevoked.add(permissionFactory.create(subject, action, resource));
            List<Permission> currentPermissions = permissionManager.getPermissions(subject);
            // second: check if permission can be merged with existing ones
            PermissionChangeSet changeSet = permissionHandling.getRevokableFromRevoked(currentPermissions, toBeRevoked, true);
            // merging might need revoke and grant operations
            revokeAndGrant(authorizer, subject, changeSet.getRevokes(), changeSet.getGrants());
        }
        Collection<Role> children = root.getRoles();
        if (!children.isEmpty()) {
            for (Role child : children) {
                propagateRevokeToSubjects(authorizer, child, action, resource, force);
            }
        }
    }

    @Override
    public void revokeAndGrant(Subject authorizer, Subject subject, Set<Permission> revoke, Set<Permission> grant) {
        if (revoke == null || grant == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }
        for (Permission permission : revoke) {
            revoke(authorizer, subject, permission);
        }

        for (Permission permission : grant) {
            grant(authorizer, subject, permission);
        }
    }

    @Override
    public void revokeAndGrant(Subject authorizer, Role role, Set<Permission> revoke, Set<Permission> grant) {
        revokeAndGrant(authorizer, role, revoke, grant, DEFAULT_PROPAGATE_TO_SUBJECTS);
    }

    @Override
    public void revokeAndGrant(Subject authorizer, Role role, Set<Permission> revoke, Set<Permission> grant, boolean propagateToSubjects) {
        if (revoke == null || grant == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }
        for (Permission permission : revoke) {
            revoke(authorizer, role, permission, propagateToSubjects);
        }

        for (Permission permission : grant) {
            grant(authorizer, role, permission, propagateToSubjects);
        }
    }

    private Action getGrantAction() {
        return actionFactory.createAction(Action.GRANT);
    }

    private Action getRevokeAction() {
        return actionFactory.createAction(Action.REVOKE);
    }

}
