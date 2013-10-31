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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;

/**
 * 
 * @author cuszk
 */
@Stateless
public class PermissionDataAccessImpl implements PermissionDataAccess {

    private static final Logger LOG = Logger.getLogger(PermissionDataAccessImpl.class.getName());

    @Inject
    private PermissionManager permissionManager;

    @Override
    public boolean isRevokable(Subject subject, Action action, Resource resource) {
        return !getRevokablePermissionsWhenRevoking(subject, action, resource).isEmpty();
    }

    @Override
    public Set<Permission> getRevokablePermissionsWhenRevoking(Subject subject, Action action, Resource resource) {
        // look up itself
        Permission permission = findPermission(subject, action, resource);
        if (permission != null) {
            // if exact permission found -> revoke that
            return new HashSet<Permission>(Arrays.asList(permission));
        } else {
            return getReplaceablePermissions(subject, action, resource);
        }
    }

    private Set<Permission> getReplaceablePermissions(Subject subject, Action action, Resource resource) {
        Set<Permission> ret = new HashSet<Permission>();
        for (Permission rolePermission : permissionManager.getPermissions(subject)) {
            if (rolePermission.getResource().isReplaceableBy(resource) && rolePermission.getAction().implies(action)) {
                ret.add(rolePermission);
            }
        }
        return ret;
    }

    @Override
    public boolean isRevokable(Role actor, Action action, Resource resource) {
        return !getRevokablePermissionsWhenRevoking(actor, action, resource).isEmpty();
    }

    @Override
    public Set<Permission> getRevokablePermissionsWhenRevoking(Role role, Action action, Resource resource) {
        // look up itself
        Permission permission = findPermission(role, action, resource);
        if (permission != null) {
            // if exact permission found -> revoke that
            return new HashSet<Permission>(Arrays.asList(permission));
        } else {
            return getReplaceablePermissions(role, action, resource);
        }
    }

    private Set<Permission> getReplaceablePermissions(Role role, Action action, Resource resource) {
        Set<Permission> ret = new HashSet<Permission>();
        for (Permission rolePermission : permissionManager.getPermissions(role)) {
            if (rolePermission.getResource().isReplaceableBy(resource) && rolePermission.getAction().implies(action)) {
                ret.add(rolePermission);
            }
        }
        return ret;
    }

    @Override
    public boolean isGrantable(Subject subject, Action action, Resource resource) {
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return isGrantable(permissions, subject, action, resource);
    }

    @Override
    public boolean isGrantable(List<Permission> permissions, Subject subject, Action action, Resource resource) {
        // check whether action is exceptional. if it is then resource cannot have a field specified
        if (!resource.isApplicable(action)) {
            LOG.warning("Action " + action + " cannot be applied to " + resource);
            return false;
        }
        Collection<Resource> parents = resource.parents();
        for (Resource r : parents) {
            if (findPermission(permissions, subject, action, r) != null) {
                LOG.warning("Overriding permission already exists");
                return false;
            }
        }
        return true;
    }

    // TODO rename
    @Override
    public Set<Permission> getRevokablePermissionsWhenGranting(Subject subject, Action action, Resource resource) {
        return getReplaceablePermissions(subject, action, resource);

    }

    @Override
    public boolean isGrantable(List<Permission> permissions, Role role, Action action, Resource _resource) {
        // first lookup the exact permission. if it already exists granting is not allowed
        Permission itself = findPermission(permissions, role, action, _resource);
        if (itself != null) {
            LOG.warning("Same permission found");
            return false;
        } else {
            // cast to entity resource to access fields
            EntityField resource = (EntityField) _resource;
            // check whether action is exceptional. if it is then resource cannot have a field specified
            if (!_resource.isApplicable(action)) {
                LOG.warning("Action " + action + " cannot be applied to " + resource);
            }

            // if field is specified -> find permission for entity with no field specified (means that there exists already a
            // permission for all entities with all fields and ids)
            // for Afi-> find A or for Af-> find A.
            if (!resource.isEmptyField()) {
                EntityField resourceWithoutField = new EntityField(resource.getEntity(), EntityField.EMPTY_FIELD);
                if (findPermission(permissions, role, action, resourceWithoutField) != null) {
                    LOG.warning(new StringBuilder().append("Permission already exists for ").append(resourceWithoutField).append(" and ").append(action).toString());
                    return false;
                }
            }
            // if resource has id
            if (_resource instanceof EntityObjectField) {
                EntityObjectField objectResource = (EntityObjectField) _resource;
                // if (!StringUtils.isEmpty(resource.getField())) {
                EntityField resourceWithField = new EntityField(resource.getEntity(), resource.getField());
                // if field and id is specified -> look for permission for entity with given field
                if (findPermission(permissions, role, action, resourceWithField) != null) {
                    LOG.warning(new StringBuilder().append("Permission already exists for ").append(resourceWithField).append(" and ").append(action).toString());
                    return false;
                }
                // if field and id is specified -> look for permission for entity with given id
                EntityObjectField resourceObjectWithoutField = new EntityObjectField(objectResource.getEntity(), EntityField.EMPTY_FIELD, objectResource.getEntityId());
                if (findPermission(permissions, role, action, resourceObjectWithoutField) != null) {
                    LOG.warning(new StringBuilder().append("Permission already exists for ").append(resourceObjectWithoutField).append(" and ").append(action).toString());
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean isGrantable(Role role, Action action, Resource resource) {
        List<Permission> permissions = permissionManager.getPermissions(role);
        return isGrantable(permissions, role, action, resource);

    }

    @Override
    public Set<Permission> getRevokablePermissionsWhenGranting(Role role, Action action, Resource resource) {
        return getReplaceablePermissions(role, action, resource);

    }

    @Override
    public Permission findPermission(Subject subject, Action action, Resource resource) {
        List<Permission> permissions = permissionManager.getPermissions(subject);
        return findPermission(permissions, subject, action, resource);
    }

    @Override
    public Permission findPermission(List<Permission> permissions, Subject subject, Action action, Resource resource) {
        for (Permission permission : permissions) {
            if (permission.getResource().equals(resource) && permission.getAction().equals(action)) {
                return permission;
            }
        }
        return null;
    }

    @Override
    public Permission findPermission(List<Permission> permissions, Role role, Action action, Resource resource) {
        for (Permission permission : permissions) {
            if (permission.getResource().equals(resource) && permission.getAction().equals(action)) {
                return permission;
            }
        }
        return null;
    }

    @Override
    public Permission findPermission(Role role, Action action, Resource resource) {
        List<Permission> permissions = permissionManager.getPermissions(role);
        return findPermission(permissions, role, action, resource);
    }
}