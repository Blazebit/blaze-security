package com.blazebit.security.impl.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import com.blazebit.security.factory.PermissionFactory;
import com.blazebit.security.impl.factory.ActionUtils;
import com.blazebit.security.impl.model.AbstractEntityAction;
import com.blazebit.security.impl.model.AbstractEntityField;
import com.blazebit.security.impl.model.AbstractEntityObjectField;
import com.blazebit.security.metamodel.ResourceMetamodel;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;

public class EntityFieldResourceHandling {

	@Inject
	private ResourceMetamodel resourceMetamodel;

	@Inject
	private PermissionFactory permissionFactory;

	@Inject
	private ActionUtils actionUtils;

	/**
	 * Groups permissions by resource name.
	 * 
	 * @param permissions
	 * @return map of entity, permission list associations
	 */
	public SortedMap<String, List<Permission>> groupPermissionsByResourceName(
			Collection<Permission> permissions) {
		SortedMap<String, List<Permission>> ret = new TreeMap<String, List<Permission>>(
				new Comparator<String>() {

					@Override
					public int compare(String o1, String o2) {
						return o1.compareToIgnoreCase(o2);
					}
				});
		List<Permission> group;
		for (Permission p : permissions) {
			String entityName = ((AbstractEntityField) p.getResource())
					.getEntity();
			if (ret.containsKey(entityName)) {
				group = ret.get(entityName);
			} else {
				group = new ArrayList<Permission>();

			}
			group.add(p);
			ret.put(entityName, group);
		}
		for (String entityName : ret.keySet()) {
			group = ret.get(entityName);
			Collections.sort(group, new Comparator<Permission>() {

				@Override
				public int compare(Permission o1, Permission o2) {
					return ((AbstractEntityField) o1.getResource()).getField()
							.compareTo(
									((AbstractEntityField) o2.getResource())
											.getField());
				}
			});
			ret.put(entityName, group);

		}
		return ret;
	}

	/**
	 * Groups permissions belonging to one resource name by their field
	 * property.
	 * 
	 * @param permissions
	 * @return Map of fields associated with the permissions they appear in.
	 */
	public SortedMap<String, List<Permission>> groupEntityPermissionsByField(
			Collection<Permission> permissions) {
		SortedMap<String, List<Permission>> ret = new TreeMap<String, List<Permission>>(
				new Comparator<String>() {

					@Override
					public int compare(String o1, String o2) {
						return o1.compareToIgnoreCase(o2);
					}
				});
		List<Permission> group;
		for (Permission p : permissions) {
			String field = ((AbstractEntityField) p.getResource()).getField();
			if (ret.containsKey(field)) {
				group = ret.get(field);
			} else {
				group = new ArrayList<Permission>();

			}
			group.add(p);
			ret.put(field, group);
		}
		for (String entityName : ret.keySet()) {
			group = ret.get(entityName);
			Collections.sort(group, new Comparator<Permission>() {

				@Override
				public int compare(Permission o1, Permission o2) {
					return ((AbstractEntityField) o1.getResource()).getField()
							.compareTo(
									((AbstractEntityField) o2.getResource())
											.getField());
				}
			});
			ret.put(entityName, group);

		}
		return ret;
	}

	/**
	 * Groups permissions belonging to one resource name by the actions they are
	 * combined with in the given permissions.
	 * 
	 * @param permissions
	 * @return Map of actions associated with the permissions they appear in.
	 */
	public SortedMap<Action, List<Permission>> groupResourcePermissionsByAction(
			Collection<Permission> permissions) {
		SortedMap<Action, List<Permission>> ret = new TreeMap<Action, List<Permission>>(
				new Comparator<Action>() {

					@Override
					public int compare(Action o1, Action o2) {
						return ((AbstractEntityAction) o1).getActionName()
								.compareToIgnoreCase(
										((AbstractEntityAction) o2)
												.getActionName());
					}
				});
		List<Permission> group;
		for (Permission p : permissions) {
			AbstractEntityAction AbstractEntityAction = (AbstractEntityAction) p
					.getAction();
			if (ret.containsKey(AbstractEntityAction)) {
				group = ret.get(AbstractEntityAction);
			} else {
				group = new ArrayList<Permission>();

			}
			group.add(p);
			ret.put(AbstractEntityAction, group);
		}
		for (Action AbstractEntityAction : ret.keySet()) {
			group = ret.get(AbstractEntityAction);
			Collections.sort(group, new Comparator<Permission>() {

				@Override
				public int compare(Permission o1, Permission o2) {
					return ((AbstractEntityField) o1.getResource()).getField()
							.compareTo(
									((AbstractEntityField) o2.getResource())
											.getField());
				}
			});
			ret.put(AbstractEntityAction, group);

		}
		return ret;
	}

