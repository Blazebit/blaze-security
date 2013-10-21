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

import java.util.List;
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
     * @return true if subject permission to be revoked for the given action and resource can be revoked ( it exists or its
     *         "subset" exists)
     */
    public boolean isRevokable(Subject<?> subject, Action action, Resource resource);

    /**
     * 
     * @param role
     * @param action
     * @param resource
     * @return true if role permission to be revoked for the given action and resource can be revoked ( it exists or its
     *         "subset" exists)
     */
    public boolean isRevokable(Role role, Action action, Resource resource);

    /**
     * 
     * @param _subject
     * @param _action
     * @param _resource
     * @return permission to be removed when revoking given permission parameters (removing itself if found or its "subset")
     */
    public Set<Permission> getRevokablePermissionsWhenRevoking(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param _role
     * @param _action
     * @param _resource
     * @return permission to be removed when revoking given permission parameters (removing itself if found or its "subset")
     */
    public Set<Permission> getRevokablePermissionsWhenRevoking(Role role, Action action, Resource resource);

    /**
     * 
     * @param subject
     * @param action
     * @param _resource
     * @return true if permission to be created from the given action and resource can be granted to the subject
     */
    public boolean isGrantable(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param permissions given permissions
     * @param subject
     * @param action
     * @param _resource
     * @return true if permission to be created from the given action and resource can be granted to the subject with the given
     *         permissions
     */
    boolean isGrantable(List<Permission> permissions, Subject subject, Action action, Resource _resource);

    /**
     * 
     * @param role
     * @param action
     * @param _resource
     * @return true if permission to be created from the given action and resource can be granted to the role
     */
    public boolean isGrantable(Role role, Action action, Resource resource);

    /**
     * 
     * @param permissions
     * @param role
     * @param action
     * @param _resource
     * @return
     */
    public boolean isGrantable(List<Permission> permissions, Role role, Action action, Resource _resource);

    /**
     * 
     * @param _subject
     * @param _action
     * @param _resource
     * @return set of permissions to be revoked when granting the given action and resource to the subject
     */
    public Set<Permission> getRevokablePermissionsWhenGranting(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param role
     * @param _action
     * @param _resource
     * @return set of permissions to be revoked when granting the given action and resource to the role
     */
    public Set<Permission> getRevokablePermissionsWhenGranting(Role role, Action _action, Resource _resource);

    /**
     * 
     * @param subject
     * @param action
     * @param resource
     * @return permission object for the given action and resource
     */
    public Permission findPermission(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param role
     * @param action
     * @param resource
     * @return permission object for the given action and resource
     */
    public Permission findPermission(Role role, Action action, Resource resource);

    /**
     * 
     * @param permissions
     * @param subject
     * @param action
     * @param resource
     * @return
     */
    public Permission findPermission(List<Permission> permissions, Subject subject, Action action, Resource resource);

    /**
     * 
     * @param permissions
     * @param role
     * @param action
     * @param resource
     * @return
     */
    public Permission findPermission(List<Permission> permissions, Role role, Action action, Resource resource);

}