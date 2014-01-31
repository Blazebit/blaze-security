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
package com.blazebit.security.impl.model;

import com.blazebit.security.constants.ActionConstants;

/**
 * 
 * @author Christian
 */
public class EntityAction extends AbstractEntityAction {

	public EntityAction(ActionConstants delete) {
		super(delete);
	}

	public <P extends AbstractPermissionId<?>> EntityAction(P id) {
		super(id);
	}

	public EntityAction() {
		super();
	}

	@Override
	public <P extends AbstractPermissionId<?>> void attachToPermissionId(
			P permissionId) {
		super.attachToPermissionId(permissionId);
	}
}
