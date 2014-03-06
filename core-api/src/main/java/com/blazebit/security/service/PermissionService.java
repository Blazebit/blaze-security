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
package com.blazebit.security.service;

import java.util.Set;

import com.blazebit.security.exception.PermissionActionException;
import com.blazebit.security.exception.PermissionException;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * 
 */
public interface PermissionService {

    /**
     * 
     * @param subject
     * @param action
     * @param resource
     * @return true if subject has permission to perform action on resource
     */
    public boolean isGranted(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param action
     * @param resource
     * @return true if logged in user has permission to perform action on resource
     */
    public boolean isGranted(Action action, Resource resource);

    /**
     * authorizer grants permission to subject to perform action on given resource
     * 
     * @param authorizer
     * @param subject
     * @param action
     * @param resource
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Subject subject, Action action, Resource resource) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param subject
     * @param permission
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Subject subject, Permission permission) throws PermissionException,
        PermissionActionException;

    /**
     * authorizer revokes permission from subject to perform action on given resource
     * 
     * @param authorizer
     * @param subject
     * @param action
     * @param resource
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Subject subject, Action action, Resource resource) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param subject
     * @param permission
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Subject subject, Permission permission) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param subject
     * @param permissions
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Subject subject, Set<Permission> permissions) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param role
     * @param action
     * @param resource
     * @return true if role has permission to perform action on resource
     */
    public boolean isGranted(Role role, Action action, Resource resource);

    /**
     * authorizer grants permission to role to perform action on given resource
     * 
     * @param authorizer
     * @param role
     * @param action
     * @param resource
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Role role, Action action, Resource resource) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permission
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Role role, Permission permission) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permission
     * @param propagate
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Role role, Permission permission, boolean propagate) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permissions
     * @param propagate
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Role role, Set<Permission> permissions, boolean propagate)
        throws PermissionException, PermissionActionException;

    /**
     * 
     * authorizer grants permission to role to perform action on given resource
     * 
     * @param authorizer
     * @param role
     * @param action
     * @param resource
     * @param propagate
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Role role, Action action, Resource resource, boolean propagate)
        throws PermissionException, PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permissions
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Role role, Set<Permission> permissions) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * authorizer revokes permission from role to perform action on given resource
     * 
     * @param authorizer
     * @param subject
     * @param action
     * @param resource
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Role subject, Action action, Resource resource) throws PermissionException,
        PermissionActionException;

    /**
     * authorizer revokes permission from role to perform action on given resource
     * 
     * @param authorizer
     * @param role
     * @param action
     * @param resource
     * @param propagate
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Role role, Action action, Resource resource, boolean propagate)
        throws PermissionException, PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permission
     * @param propagate
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Role role, Permission permission, boolean propagate) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permission
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Role role, Permission permission) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permissions
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Role role, Set<Permission> permissions) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param permissions
     * @param propagate
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Role role, Set<Permission> permissions, boolean propagate)
        throws PermissionException, PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param subject
     * @param permissions
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void grant(Subject authorizer, Subject subject, Set<Permission> permissions) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param authorizer
     * @param role
     * @param revoke
     * @param grant
     */
    public void revokeAndGrant(Subject authorizer, Role role, Set<Permission> revoke, Set<Permission> grant);

    /**
     * 
     * @param authorizer
     * @param role
     * @param revoke
     * @param grant
     * @param propagate
     */
    public void revokeAndGrant(Subject authorizer, Role role, Set<Permission> revoke, Set<Permission> grant, boolean propagate);

    /**
     * 
     * @param authorizer
     * @param subject
     * @param revoke
     * @param grant
     */
    public void revokeAndGrant(Subject authorizer, Subject subject, Set<Permission> revoke, Set<Permission> grant);

    /**
     * 
     * @param authorizer
     * @param subject
     * @param action
     * @param resource
     * @param force
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Subject subject, Action action, Resource resource, boolean force)
        throws PermissionException, PermissionActionException;

    /**
     * Revokes permissions for given action and resource, optionally forced and propagated to the subject of the role
     * 
     * @param authorizer
     * @param role
     * @param action
     * @param resource
     * @param force
     * @param propagate
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public void revoke(Subject authorizer, Role role, Action action, Resource resource, boolean force, boolean propagate)
        throws PermissionException, PermissionActionException;

}
