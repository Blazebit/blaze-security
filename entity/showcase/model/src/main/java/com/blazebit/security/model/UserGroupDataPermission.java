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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;

import com.blazebit.security.model.AbstractDataPermission;

/**
 * @author Christian Beikov
 */
@Entity
public class UserGroupDataPermission
		extends
		AbstractDataPermission<UserGroup, UserGroupDataPermissionId, EntityAction, EntityObjectField>
		implements RolePermission {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	public UserGroupDataPermission() {
	}

	@EmbeddedId
	public UserGroupDataPermissionId getId() {
		return id;
	}

    @Override
    @Transient
	public UserGroup getRole() {
	    return getSubject();
	}

	@Override
	protected EntityAction createEntityAction(UserGroupDataPermissionId id) {
		return new EntityAction(id);
	}

	@Override
	protected EntityObjectField createEntityObjectField(
			UserGroupDataPermissionId id) {
		return new EntityObjectField(id);
	}
}
