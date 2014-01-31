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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Resource;

/**
 * Class representing an Entity resource with an optional field attribute. If
 * field attribute is empty the resource refers to all the entities defined by
 * the entity name attribute.
 * 
 * @author Christian
 */
public abstract class AbstractEntityField implements Resource {

	public static final String EMPTY_FIELD = "";
	protected String entity;
	protected String field;
	protected AbstractPermissionId<?> permissionId;

	public AbstractEntityField() {
	}

	public AbstractEntityField(String entity, String field) {
		this.entity = entity;
		this.field = field;
	}

	public AbstractEntityField(String entity) {
		this.entity = entity;
		this.field = EMPTY_FIELD;
	}

	public AbstractPermissionId<?> getPermissionId() {
		return permissionId;
	}

	public <P extends AbstractPermissionId<?>> AbstractEntityField(
			P permissionId) {
		this.permissionId = permissionId;
		this.entity = permissionId.getEntity();
		this.field = permissionId.getField();
	}

	public <P extends AbstractPermissionId<?>> void attachToPermissionId(
			P permissionId) {
		this.permissionId = permissionId;

		if (entity != null) {
			permissionId.setEntity(entity);
			entity = null;
		}
		if (field != null) {
			permissionId.setField(field);
			field = null;
		}
	}

	public String getEntity() {
		return permissionId == null ? entity : permissionId.getEntity();
	}

	public void setEntity(String entity) {
		this.entity = entity;
		if (permissionId != null) {
			permissionId.setEntity(entity);
		}
	}

	public String getField() {
		return permissionId == null ? field : permissionId.getField();
	}

	public void setField(String field) {
		this.field = field;
		if (permissionId != null) {
			permissionId.setField(field);
		}
	}

	@Override
	public String toString() {
		return "EntityField{" + getEntity() + ", " + getField() + '}';
	}

	@Override
	public int hashCode() {
		int hash = 3;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbstractEntityField other = (AbstractEntityField) obj;
		if ((this.entity == null) ? (other.entity != null) : !this.entity
				.equals(other.entity)) {
			return false;
		}
		if ((this.field == null) ? (other.field != null) : !this.field
				.equals(other.field)) {
			return false;
		}
		return true;
	}

	public boolean isEmptyField() {
		return this.getField().equals(EMPTY_FIELD);
	}

	@Override
	public boolean implies(Resource resource) {
		if (resource instanceof AbstractEntityField) {
			AbstractEntityField _resource = (AbstractEntityField) resource;
			if (permissionId == null) {
				return this.getEntity().equals(_resource.getEntity())
						&& (this.isEmptyField() || this.getField().equals(
								_resource.getField()));
			} else {
				return permissionId.getEntity().equals(_resource.getEntity())
						&& (permissionId.getField().equals(EMPTY_FIELD) || permissionId
								.getField().equals(_resource.getField()));
			}
		} else {
			throw new IllegalArgumentException("Not supported resource type");
		}
	}

	@Override
	public boolean isReplaceableBy(Resource resource) {
		if (resource instanceof AbstractEntityObjectField) {
			return false;
		} else {
			if (resource instanceof AbstractEntityField) {
				AbstractEntityField _resource = (AbstractEntityField) resource;
				// EntityField can replace other EntityFields if the existing
				// EntityField has specific field and the new
				// EntityField
				// has EMPTY_FIELD
				if (permissionId == null) {
					return this.getEntity().equals(_resource.getEntity())
							&& (_resource.isEmptyField() && !this
									.isEmptyField());
				} else {
					return permissionId.getEntity().equals(
							_resource.getEntity())
							&& (_resource.isEmptyField() && !permissionId
									.getField().equals(EMPTY_FIELD));
				}
			} else {
				throw new IllegalArgumentException(
						"Not supported resource type");

			}
		}
	}

	@Override
	public Collection<Resource> connectedResources() {
		List<Resource> ret = new ArrayList<Resource>();
		ret.add(this);
		Resource parent = getParent();
		if (parent != this) {
			ret.add(parent);
		}
		return ret;
	}

	@Override
	public abstract boolean isApplicable(Action action);

	@Override
	public abstract Resource getParent();

	
	public abstract Resource getChild(String field);
}
