package com.blazebit.security.impl.service.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.Resource;
import com.blazebit.security.impl.model.AbstractDataPermission;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.metamodel.ResourceMetamodel;

public class EntityFieldResourceHandlingUtils {

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
    public SortedMap<String, List<Permission>> groupPermissionsByResourceName(Collection<Permission> permissions) {
        SortedMap<String, List<Permission>> ret = new TreeMap<String, List<Permission>>(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        List<Permission> group;
        for (Permission p : permissions) {
            String entityName = ((EntityField) p.getResource()).getEntity();
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
                    return ((EntityField) o1.getResource()).getField().compareTo(((EntityField) o2.getResource()).getField());
                }
            });
            ret.put(entityName, group);

        }
        return ret;
    }

    /**
     * Groups permissions belonging to one resource name by their field property.
     * 
     * @param permissions
     * @return Map of fields associated with the permissions they appear in.
     */
    public SortedMap<String, List<Permission>> groupEntityPermissionsByField(Collection<Permission> permissions) {
        SortedMap<String, List<Permission>> ret = new TreeMap<String, List<Permission>>(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        List<Permission> group;
        for (Permission p : permissions) {
            String field = ((EntityField) p.getResource()).getField();
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
                    return ((EntityField) o1.getResource()).getField().compareTo(((EntityField) o2.getResource()).getField());
                }
            });
            ret.put(entityName, group);

        }
        return ret;
    }

    /**
     * Groups permissions belonging to one resource name by the actions they are combined with in the given permissions.
     * 
     * @param permissions
     * @return Map of actions associated with the permissions they appear in.
     */
    public SortedMap<Action, List<Permission>> groupResourcePermissionsByAction(Collection<Permission> permissions) {
        SortedMap<Action, List<Permission>> ret = new TreeMap<Action, List<Permission>>(new Comparator<Action>() {

            @Override
            public int compare(Action o1, Action o2) {
                return ((EntityAction) o1).getActionName().compareToIgnoreCase(((EntityAction) o2).getActionName());
            }
        });
        List<Permission> group;
        for (Permission p : permissions) {
            EntityAction entityAction = (EntityAction) p.getAction();
            if (ret.containsKey(entityAction)) {
                group = ret.get(entityAction);
            } else {
                group = new ArrayList<Permission>();

            }
            group.add(p);
            ret.put(entityAction, group);
        }
        for (Action entityAction : ret.keySet()) {
            group = ret.get(entityAction);
            Collections.sort(group, new Comparator<Permission>() {

                @Override
                public int compare(Permission o1, Permission o2) {
                    return ((EntityField) o1.getResource()).getField().compareTo(((EntityField) o2.getResource()).getField());
                }
            });
            ret.put(entityAction, group);

        }
        return ret;
    }

    /**
     * Looks up a permission with the given field in a list of permissions with the same resource name.
     * 
     * @param permissions
     * @param field
     * @return true if found.
     */
    public boolean findResourceWithField(Collection<Permission> permissions, String field) {
        for (Permission permission : permissions) {
            EntityField resource = (EntityField) permission.getResource();
            if (field.equals(resource.getField())) {
                return true;
            }

        }
        return false;
    }

    /**
     * find permission (action and resource match)
     * 
     * @param permissions
     * @param givenPermission
     * @param resourceTypeMatch
     * @return
     */
    public Permission findPermission(Collection<Permission> permissions, Permission givenPermission) {
        if (givenPermission == null) {
            return null;
        }
        for (Permission permission : permissions) {
            if (givenPermission.getAction().equals(permission.getAction())) {

                if (givenPermission.getResource().getClass().equals(permission.getResource().getClass())) {
                    if (givenPermission.getResource().equals(permission.getResource())) {
                        return permission;
                    }
                } else {
                    // different resource types, dont compare ID field, only the rest!
                    if (((EntityField) permission.getResource()).getEntity().equals(((EntityField) givenPermission.getResource()).getEntity())
                        && ((EntityField) permission.getResource()).getField().equals(((EntityField) givenPermission.getResource()).getField())) {
                        return permission;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 
     * @param action
     * @param resource
     * @return
     */
    public Set<Permission> getChildPermissions(Action action, Resource resource) {
        Set<Permission> grant = new HashSet<Permission>();
        EntityField entityField = (EntityField) resource;
        try {
            List<String> fields = new ArrayList<String>();
            if (actionUtils.getUpdateActionsForCollectionField().contains(action)) {
                fields = resourceMetamodel.getCollectionFields(entityField.getEntity());
            } else {
                if (actionUtils.getActionsForPrimitiveField().contains(action)) {
                    fields = resourceMetamodel.getPrimitiveFields(entityField.getEntity());
                }
            }
            // add rest of the fields
            for (String field : fields) {
                if (!field.equals(entityField.getField())) {
                    grant.add(permissionFactory.create(action, entityField.getParent().getChild(field)));
                }
            }
        } catch (ClassNotFoundException e) {
        }
        return grant;
    }

    /**
     * if all fields present->merges permissions into entity permission
     * 
     * @param permissions
     * @return
     */
    public Set<Permission> mergePermissions(Collection<Permission> permissions) {
        Set<Permission> remove = new HashSet<Permission>();
        Set<Permission> add = new HashSet<Permission>();

        Set<Permission> ret = new HashSet<Permission>(permissions);

        Map<String, List<Permission>> permissionsByEntity = groupPermissionsByResourceName(permissions);

        for (String entity : permissionsByEntity.keySet()) {
            Map<Action, List<Permission>> entityPermissionsByAction = groupResourcePermissionsByAction(permissionsByEntity.get(entity));

            for (Action action : entityPermissionsByAction.keySet()) {
                PermissionFamily permissionFamily = getSeparatedParentAndChildEntityPermissions(entityPermissionsByAction.get(action));
                if (permissionFamily.parent != null) {
                    // both entity resource and some or all entity fields exist -> merge into entity resource permission
                    remove.addAll(permissionFamily.children);
                } else {
                    // all PRIMITIVE entity fields are listed -> merge into entity resource permission
                    try {
                        boolean foundMissingField = false;
                        List<String> fields = resourceMetamodel.getPrimitiveFields(entity);
                        for (String field : fields) {
                            if (!findResourceWithField(permissionFamily.children, field)) {
                                foundMissingField = true;
                                break;
                            }
                        }
                        if (!foundMissingField) {
                            remove.addAll(permissionFamily.children);
                            add.add(permissionFactory.create(action, permissionFamily.children.iterator().next().getResource().getParent()));
                        }
                    } catch (ClassNotFoundException e) {
                        // cannot merge permissions
                    }
                }
            }
        }
        ret.removeAll(remove);
        ret.addAll(add);
        return ret;
    }

    public class PermissionFamily {

        Permission parent;
        Collection<Permission> children;

    }

    /**
     * Separates the parent and the child permissions of permission collection. Requirement: all permissions belong to the same
     * resource name.
     * 
     * @param permissions
     * @return
     */
    public PermissionFamily getSeparatedParentAndChildEntityPermissions(Collection<Permission> permissions) {
        PermissionFamily family = new PermissionFamily();
        Set<Permission> children = new HashSet<Permission>();
        for (Permission permission : permissions) {
            if (permission.getResource().getParent().equals(this)) {
                family.parent = permission;
            } else {
                children.add(permission);
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
    public List<List<Permission>> getSeparatedPermissionsByResource(Collection<Permission> permissions) {
        List<List<Permission>> ret = new ArrayList<List<Permission>>();
        List<Permission> entities = new ArrayList<Permission>();
        List<Permission> objects = new ArrayList<Permission>();
        for (Permission p : permissions) {
            if (p instanceof AbstractDataPermission) {
                objects.add(p);
            } else {
                entities.add(p);
            }
        }
        ret.add(entities);
        ret.add(objects);
        return ret;
    }

    public Set<Permission> getParentPermissions(Collection<Permission> permissions) {
        Set<Permission> ret = new HashSet<Permission>();
        for (Permission permission : permissions) {
            if (!actionUtils.getUpdateActionsForCollectionField().contains(permission.getAction())) {
                ret.add(permissionFactory.create(permission.getAction(), permission.getResource().getParent()));
            }
        }
        return ret;
    }

}
