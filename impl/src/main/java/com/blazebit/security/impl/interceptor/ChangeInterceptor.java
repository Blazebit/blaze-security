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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.Type;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.annotation.Parent;
import com.blazebit.security.annotation.ResourceName;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.exception.PermissionActionException;
import com.blazebit.security.factory.ActionFactory;
import com.blazebit.security.factory.ResourceNameFactory;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.model.BaseEntity;
import com.blazebit.security.model.Permission;
import com.blazebit.security.service.PermissionService;

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
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		if (!ChangeInterceptor.active) {
			return super.onFlushDirty(entity, id, currentState, previousState,
					propertyNames, types);
		}
		if (AnnotationUtils.findAnnotation(entity.getClass(),
				ResourceName.class) == null) {
			return super.onFlushDirty(entity, id, currentState, previousState,
					propertyNames, types);
		}
		List<String> changedPropertyNames = new ArrayList<String>();
		if (previousState != null) {
			for (int i = 0; i < currentState.length; i++) {

				// we dont check collections here, there is a separate method
				// for it See: {@link #onCollectionUpdate(collection,
				// key) onCollectionUpdate}
				if (!types[i].isCollectionType()) {
					if ((currentState[i] != null && !currentState[i]
							.equals(previousState[i]))
							|| (currentState[i] == null && previousState[i] != null)) {
						changedPropertyNames.add(propertyNames[i]);
					}
				}
			}
		}
		UserContext userContext = BeanProvider
				.getContextualReference(UserContext.class);
		ActionFactory actionFactory = BeanProvider
				.getContextualReference(ActionFactory.class);
		ResourceNameFactory resourceNameFactory = BeanProvider
				.getContextualReference(ResourceNameFactory.class);
		PermissionService permissionService = BeanProvider
				.getContextualReference(PermissionService.class);
		boolean isGranted = changedPropertyNames.isEmpty();
		for (String propertyName : changedPropertyNames) {
			isGranted = permissionService.isGranted(actionFactory
					.createAction(ActionConstants.UPDATE), resourceNameFactory
					.createResource((BaseEntity) entity, propertyName));
			if (!isGranted) {
				break;
			}
		}
		if (isGranted) {
			return super.onFlushDirty(entity, id, currentState, previousState,
					propertyNames, types);
		} else {
			throw new PermissionActionException("Entity " + entity
					+ " is not permitted to be flushed by "
					+ userContext.getUser());
		}
	}

	/**
    * 
    */
	@Override
	public void onCollectionUpdate(Object collection, Serializable key)
			throws CallbackException {
		if (!ChangeInterceptor.active) {
			super.onCollectionUpdate(collection, key);
			return;
		}
		if (collection instanceof PersistentCollection) {
			PersistentCollection newValuesCollection = (PersistentCollection) collection;
			Object entity = newValuesCollection.getOwner();
			if (AnnotationUtils.findAnnotation(entity.getClass(),
					ResourceName.class) == null) {
				super.onCollectionUpdate(collection, key);
				return;
			}
			// copy new values and old values
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Collection<?> newValues = new HashSet(
					(Collection<?>) newValuesCollection.getValue());
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Set<?> oldValues = new HashSet(
					((Map<?, ?>) newValuesCollection.getStoredSnapshot())
							.keySet());

			String fieldName = StringUtils.replace(
					newValuesCollection.getRole(), entity.getClass().getName()
							+ ".", "");
			UserContext userContext = BeanProvider
					.getContextualReference(UserContext.class);
			ActionFactory actionFactory = BeanProvider
					.getContextualReference(ActionFactory.class);
			ResourceNameFactory resourceNameFactory = BeanProvider
					.getContextualReference(ResourceNameFactory.class);
			PermissionService permissionService = BeanProvider
					.getContextualReference(PermissionService.class);

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
				isGrantedToRemove = permissionService.isGranted(actionFactory
						.createAction(ActionConstants.REMOVE),
						resourceNameFactory.createResource((BaseEntity) entity,
								fieldName));
			}
			newValues.removeAll(retained);
			if (!newValues.isEmpty()) {
				isGrantedToAdd = permissionService.isGranted(actionFactory
						.createAction(ActionConstants.ADD), resourceNameFactory
						.createResource((BaseEntity) entity, fieldName));
			}

			if (!isGrantedToAdd) {
				throw new PermissionActionException(
						"Element cannot be added to entity " + entity
								+ "'s collection " + fieldName + " by "
								+ userContext.getUser());
			} else {
				if (!isGrantedToRemove) {
					throw new PermissionActionException(
							"Element cannot be removed from entity " + entity
									+ "'s collection " + fieldName + " by "
									+ userContext.getUser());
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
	public void onCollectionRecreate(Object collection, Serializable key)
			throws CallbackException {
		// TODO newly created entities with collections should be checked here
		// for permission but collection cannot give back
		// its
		// role in the parent entity. BUG? Workaround: it can be checked in the
		// #onSave(...) method
		// if (!ChangeInterceptor.active) {
		// super.onCollectionRecreate(collection, key);
		// }
		// if (collection instanceof PersistentCollection) {
		// PersistentCollection newValuesCollection = (PersistentCollection)
		// collection;
		// Object entity = newValuesCollection.getOwner();
		// if (AnnotationUtils.findAnnotation(entity.getClass(),
		// ResourceName.class) == null) {
		// super.onCollectionRecreate(collection, key);
		// }
		// if (ReflectionUtils.isSubtype(entity.getClass(), Permission.class)) {
		// throw new
		// IllegalArgumentException("Permission cannot be persisted by this persistence unit!");
		// }
		// @SuppressWarnings({ "unchecked", "rawtypes" })
		// Collection<?> newValues = new HashSet((Collection<?>)
		// newValuesCollection.getValue());
		// String fieldName = StringUtils.replace(newValuesCollection.getRole(),
		// entity.getClass().getName() + ".", "");
		// if (!newValues.isEmpty()) {
		// // element has been added to the collection - check Add permission
		// UserContext userContext =
		// BeanProvider.getContextualReference(UserContext.class);
		// ActionFactory actionFactory =
		// BeanProvider.getContextualReference(ActionFactory.class);
		// EntityResourceFactory entityFieldFactory =
		// BeanProvider.getContextualReference(EntityResourceFactory.class);
		// PermissionService permissionService =
		// BeanProvider.getContextualReference(PermissionService.class);
		// boolean isGranted =
		// permissionService.isGranted(userContext.getUser(),
		// actionFactory.createAction(ActionConstants.ADD),
		// entityFieldFactory.createResource((IdHolder) entity, fieldName));
		// if (!isGranted) {
		// // throw new PermissionException("Element cannot be added to Entity "
		// + entity + "'s collection " +
		// // fieldName + " by " + userContext.getUser());
		// }
		// }
		// }
		super.onCollectionRecreate(collection, key);
	}

	// it is invoked after deleting an entity with oneToMany children. method is
	// invoked after onDelete the respective child entity. kinda useless
	@Override
	public void onCollectionRemove(Object collection, Serializable key)
			throws CallbackException {
		// if (!ChangeInterceptor.active) {
		// super.onCollectionRemove(collection, key);
		// }
		// if (collection instanceof PersistentCollection) {
		// PersistentCollection newValuesCollection = (PersistentCollection)
		// collection;
		// Object entity = newValuesCollection.getOwner();
		// if (AnnotationUtils.findAnnotation(entity.getClass(),
		// ResourceName.class) == null) {
		// super.onCollectionRecreate(collection, key);
		// }
		// if (ReflectionUtils.isSubtype(entity.getClass(), Permission.class)) {
		// throw new IllegalArgumentException(
		// "Permission cannot be persisted by this persistence unit!");
		// }
		// @SuppressWarnings({ "unchecked", "rawtypes" })
		// Collection<?> newValues = new HashSet(
		// (Collection<?>) newValuesCollection.getValue());
		// String fieldName = StringUtils.replace(
		// newValuesCollection.getRole(), entity.getClass().getName()
		// + ".", "");
		// if (!newValues.isEmpty()) {
		// // element has been added to the collection - check Add
		// // permission
		// UserContext userContext = BeanProvider
		// .getContextualReference(UserContext.class);
		// ActionFactory actionFactory = BeanProvider
		// .getContextualReference(ActionFactory.class);
		// EntityResourceFactory entityFieldFactory = BeanProvider
		// .getContextualReference(EntityResourceFactory.class);
		// PermissionService permissionService = BeanProvider
		// .getContextualReference(PermissionService.class);
		// boolean isGranted = permissionService.isGranted(userContext
		// .getUser(), actionFactory
		// .createAction(ActionConstants.REMOVE),
		// entityFieldFactory.createResource((IdHolder) entity,
		// fieldName));
		// if (!isGranted) {
		// throw new PermissionException(
		// "Element cannot be added to Entity " + entity
		// + "'s collection " + fieldName + " by "
		// + userContext.getUser());
		// }
		// }
		// }
		super.onCollectionRemove(collection, key);
	}

	// add
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		if (!ChangeInterceptor.active) {
			return super.onSave(entity, id, state, propertyNames, types);
		}
		if (AnnotationUtils.findAnnotation(entity.getClass(),
				ResourceName.class) == null) {
			return super.onSave(entity, id, state, propertyNames, types);
		}
		if (ReflectionUtils.isSubtype(entity.getClass(), Permission.class)) {
			throw new IllegalArgumentException(
					"Permission cannot be persisted by this persistence unit!");
		}
		UserContext userContext = BeanProvider
				.getContextualReference(UserContext.class);
		ActionFactory actionFactory = BeanProvider
				.getContextualReference(ActionFactory.class);
		ResourceNameFactory resourceNameFactory = BeanProvider
				.getContextualReference(ResourceNameFactory.class);
		PermissionService permissionService = BeanProvider
				.getContextualReference(PermissionService.class);

		// check if collection relations have permission
		boolean isGrantedAddEntity = true;
		boolean isGrantedCreateRelatedEntity = true;
		boolean foundNotEmptyField = false;

		// check every field for create permission, if no field is filled out,
		// check for entity create permission
		for (int i = 0; i < state.length; i++) {
			String fieldName = propertyNames[i];
			if (types[i].isCollectionType()) {
				Collection<?> collection = (Collection<?>) state[i];
				if (!collection.isEmpty()) {
					// elements have been added
					isGrantedAddEntity = permissionService.isGranted(
							actionFactory.createAction(ActionConstants.CREATE),
							resourceNameFactory.createResource(
									(BaseEntity) entity, fieldName));
					if (!isGrantedAddEntity) {
						throw new PermissionActionException(
								"Element to Entity " + entity
										+ "'s collection " + fieldName
										+ " cannot be added by "
										+ userContext.getUser());
					}
				}
			} else {
				if (state[i] != null) {
					foundNotEmptyField = true;
					boolean isGranted = permissionService.isGranted(userContext
							.getUser(), actionFactory
							.createAction(ActionConstants.CREATE),
							resourceNameFactory.createResource(
									(BaseEntity) entity, fieldName));
					if (!isGranted) {
						throw new PermissionActionException("Entity " + entity
								+ "'s field " + fieldName
								+ " is not permitted to be persisted by "
								+ userContext.getUser());
					}
					// check relations
					if (types[i].isAssociationType()) {
						isGrantedCreateRelatedEntity = permissionService
								.isGranted(userContext.getUser(), actionFactory
										.createAction(ActionConstants.CREATE),
										resourceNameFactory.createResource(
												(BaseEntity) entity, fieldName));
						if (!isGrantedCreateRelatedEntity) {
							throw new PermissionActionException("Entity "
									+ entity + "'s field " + fieldName
									+ " cannot be updated by "
									+ userContext.getUser());
						}
						checkAssociation(entity, fieldName, types[i],
								ActionConstants.ADD, userContext,
								permissionService, resourceNameFactory,
								actionFactory);
					}

				}
			}
		}

		if (!foundNotEmptyField) {
			boolean isGranted = permissionService.isGranted(
					userContext.getUser(),
					actionFactory.createAction(ActionConstants.CREATE),
					resourceNameFactory.createResource((BaseEntity) entity));
			if (!isGranted) {
				throw new PermissionActionException("Entity " + entity
						+ " is not permitted to be persisted by "
						+ userContext.getUser());
			}
		}
		return super.onSave(entity, id, state, propertyNames, types);

	}

	private void checkAssociation(Object entity, String propertyName,
			Type type, ActionConstants action, UserContext userContext,
			PermissionService permissionService,
			ResourceNameFactory resourceNameFactory, ActionFactory actionFactory) {
		Map<Class<?>, Tuple> toBeChecked = new HashMap<Class<?>, Tuple>();
		// it can only be on collection types
		ManyToOne manyToOne;
		// look for annotation on getter level
		manyToOne = ReflectionUtils.getGetter(entity.getClass(), propertyName)
				.getAnnotation(ManyToOne.class);
		if (manyToOne == null) {
			// on field level
			manyToOne = ReflectionUtils.getField(entity.getClass(),
					propertyName).getAnnotation(ManyToOne.class);
		}

		if (manyToOne != null) {
			Object val = null;

			try {
				val = ReflectionUtils
						.getGetter(entity.getClass(), propertyName).invoke(
								entity);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}

			if (val != null) {
				toBeChecked.put(type.getReturnedClass(), new Tuple(val,
						propertyName));
			}

		}

		for (Map.Entry<Class<?>, Tuple> entry : toBeChecked.entrySet()) {
			Class<?> c = entry.getKey();

			for (Field f : ReflectionUtils.getInstanceFields(c)) {
				OneToMany oneToMany;
				if ((oneToMany = ReflectionUtils.getGetter(c, f.getName())
						.getAnnotation(OneToMany.class)) != null) {
					if (entity
							.getClass()
							.isAssignableFrom(
									ReflectionUtils
											.getResolvedFieldTypeArguments(c, f)[0])
							&& (oneToMany.mappedBy().isEmpty() ? true
									: oneToMany.mappedBy().equals(
											entry.getValue().fieldName))) {
						if (!permissionService.isGranted(userContext.getUser(),
								actionFactory.createAction(action),
								resourceNameFactory.createResource(
										(BaseEntity) entry.getValue().o,
										f.getName()))) {
							throw new PermissionActionException("Entity " + c
									+ "'s field " + f.getName() + " cannot be "
									+ action + "(e)d by "
									+ userContext.getUser());
						}
					}
				}
			}
		}

	}

	private Object getParent(Object entity, String propertyName) {
		Parent parent;
		// look for annotation on getter level
		parent = ReflectionUtils.getGetter(entity.getClass(), propertyName)
				.getAnnotation(Parent.class);
		if (parent == null) {
			// on field level
			parent = ReflectionUtils.getField(entity.getClass(), propertyName)
					.getAnnotation(Parent.class);
		}

		if (parent != null) {
			Object val = null;

			try {
				val = ReflectionUtils
						.getGetter(entity.getClass(), propertyName).invoke(
								entity);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			return val;
		}
		return null;
	}

	// delete

	private static class Tuple {

		Object o;
		String fieldName;

		public Tuple(Object o, String fieldName) {
			super();
			this.o = o;
			this.fieldName = fieldName;
		}
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		if (!ChangeInterceptor.active) {
			super.onDelete(entity, id, state, propertyNames, types);
		}
		if (AnnotationUtils.findAnnotation(entity.getClass(),
				ResourceName.class) == null) {
			super.onDelete(entity, id, state, propertyNames, types);
		}
		if (ReflectionUtils.isSubtype(entity.getClass(), Permission.class)) {
			throw new IllegalArgumentException(
					"Permission cannot be deleted by this persistence unit!");
		}
		UserContext userContext = BeanProvider
				.getContextualReference(UserContext.class);
		ActionFactory actionFactory = BeanProvider
				.getContextualReference(ActionFactory.class);
		ResourceNameFactory resourceNameFactory = BeanProvider
				.getContextualReference(ResourceNameFactory.class);
		PermissionService permissionService = BeanProvider
				.getContextualReference(PermissionService.class);
		boolean isGranted = permissionService.isGranted(userContext.getUser(),
				actionFactory.createAction(ActionConstants.DELETE),
				resourceNameFactory.createResource((BaseEntity) entity));
		if (!isGranted) {
			// try parent permission
			for (int i = 0; i < propertyNames.length; i++) {
				String propertyName = propertyNames[i];
				// it can only be on collection types
				if (types[i].isAssociationType()) {
					Object parent = getParent(entity, propertyName);
					if (parent != null) {
						isGranted = permissionService.isGranted(userContext
								.getUser(), actionFactory
								.createAction(ActionConstants.DELETE),
								resourceNameFactory
										.createResource((BaseEntity) parent));
						super.onDelete(entity, id, state, propertyNames, types);
						return;
					}
				}
			}
		}
		if (!isGranted)
			throw new PermissionActionException("Entity " + entity
					+ " is not permitted to be deleted by "
					+ userContext.getUser());

		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];
			// it can only be on collection types
			if (types[i].isAssociationType()) {
				checkAssociation(entity, propertyName, types[i],
						ActionConstants.REMOVE, userContext, permissionService,
						resourceNameFactory, actionFactory);
			}
		}

		super.onDelete(entity, id, state, propertyNames, types);
	}
}
