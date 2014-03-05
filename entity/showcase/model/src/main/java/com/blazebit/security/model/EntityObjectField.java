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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.blazebit.security.model.AbstractDataPermissionId;
import com.blazebit.security.model.AbstractEntityObjectField;

/**
 * Class that represents an entity object with an id. Field attribute is
 * inherited from EntityField and it is optional. If not specified it refers to
 * all the entities with the given id.
 * 
 * @author Christian
 */
public class EntityObjectField extends AbstractEntityObjectField {

	public EntityObjectField(String entity, String field, String entityId) {
		super(entity, field, entityId);
	}

	public EntityObjectField(String entity, String entityId) {
		super(entity, entityId);
	}

	public EntityObjectField(AbstractDataPermissionId id) {
		super(id);
	}

	@Override
	public boolean isApplicable(Action action) {
		if (!isEmptyField()) {
			return !Action.CREATE.equals(action.getName());
		} else {
			return !Action.CREATE.equals(action.getName())
					&& !Action.ADD.equals(action.getName())
					&& !Action.REMOVE.equals(action.getName());
		}

	}

	@Override
	public EntityObjectField getParent() {
		if (!super.isEmptyField()) {
			return new EntityObjectField(entity, entityId);
		} else {
			return this;
		}
	}

	@Override
	public EntityObjectField withField(String field) {
		return new EntityObjectField(entity, field, entityId);
	}

	@Override
	public EntityObjectField withEntityId(String entityId) {
		return new EntityObjectField(entity, field, entityId);
	}

	@Override
	public Collection<Resource> connectedResources() {
		List<Resource> l = new ArrayList<Resource>();
		l.add(this);
		l.add(new EntityField(entity));

		if (!isEmptyField()) {
			l.add(getParent());
			l.add(new EntityField(entity, field));
		}

		return l;
	}

}
