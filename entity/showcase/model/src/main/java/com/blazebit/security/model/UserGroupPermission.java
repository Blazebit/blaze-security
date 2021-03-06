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

import com.blazebit.security.model.AbstractPermission;

/**
 * @author Christian Beikov
 */
@Entity
public class UserGroupPermission
		extends
		AbstractPermission<UserGroup, UserGroupPermissionId, EntityAction, EntityField>
		implements RolePermission {

	private static final long serialVersionUID = 1L;

	public UserGroupPermission() {
	}

	@EmbeddedId
	public UserGroupPermissionId getId() {
		return id;
	}

    @Override
    @Transient
    public UserGroup getRole() {
        return getSubject();
    }

	@Override
	protected EntityAction createEntityAction(UserGroupPermissionId id) {
		return new EntityAction(id);
	}

	@Override
	protected EntityField createEntityField(UserGroupPermissionId id) {
		return new EntityField(id);
	}
}
