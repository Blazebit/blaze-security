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
package com.blazebit.security;

import java.util.Collection;

/**
 * 
 * @author Christian Beikov
 */
public interface PermissionService {

    /**
     * 
     * @param subject
     * @param action
     * @param resource
     * @return true if subject has permission to perform action on resource
     */
    public <R extends Role<R>> boolean isGranted(Subject<R> subject, Action action, Resource resource);

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
    public <R extends Role<R>> void grant(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource) throws PermissionException, PermissionActionException;

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
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource) throws PermissionException, PermissionActionException;

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
    public <R extends Role<R>> void grant(Subject<R> authorizer, Role role, Action action, Resource resource) throws PermissionException, PermissionActionException;

    /**
     * 
     * authorizer grants permission to role to perform action on given resource
     * 
     * @param authorizer
     * @param role
     * @param action
     * @param resource
     * @param propagateToUsers
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public <R extends Role<R>> void grant(Subject<R> authorizer, Role role, Action action, Resource resource, boolean propagateToUsers) throws PermissionException,
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
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Role subject, Action action, Resource resource) throws PermissionException, PermissionActionException;

    /**
     * authorizer revokes permission from role to perform action on given resource
     * 
     * @param authorizer
     * @param role
     * @param action
     * @param resource
     * @param propagateToUsers
     * @throws PermissionException
     * @throws PermissionActionException
     */
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Role role, Action action, Resource resource, boolean propagateToUsers) throws PermissionException,
        PermissionActionException;

    /**
     * 
     * @param subject
     * @return allowed actions for a subject and resource
     */
    public <R extends Role<R>> Collection<Action> getAllowedActions(Subject<R> subject, Resource resource);

    /**
     * 
     * @return grant action
     */
    public Action getGrantAction();

    /**
     * 
     * @return revoke action
     */
    public Action getRevokeAction();

    /**
     * 
     * @return create action
     */
    public Action getCreateAction();

    /**
     * 
     * @return delete action
     */
    public Action getDeleteAction();

}
