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
package com.blazebit.security.data;

import java.util.Set;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * 
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
    public boolean isRevokable(Subject subject, Action action, Resource resource);

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
     * @param subject
     * @param action
     * @param resource
     * @return permission to be removed when revoking given permission parameters (removing itself if found or its "subset")
     */
    public Set<Permission> getRevokeImpliedPermissions(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param role
     * @param action
     * @param resource
     * @return permission to be removed when revoking given permission parameters (removing itself if found or its "subset")
     */
    public Set<Permission> getRevokeImpliedPermissions(Role role, Action action, Resource resource);

    /**
     * 
     * @param subject
     * @param action
     * @param resource
     * @return true if permission to be created from the given action and resource can be granted to the subject
     */
    public boolean isGrantable(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param role
     * @param action
     * @param resource
     * @return true if permission to be created from the given action and resource can be granted to the role
     */
    public boolean isGrantable(Role role, Action action, Resource resource);

    /**
     * 
     * @param subject
     * @param action
     * @param resource
     * @return set of permissions to be revoked when granting the given action and resource to the subject
     */
    public Set<Permission> getGrantImpliedPermissions(Subject subject, Action action, Resource resource);

    /**
     * 
     * @param role
     * @param action
     * @param resource
     * @return set of permissions to be revoked when granting the given action and resource to the role
     */
    public Set<Permission> getGrantImpliedPermissions(Role role, Action action, Resource resource);

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
     * @param subject
     * @param action
     * @param resource
     * @return
     */
    public Set<Permission> getImpliedBy(Subject subject, Action action, Resource resource);

}