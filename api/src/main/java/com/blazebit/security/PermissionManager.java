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

/**
 * 
 * @author Christian Beikov
 */
public interface PermissionManager {

    /**
     * 
     * @param permission
     * @return saved permission
     */
    public <P extends Permission> P save(P permission);

    /**
     * flushes
     */
    public void flush();

    /**
     * 
     * @param subject
     * @return list of all permissions for a given subject
     */
    public <P extends Permission> List<P> getAllPermissions(Subject subject);

    /**
     * 
     * @param role
     * @return list of all permissions for a given role
     */
    public <P extends Permission> List<P> getAllPermissions(Role role);

    /**
     * deletes a permission
     * 
     * @param permission
     */
    public <P extends Permission> void remove(P permission);

    /**
     * deletes a list of permissions
     * 
     * @param permissions
     */
    public <P extends Permission> void remove(List<P> permissions);

    /**
     * 
     * @param subject
     * @return reloaded subject with all the permissions
     */
    public Subject reloadSubjectWithPermissions(Subject subject);

    /**
     * 
     * @param role
     * @return reloaded role with all the permissions
     */
    public Role reloadSubjectWithPermissions(Role role);

}
