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
import com.blazebit.security.model.Action;

/**
 * Class representing an Entity resource with an optional field attribute. If
 * field attribute is empty the resource refers to all the entities defined by
 * the entity name attribute.
 * 
 * @author Christian
 */
public class EntityField extends AbstractEntityField {

	public EntityField(AbstractPermissionId id) {
		super(id);
	}

	public EntityField(String entity, String field) {
		super(entity, field);
	}

	public EntityField(String entity) {
		super(entity);
	}

	@Override
	public boolean isApplicable(Action action) {
		if (!super.isEmptyField()) {
			return !action.implies(new EntityAction(ActionConstants.DELETE));
		} else {
			return !action.implies(new EntityAction(ActionConstants.ADD))
					&& !action
							.implies(new EntityAction(ActionConstants.REMOVE));
		}
	}

	@Override
	public EntityField getParent() {
		if (!isEmptyField()) {
			return new EntityField(entity);
		} else {
			return this;
		}
	}

	@Override
	public EntityField getChild(String field) {
		return new EntityField(entity, field);
	}
}
