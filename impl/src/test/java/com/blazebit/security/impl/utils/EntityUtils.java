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
package com.blazebit.security.impl.utils;

import com.blazebit.security.Action;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.SubjectRoleConstants;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

/**
 *
 * @author cuszk
 */
public class EntityUtils {

    public static EntityField getEntityFieldFor(Class<?> clazz) {
        return new EntityField(clazz.getSimpleName().toUpperCase(), "");
    }

    public static EntityField getEntityFieldFor(Enum<?> entity) {
        return new EntityField(entity.name(), "");
    }

    public static EntityField getEntityFieldFor(Enum<?> entity, String field) {
        return new EntityField(entity.name(), field);
    }

    public static EntityObjectField getEntityObjectFieldFor(Enum<?> entity, String id) {
        return new EntityObjectField(entity.name(), "", id);
    }

    public static EntityObjectField getEntityObjectFieldFor(Enum<?> entity, String field, String id) {
        return new EntityObjectField(entity.name(), field, id);
    }

    public static <S extends Subject> EntityObjectField getEntityObjectFieldFor(S subject) {
        if (subject instanceof User) {
            return new EntityObjectField(SubjectRoleConstants.USER.name(), ((IdHolder) subject).getEntityId());
        } else {
            throw new IllegalArgumentException("Not supported entity type");
        }
    }

    public static <R extends Role> EntityObjectField getEntityObjectFieldFor(R role) {
        if (role instanceof UserGroup) {
            return new EntityObjectField(SubjectRoleConstants.USERGROUP.name(), ((IdHolder) role).getEntityId());
        } else {
            throw new IllegalArgumentException("Not supported entity type");
        }
    }

    public static <A extends Action> EntityField getEntityFieldFor(A action) {
        if (action instanceof EntityAction) {
            return new EntityField(((EntityAction) action).getActionName(), "");
        } else {
            throw new IllegalArgumentException("Not supported action type");
        }
    }

}