	/**
	 * 
	 * @param action
	 * @param resource
	 * @return
	 */
	public Set<Permission> getChildPermissions(Permission parentPermission) {
		Set<Permission> grant = new HashSet<Permission>();
		AbstractEntityField AbstractEntityField = (AbstractEntityField) parentPermission
				.getResource();
		Action action = parentPermission.getAction();
		if (!AbstractEntityField.getParent().equals(AbstractEntityField)) {
			throw new IllegalArgumentException(
					"Permission must be a parent resource permission");
		}
		try {
			List<String> fields = new ArrayList<String>();
			if (actionUtils.getUpdateActionsForCollectionField().contains(
					action)) {
				fields = resourceMetamodel
						.getCollectionFields(AbstractEntityField.getEntity());
			} else {
				if (actionUtils.getActionsForPrimitiveField().contains(action)) {
					fields = resourceMetamodel
							.getPrimitiveFields(AbstractEntityField.getEntity());
				}
			}
			// add rest of the fields
			for (String field : fields) {
				grant.add(permissionFactory.create(action,
						AbstractEntityField.getChild(field)));
			}
		} catch (ClassNotFoundException e) {
		}
		return grant;
	}

	public class PermissionFamily {

		public Permission parent;
		public Set<Permission> children = new HashSet<Permission>();

		public PermissionFamily() {
		}

		public PermissionFamily(Permission parent, Set<Permission> children) {
			this.parent = parent;
			this.children = children;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((children == null) ? 0 : children.hashCode());
			result = prime * result
					+ ((parent == null) ? 0 : parent.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PermissionFamily other = (PermissionFamily) obj;
			if (children == null) {
				if (other.children != null)
					return false;
			} else if (!children.equals(other.children))
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}

	}

	/**
	 * Separates the parent and the child permissions of permission collection.
	 * Requirement: all permissions belong to the same resource name.
	 * 
	 * @param permissions
	 * @return
	 */
	public PermissionFamily getSeparatedParentAndChildEntityPermissions(
			Collection<Permission> permissions) {
		PermissionFamily family = new PermissionFamily();
		Set<Permission> children = new HashSet<Permission>();

		if (!permissions.isEmpty()) {
			String firstResourceName = ((AbstractEntityField) permissions
					.iterator().next().getResource()).getEntity();

			for (Permission permission : permissions) {
				String currentResourceName = ((AbstractEntityField) permission
						.getResource()).getEntity();
				if (!currentResourceName.equals(firstResourceName)) {
					throw new IllegalArgumentException(
							"Resourcenames must match");
				}
				if (permission.getResource().getParent()
						.equals(permission.getResource())) {
					family.parent = permission;
				} else {
					children.add(permission);
				}
			}
		}
		family.children = children;
		return family;
	}

	/**
	 * separates permissions
	 * 
	 * @param permissions
	 * @return
	 */
	public List<List<Permission>> getSeparatedPermissionsByResource(
			Collection<Permission> permissions) {
		List<List<Permission>> ret = new ArrayList<List<Permission>>();
		List<Permission> entities = new ArrayList<Permission>();
		List<Permission> objects = new ArrayList<Permission>();
		for (Permission p : permissions) {
			if (p.getResource() instanceof AbstractEntityObjectField) {
				objects.add(p);
			} else {
				entities.add(p);
			}
		}
		ret.add(entities);
		ret.add(objects);
		return ret;
	}

	/**
	 * 
	 * @param permissions
	 * @return
	 */
	public Set<Permission> getParentPermissions(
			Collection<Permission> permissions) {
		Set<Permission> ret = new HashSet<Permission>();
		for (Permission permission : permissions) {
			if (!actionUtils.getUpdateActionsForCollectionField().contains(
					permission.getAction())) {
				ret.add(permissionFactory.create(permission.getAction(),
						permission.getResource().getParent()));
			}
		}
		return ret;
	}

}
