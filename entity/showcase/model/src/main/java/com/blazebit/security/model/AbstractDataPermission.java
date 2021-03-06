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

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.blazebit.security.entity.EntityDataResource;

/**
 * @author Christian Beikov
 */
@MappedSuperclass
public abstract class AbstractDataPermission<S, P extends AbstractDataPermissionId<S>, A extends EntityAction, F extends EntityDataResource>
extends BaseEntity<P> implements Permission {

	private static final long serialVersionUID = 1L;
	private A entityAction;
	private F entityObjectField;
	
	protected abstract A createEntityAction(P id);
	
	protected abstract F createEntityObjectField(P id);

	public void setId(P id) {
		this.id = id;
		this.entityAction = createEntityAction(id);
		this.entityObjectField = createEntityObjectField(id);

		if (id != null) {
			if (entityAction != null) {
				id.setActionName(entityAction.getName());
			}
			if (entityObjectField != null) {
				id.setEntity(entityObjectField.getEntity());
				id.setField(entityObjectField.getField());
				id.setEntityId(entityObjectField.getEntityId());
			}
		}
	}

	@Transient
	@Override
	public A getAction() {
		return entityAction;
	}

	@Transient
	@Override
	public F getResource() {
		return entityObjectField;
	}

	@Transient
	public S getSubject() {
        P thisId = getId();
		return thisId == null ? null : thisId.getSubject();
	}

    @Override
	public String toString() {
		return "Permission{" + "id=" + id + "}";
	}

}