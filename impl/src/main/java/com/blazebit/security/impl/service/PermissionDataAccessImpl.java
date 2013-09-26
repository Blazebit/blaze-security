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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.blazebit.lang.StringUtils;
import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.constants.ActionConstants;
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
    private ActionFactory actionFactory;

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
        for (Permission rolePermission : permissionManager.getAllPermissions(subject)) {
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
        for (Permission rolePermission : permissionManager.getAllPermissions(role)) {
            if (rolePermission.getResource().isReplaceableBy(resource) && rolePermission.getAction().implies(action)) {
                ret.add(rolePermission);
            }
        }
        return ret;
    }
    
  
    @Override
    public boolean isGrantable(Subject subject, Action action, Resource _resource) {
        // first lookup the exact permission. if it already exists granting is not allowed
        Permission itself = findPermission(subject, action, _resource);
        if (itself != null) {
            LOG.warning("Same permission found");
            return false;
        } else {
            List<Permission> permissions = permissionManager.getAllPermissions(subject);
            // cast to entity resource to access fields
            EntityField resource = (EntityField) _resource;
            // check whether action is exceptional. if it is then resource cannot have a field specified
            if (isExceptionalAction(action)) {
                if (!StringUtils.isEmpty(resource.getField())) {
                    LOG.warning("Action " + action + " cannot be applied to " + resource);
                    return false;
                }
                // entityid cannot be specified for add action
                if (action.implies(actionFactory.createAction(ActionConstants.CREATE))) {
                    if (resource instanceof EntityObjectField) {
                        LOG.warning("Action " + action + " cannot be applied to " + resource);
                        return false;
                    }
                }
            }

            // if field is specified -> find permission for entity with no field specified (means that there exists already a
            // permission for all entities with all fields and ids)
            // for Afi-> find A or for Af-> find A.
            if (!StringUtils.isEmpty(resource.getField())) {
                EntityField resourceWithoutField = new EntityField(resource.getEntity(), EntityField.EMPTY_FIELD);
                if (findPermission(subject, action, resourceWithoutField) != null) {
                    LOG.warning("Permission for all fields already exists");
                    return false;
                }
            }
            // if resource has id
            if (_resource instanceof EntityObjectField) {
                EntityObjectField objectResource = (EntityObjectField) _resource;
                // if (!StringUtils.isEmpty(resource.getField())) {
                EntityField resourceWithField = new EntityField(resource.getEntity(), resource.getField());
                // if field and id is specified -> look for permission for entity with given field
                if (findPermission(subject, action, resourceWithField) != null) {
                    LOG.warning("Permission for all entities with this fields already exists");
                    return false;
                }
                // if field and id is specified -> look for permission for entity with given id
                EntityObjectField resourceObjectWithoutField = new EntityObjectField(objectResource.getEntity(), EntityField.EMPTY_FIELD, objectResource.getEntityId());
                if (findPermission(subject, action, resourceObjectWithoutField) != null) {
                    LOG.warning("Data Permission for all entities with this id for all fields already exists");
                    return false;
                }
                // }
            }
            return true;
        }
    }

    private boolean isExceptionalAction(Action action) {
        List<Action> actions = actionFactory.getExceptionalActions();
        for (Action exceptionalAction : actions) {
            if (exceptionalAction.implies(action)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Permission> getRevokablePermissionsWhenGranting(Subject subject, Action action, Resource resource) {
        return getReplaceablePermissions(subject, action, resource);

    }

    @Override
    public boolean isGrantable(Role role, Action action, Resource _resource) {
        // first lookup the exact permission. if it already exists granting is not allowed
        Permission itself = findPermission(role, action, _resource);
        if (itself != null) {
            System.err.println(new StringBuilder("Permission for ").append(action).append(" and ").append(_resource).append(" already exists"));
            return false;
        } else {
            // cast to entity resource to access fields
            EntityField resource = (EntityField) _resource;
            // check whether action is exceptional. if it is then resource cannot have a field specified
            if (isExceptionalAction(action)) {
                if (!StringUtils.isEmpty(resource.getField())) {
                    LOG.warning(new StringBuilder().append("Action ").append(action).append(" cannot be applied to ").append(resource).toString());
                    return false;
                }
                // entityid cannot be specified for add action
                if (action.implies(actionFactory.createAction(ActionConstants.CREATE))) {
                    if (resource instanceof EntityObjectField) {
                        LOG.warning(new StringBuilder().append("Action ").append(action).append(" cannot be applied to ").append(resource).toString());
                        return false;
                    }
                }
            }

            // if field is specified -> find permission for entity with no field specified (means that there exists already a
            // permission for all entities with all fields and ids)
            // for Afi-> find A or for Af-> find A.
            if (!StringUtils.isEmpty(resource.getField())) {
                EntityField resourceWithoutField = new EntityField(resource.getEntity(), EntityField.EMPTY_FIELD);
                if (findPermission(role, action, resourceWithoutField) != null) {
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
                if (findPermission(role, action, resourceWithField) != null) {
                    LOG.warning(new StringBuilder().append("Permission already exists for ").append(resourceWithField).append(" and ").append(action).toString());
                    return false;
                }
                // if field and id is specified -> look for permission for entity with given id
                EntityObjectField resourceObjectWithoutField = new EntityObjectField(objectResource.getEntity(), EntityField.EMPTY_FIELD, objectResource.getEntityId());
                if (findPermission(role, action, resourceObjectWithoutField) != null) {
                    LOG.warning(new StringBuilder().append("Permission already exists for ").append(resourceObjectWithoutField).append(" and ").append(action).toString());
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public Set<Permission> getRevokablePermissionsWhenGranting(Role role, Action action, Resource resource) {
        return getReplaceablePermissions(role, action, resource);

    }

    @Override
    public Permission findPermission(Subject subject, Action action, Resource resource) {
        List<Permission> permissions = permissionManager.getAllPermissions(subject);
        for (Permission permission : permissions) {
            if (permission.getResource().equals(resource) && permission.getAction().equals(action)) {
                return permission;
            }
        }
        return null;
    }

    @Override
    public Permission findPermission(Role role, Action action, Resource resource) {
        List<Permission> permissions = permissionManager.getAllPermissions(role);
        for (Permission permission : permissions) {
            if (permission.getResource().equals(resource) && permission.getAction().equals(action)) {
                return permission;
            }
        }
        return null;
    }

}