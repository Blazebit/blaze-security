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
package com.blazebit.security.service;

import com.blazebit.security.Action;
import com.blazebit.security.Actor;
import com.blazebit.security.Permission;
import com.blazebit.security.Resource;
import java.util.Set;

/**
 *
 * @author cuszk
 */
public interface PermissionDataAccess {

    /**
     *
     * @param subject
     * @param action
     * @param resource
     * @return true if user permission to be revoked for the given action and
     * resource can be revoked ( it exists or its "subset" exists)
     */
    public boolean isRevokable(Actor actor, Action action, Resource resource);

    /**
     *
     * @param _subject
     * @param _action
     * @param _resource
     * @return permission to be removed when revoking given permission
     * parameters (removing itself if found or its "subset")
     */
    public Set<Permission> getRevokablePermissionsWhenRevoking(Actor _subject, Action _action, Resource _resource);

    /**
     *
     * @param subject
     * @param action
     * @param _resource
     * @return true if permission to be created from the given actior, action
     * and resource can be granted to the actor
     */
    public boolean isGrantable(Actor actor, Action action, Resource _resource);

    /**
     *
     * @param _subject
     * @param _action
     * @param _resource
     * @return set of permissions to be revoked when granting the given action
     * and resource to the actor
     */
    public Set<Permission> getRevokablePermissionsWhenGranting(Actor _actor, Action _action, Resource _resource);

    /**
     *
     * @param subject
     * @param action
     * @param resource
     * @return finds subject permission for the given action and resource
     */
    public Permission findPermission(Actor actor, Action action, Resource resource);

}