/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl.model;

import com.blazebit.security.Permission;
import java.io.Serializable;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class AbstractDataPermission<S, P extends DataPermissionId<S>> implements Permission<EntityObjectField>, Serializable {

    private static final long serialVersionUID = 1L;
    protected P id;
    private EntityAction entityAction;
    private EntityObjectField entityObjectField;
    private S subject;

    public void setId(P id) {
        this.id = id;
        //why does this create new entityaction and field?
//        this.entityAction = new EntityAction(id);
//        this.entityObjectField = new EntityObjectField(id);
//
//        if (id != null) {
//            if (subject != null) {
//                id.setSubject(subject);
//            }
//            if (entityAction != null) {
//                id.setActionName(entityAction.getActionName());
//            }
//            if (entityObjectField != null) {
//                id.setEntity(entityObjectField.getEntity());
//                id.setField(entityObjectField.getField());
//                id.setEntityId(entityObjectField.getEntityId());
//            }
//        }
    }

    @Override
    @Transient
    public EntityAction getAction() {
        return entityAction;
    }

    public void setEntityAction(EntityAction entityAction) {
        entityAction.attachToPermissionId(id);
        this.entityAction = entityAction;
    }

    @Transient
    public EntityAction getEntityAction() {
        return entityAction;
    }

    @Transient
    @Override
    public EntityObjectField getResource() {
        return entityObjectField;
    }

    @Transient
    public EntityObjectField getEntityObjectField() {
        return entityObjectField;
    }

    public void setEntityObjectField(EntityObjectField entityObjectField) {
        entityObjectField.attachToPermissionId(id);
        this.entityObjectField = entityObjectField;
    }

    @Transient
    public S getSubject() {
        return id == null ? subject : id.getSubject();
    }

    public void setSubject(S subject) {
        if (id == null) {
            this.subject = subject;
        } else {
            id.setSubject(subject);
        }
    }
}