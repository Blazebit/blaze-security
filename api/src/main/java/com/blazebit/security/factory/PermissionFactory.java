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
package com.blazebit.security.factory;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * Produces Permission objects
 * 
 * @author Christian Beikov
 */
public interface PermissionFactory {

	/**
	 * 
	 * @param <R>
	 * @param <P>
	 * @param subject
	 * @param action
	 * @param resource
	 * @return creates a permission for a subject
	 */
	public Permission create(Subject subject, Action action, Resource resource);

	/**
	 * 
	 * @param <R>
	 * @param <P>
	 * @param role
	 * @param action
	 * @param resource
	 * @return creates a permissions for a role
	 */
	public Permission create(Role role, Action action, Resource resource);

	/**
	 * 
	 * @param action
	 * @param resource
	 * @return creates a permission without a subject reference
	 */
	public Permission create(Action action, Resource resource);

}
