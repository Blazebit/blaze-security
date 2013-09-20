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
package com.blazebit.security.impl.interceptor;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.security.IdHolder;
import com.blazebit.security.PermissionException;
import com.blazebit.security.SecurityService;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.ResourceName;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.impl.utils.ActionUtils.ActionConstants;
import com.blazebit.security.impl.utils.EntityUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.Type;

/**
 *
 * @author cuszk
 */
public class ChangeInterceptor extends EmptyInterceptor {

    private static volatile boolean activeUpdate = false;
    private static volatile boolean activeDelete = false;
    private static volatile boolean activePersist = false;

    public static void activateUpdate() {
        ChangeInterceptor.activeUpdate = true;
    }

    public static void activatePersist() {
        ChangeInterceptor.activePersist = true;
    }

    public static void activateDelete() {
        ChangeInterceptor.activeDelete = true;
    }

    public static void deactivate() {
        ChangeInterceptor.activeUpdate = false;
        ChangeInterceptor.activePersist = false;
        ChangeInterceptor.activeDelete = false;
    }

    public static void activate() {
        ChangeInterceptor.activeUpdate = true;
        ChangeInterceptor.activePersist = true;
        ChangeInterceptor.activeDelete = true;
    }

    /**
     *
     *
     * @param entity
     * @param id
     * @param currentState
     * @param previousState
     * @param propertyNames
     * @param types
     * @return
     */
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        if (!ChangeInterceptor.activeUpdate) {
            return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        }
        if (AnnotationUtils.findAnnotation(entity.getClass(), ResourceName.class) == null) {
            return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        }
        List<String> changedPropertyNames = new ArrayList<String>();
        for (int i = 0; i < currentState.length; i++) {
            //we dont check collections here
            if (!types[i].isCollectionType()) {
                if ((currentState[i] != null && !currentState[i].equals(previousState[i])) || (currentState[i] == null && previousState[i] != null)) {
                    changedPropertyNames.add(propertyNames[i]);
                }
            }
        }
        UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
        boolean isGranted = false;
        for (String propertyName : changedPropertyNames) {
            isGranted = BeanProvider.getContextualReference(SecurityService.class).isGranted(userContext.getUser(), ActionUtils.getAction(ActionConstants.UPDATE), EntityUtils.getEntityObjectFieldFor(entity.getClass(), propertyName, ((IdHolder) entity).getEntityId()));
            if (!isGranted) {
                break;
            }
        }
        if (isGranted) {
            return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        } else {
            throw new PermissionException("Entity " + entity + " is not permitted to be flushed by " + userContext.getUser());
        }
    }

    @Override
    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
        if (!ChangeInterceptor.activeUpdate) {
            super.onCollectionUpdate(collection, key);
            return;
        }
        if (collection instanceof PersistentCollection) {
            PersistentCollection coll = (PersistentCollection) collection;
            Object owner = coll.getOwner();
            if (AnnotationUtils.findAnnotation(owner.getClass(), ResourceName.class) == null) {
                super.onCollectionUpdate(collection, key);
            }
            String fieldName = coll.getRole().replace(owner.getClass().getName() + ".", "");
            UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
            //find resource to check
            String entityId = null;
            if (owner instanceof IdHolder) {
                entityId = ((IdHolder) owner).getEntityId();
            }
            boolean isGranted = BeanProvider.getContextualReference(SecurityService.class).isGranted(userContext.getUser(), ActionUtils.getAction(ActionConstants.UPDATE), EntityUtils.getEntityResourceFor(owner.getClass(), fieldName, entityId));

            if (!isGranted) {
                throw new PermissionException("Entity " + owner + " is not permitted to be flushed by " + userContext.getUser());
            } else {
                super.onCollectionUpdate(collection, key);
            }
        } else {
            //not a persistent collection?
        }
    }

    @Override
    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
        super.onCollectionRecreate(collection, key); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
        super.onCollectionRemove(collection, key); //To change body of generated methods, choose Tools | Templates.
    }

    //add
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (!ChangeInterceptor.activePersist) {
            return super.onSave(entity, id, state, propertyNames, types);
        }
        if (AnnotationUtils.findAnnotation(entity.getClass(), ResourceName.class) == null) {
            return super.onSave(entity, id, state, propertyNames, types);
        }
        UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
        //find resource to check
        boolean isGranted = BeanProvider.getContextualReference(SecurityService.class).isGranted(userContext.getUser(), ActionUtils.getAction(ActionConstants.CREATE), EntityUtils.getEntityFieldFor(entity.getClass(), EntityField.EMPTY_FIELD));
        if (!isGranted) {
            throw new PermissionException("Entity " + entity + " is not permitted to be persisted by " + userContext.getUser());
        } else {
            return super.onSave(entity, id, state, propertyNames, types);
        }
    }
//delete

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (!ChangeInterceptor.activeDelete) {
            super.onDelete(entity, id, state, propertyNames, types);


        }
        UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
        //find resource to check
        String entityId = null;
        if (entity instanceof IdHolder) {
            entityId = ((IdHolder) entity).getEntityId();
        }
        boolean isGranted = BeanProvider.getContextualReference(SecurityService.class).isGranted(userContext.getUser(), ActionUtils.getAction(ActionConstants.DELETE), EntityUtils.getEntityResourceFor(entity.getClass(), EntityField.EMPTY_FIELD, entityId));
        if (!isGranted) {
            throw new PermissionException("Entity " + entity + " is not permitted to be deleted by " + userContext.getUser());
        } else {
            super.onDelete(entity, id, state, propertyNames, types);
        }
    }
}
