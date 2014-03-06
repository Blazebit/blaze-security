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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * 
 */
public interface PermissionManager {

	/**
	 * saves a permission
	 * 
	 * @param permission
	 * @return saved permission
	 */
	public <P extends Permission> P save(P permission);

	/**
	 * 
	 * list of all permissions of a subject
	 * 
	 * @param subject
	 * @return list of all permissions for a given subject
	 */
	public List<Permission> getPermissions(Subject subject);

	/**
	 * 
	 * list of all permissions of a role
	 * 
	 * @param role
	 * @return list of all permissions for a given role
	 */
	public List<Permission> getPermissions(Role role);

	/**
	 * deletes a permission
	 * 
	 * @param permission
	 */
	public void remove(Collection<? extends Permission> permissions);

	/**
	 * deletes a list of permissions
	 * 
	 * @param permissions
	 */
	public void remove(Permission permission);

	/**
	 * deletes all permissions of a subject
	 * 
	 * @param subject
	 */
	public void removeAllPermissions(Subject subject);

	/**
	 * list of all resources the given subject has any permission
	 * 
	 * @param subject
	 * @return
	 */
	public List<String> getPermissionResources(Subject subject);

    public Set<Permission> getPermissions(Collection<? extends Role> roles);

    public Set<Permission> getPermissions(Collection<? extends Role> roles, boolean includeInherited);

}
