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
package com.blazebit.security.model;

import java.io.Serializable;
import java.security.Principal;

/**
 * 
 */
public interface Subject extends Principal, Serializable {
	/**
	 * 
	 * @return roles of a subject
	 */
	// public Collection<Role> getRoles();

	/**
	 * 
	 * @return collection of all the permissions of a subject
	 */
	// public Collection<Permission> getAllPermissions();
}
