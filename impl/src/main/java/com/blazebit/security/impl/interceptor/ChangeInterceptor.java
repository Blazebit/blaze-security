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
package com.blazebit.security.impl.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.Type;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionActionException;
import com.blazebit.security.PermissionService;
import com.blazebit.security.ResourceNameFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.ResourceName;
import com.blazebit.security.service.api.PropertyDataAccess;

/**
 * 
 * @author cuszk
 */
public class ChangeInterceptor extends EmptyInterceptor {

    private static final long serialVersionUID = 1L;
    private static volatile boolean active = false;

    public static void activate() {
        ChangeInterceptor.active = true;
    }

    public static void deactivate() {
        ChangeInterceptor.active = false;

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
     * @return true if given entity is permitted to be flushed
     */
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        if (!ChangeInterceptor.active) {
            return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        }
        if (AnnotationUtils.findAnnotation(entity.getClass(), ResourceName.class) == null) {
            return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        }
        List<String> changedPropertyNames = new ArrayList<String>();
        if (previousState != null) {
            for (int i = 0; i < currentState.length; i++) {

                // we dont check collections here, there is a separate method for it See: {@link #onCollectionUpdate(collection,
                // key) onCollectionUpdate}
                if (!types[i].isCollectionType()) {
                    if ((currentState[i] != null && !currentState[i].equals(previousState[i])) || (currentState[i] == null && previousState[i] != null)) {
                        changedPropertyNames.add(propertyNames[i]);
                    }
                }
            }
        }
        UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);
        ResourceNameFactory resourceNameFactory = BeanProvider.getContextualReference(ResourceNameFactory.class);
        PermissionService permissionService = BeanProvider.getContextualReference(PermissionService.class);
        boolean isGranted = changedPropertyNames.isEmpty();
        for (String propertyName : changedPropertyNames) {
            isGranted = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.UPDATE),
            resourceNameFactory.createResource((IdHolder) entity, propertyName));
            if (!isGranted) {
                break;
            }
        }
        if (isGranted) {
            return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
        } else {
            throw new PermissionActionException("Entity " + entity + " is not permitted to be flushed by " + userContext.getUser());
        }
    }

    /**
    * 
    */
    @Override
    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
        if (!ChangeInterceptor.active) {
            super.onCollectionUpdate(collection, key);
            return;
        }
        if (collection instanceof PersistentCollection) {
            PersistentCollection newValuesCollection = (PersistentCollection) collection;
            Object entity = newValuesCollection.getOwner();
            if (AnnotationUtils.findAnnotation(entity.getClass(), ResourceName.class) == null) {
                super.onCollectionUpdate(collection, key);
                return;
            }
            // copy new values and old values
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Collection<?> newValues = new HashSet((Collection<?>) newValuesCollection.getValue());
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Set<?> oldValues = new HashSet(((Map<?, ?>) newValuesCollection.getStoredSnapshot()).keySet());

            String fieldName = StringUtils.replace(newValuesCollection.getRole(), entity.getClass().getName() + ".", "");
            UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
            ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);
            ResourceNameFactory resourceNameFactory = BeanProvider.getContextualReference(ResourceNameFactory.class);
            PermissionService permissionService = BeanProvider.getContextualReference(PermissionService.class);
            PropertyDataAccess propertyDataAccess = BeanProvider.getContextualReference(PropertyDataAccess.class);

            // find all objects that were added
            boolean isGrantedToAdd = true;
            boolean isGrantedToRemove = true;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            Set<?> retained = new HashSet(oldValues);
            retained.retainAll(newValues);

            oldValues.removeAll(retained);
            // if there is a difference between oldValues and newValues
            if (!oldValues.isEmpty()) {
                // if something remained
                if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.FIELD_LEVEL))) {
                    isGrantedToRemove = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.REMOVE),
                                                                    resourceNameFactory.createResource((IdHolder) entity, fieldName));
                } else {
                    isGrantedToRemove = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.UPDATE),
                                                                 resourceNameFactory.createResource((IdHolder) entity));
                }
            }
            
            newValues.removeAll(retained);
            if (!newValues.isEmpty()) {
                if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.FIELD_LEVEL))) {
                    isGrantedToAdd = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.ADD),
                                                                 resourceNameFactory.createResource((IdHolder) entity, fieldName));
                } else {
                    isGrantedToAdd = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.UPDATE),
                                                                 resourceNameFactory.createResource((IdHolder) entity));
                }
            }

            if (!isGrantedToAdd) {
                throw new PermissionActionException("Element cannot be added to entity " + entity + "'s collection " + fieldName + " by " + userContext.getUser());
            } else {
                if (!isGrantedToRemove) {
                    throw new PermissionActionException("Element cannot be removed from entity " + entity + "'s collection " + fieldName + " by " + userContext.getUser());
                } else {
                    super.onCollectionUpdate(collection, key);
                    return;
                }
            }
        } else {
            // not a persistent collection?
        }
    }

    /**
 * 
 */
    @Override
    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
        // TODO newly created entities with collections should be checked here for permission but collection cannot give back
        // its
        // role in the parent entity. BUG? Workaround: it can be checked in the #onSave(...) method
        // if (!ChangeInterceptor.active) {
        // super.onCollectionRecreate(collection, key);
        // }
        // if (collection instanceof PersistentCollection) {
        // PersistentCollection newValuesCollection = (PersistentCollection) collection;
        // Object entity = newValuesCollection.getOwner();
        // if (AnnotationUtils.findAnnotation(entity.getClass(), ResourceName.class) == null) {
        // super.onCollectionRecreate(collection, key);
        // }
        // if (ReflectionUtils.isSubtype(entity.getClass(), Permission.class)) {
        // throw new IllegalArgumentException("Permission cannot be persisted by this persistence unit!");
        // }
        // @SuppressWarnings({ "unchecked", "rawtypes" })
        // Collection<?> newValues = new HashSet((Collection<?>) newValuesCollection.getValue());
        // String fieldName = StringUtils.replace(newValuesCollection.getRole(), entity.getClass().getName() + ".", "");
        // if (!newValues.isEmpty()) {
        // // element has been added to the collection - check Add permission
        // UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
        // ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);
        // EntityResourceFactory entityFieldFactory = BeanProvider.getContextualReference(EntityResourceFactory.class);
        // PermissionService permissionService = BeanProvider.getContextualReference(PermissionService.class);
        // boolean isGranted = permissionService.isGranted(userContext.getUser(),
        // actionFactory.createAction(ActionConstants.ADD),
        // entityFieldFactory.createResource((IdHolder) entity, fieldName));
        // if (!isGranted) {
        // // throw new PermissionException("Element cannot be added to Entity " + entity + "'s collection " +
        // // fieldName + " by " + userContext.getUser());
        // }
        // }
        // }
        super.onCollectionRecreate(collection, key);
    }

    @Override
    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
        super.onCollectionRemove(collection, key); // To change body of generated methods, choose Tools | Templates.
    }

    // add
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (!ChangeInterceptor.active) {
            return super.onSave(entity, id, state, propertyNames, types);
        }
        if (AnnotationUtils.findAnnotation(entity.getClass(), ResourceName.class) == null) {
            return super.onSave(entity, id, state, propertyNames, types);
        }
        if (ReflectionUtils.isSubtype(entity.getClass(), Permission.class)) {
            throw new IllegalArgumentException("Permission cannot be persisted by this persistence unit!");
        }
        UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);
        // EntityResourceFactory entityFieldFactory = BeanProvider.getContextualReference(EntityResourceFactory.class);
        ResourceNameFactory resourceNameFactory = BeanProvider.getContextualReference(ResourceNameFactory.class);
        PermissionService permissionService = BeanProvider.getContextualReference(PermissionService.class);
        PropertyDataAccess propertyDataAccess = BeanProvider.getContextualReference(PropertyDataAccess.class);

        boolean isGranted = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.CREATE),
                                                        resourceNameFactory.createResource((IdHolder) entity, EntityField.EMPTY_FIELD));
        if (!isGranted) {
            throw new PermissionActionException("Entity " + entity + " is not permitted to be persisted by " + userContext.getUser());
        }
        // check if collection relations have permission
        boolean isGrantedAddEntity = true;
        boolean isGrantedUpdateRelatedEntity = true;
        for (int i = 0; i < state.length; i++) {
            String fieldName = propertyNames[i];
            // check only relations but dont check collection type @link #onCollectionRecreate)
            if (types[i].isCollectionType()) {
                Collection<?> collection = (Collection<?>) state[i];
                if (!collection.isEmpty()) {
                    // elements have been added
                    if (Boolean.valueOf(propertyDataAccess.getPropertyValue(Company.FIELD_LEVEL))) {
                        isGrantedAddEntity = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.ADD),
                                                                         resourceNameFactory.createResource((IdHolder) entity, fieldName));
                    } else {
                        isGrantedAddEntity = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.UPDATE),
                                                                         resourceNameFactory.createResource((IdHolder) entity));
                    }

                    if (!isGrantedAddEntity) {
                        throw new PermissionActionException("Element to Entity " + entity + "'s collection " + fieldName + " cannot be added by " + userContext.getUser());
                    }
                }
            } else {
                if (types[i].isAssociationType()) {
                    // if value has been changed
                    if (state[i] != null) {
                        isGrantedUpdateRelatedEntity = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.UPDATE),
                                                                                   resourceNameFactory.createResource((IdHolder) entity, fieldName));
                        if (!isGrantedUpdateRelatedEntity) {
                            throw new PermissionActionException("Entity " + entity + "'s field " + fieldName + " cannot be updated by " + userContext.getUser());
                        }
                    }
                }
            }
        }
        return super.onSave(entity, id, state, propertyNames, types);

    }

    // delete

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (!ChangeInterceptor.active) {
            super.onDelete(entity, id, state, propertyNames, types);
        }
        if (AnnotationUtils.findAnnotation(entity.getClass(), ResourceName.class) == null) {
            super.onDelete(entity, id, state, propertyNames, types);
        }
        if (ReflectionUtils.isSubtype(entity.getClass(), Permission.class)) {
            throw new IllegalArgumentException("Permission cannot be deleted by this persistence unit!");
        }
        UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);
        ResourceNameFactory resourceNameFactory = BeanProvider.getContextualReference(ResourceNameFactory.class);
        PermissionService permissionService = BeanProvider.getContextualReference(PermissionService.class);
        boolean isGranted = permissionService.isGranted(userContext.getUser(), actionFactory.createAction(ActionConstants.DELETE),
                                                        resourceNameFactory.createResource((IdHolder) entity));
        if (!isGranted) {
            throw new PermissionActionException("Entity " + entity + " is not permitted to be deleted by " + userContext.getUser());
        }

        // TODO decide if collection remove should be checked

        super.onDelete(entity, id, state, propertyNames, types);
    }

}
