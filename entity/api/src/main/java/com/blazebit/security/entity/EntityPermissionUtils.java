package com.blazebit.security.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;


public final class EntityPermissionUtils {

    private EntityPermissionUtils() {
        
    }

    /**
     * Groups permissions by resource name.
     * 
     * @param permissions
     * @return map of entity, permission list associations
     */
    public static SortedMap<String, List<Permission>> groupPermissionsByResourceName(
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
            String entityName = ((EntityResource) p.getResource())
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
                    return ((EntityResource) o1.getResource()).getField()
                            .compareTo(
                                    ((EntityResource) o2.getResource())
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
    public static SortedMap<Action, List<Permission>> groupResourcePermissionsByAction(
            Collection<Permission> permissions) {
        SortedMap<Action, List<Permission>> ret = new TreeMap<Action, List<Permission>>(
                new Comparator<Action>() {
    
                    @Override
                    public int compare(Action o1, Action o2) {
                        return o1.getName()
                                .compareToIgnoreCase(
                                        o2
                                                .getName());
                    }
                });
        List<Permission> group;
        for (Permission p : permissions) {
            Action AbstractEntityAction = p
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
                    return ((EntityResource) o1.getResource()).getField()
                            .compareTo(
                                    ((EntityResource) o2.getResource())
                                            .getField());
                }
            });
            ret.put(AbstractEntityAction, group);
    
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
    public static SortedMap<String, List<Permission>> groupEntityPermissionsByField(
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
            String field = ((EntityResource) p.getResource()).getField();
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
                    return ((EntityResource) o1.getResource()).getField()
                            .compareTo(
                                    ((EntityResource) o2.getResource())
                                            .getField());
                }
            });
            ret.put(entityName, group);
    
        }
        return ret;
    }


    /**
     * Separates the parent and the child permissions of permission collection. Requirement: all permissions belong to the same
     * resource name.
     * 
     * @param permissions
     * @return
     */
    public static List<List<Permission>> getSeparatedPermissionsByResource(
            Collection<Permission> permissions) {
        List<List<Permission>> ret = new ArrayList<List<Permission>>();
        List<Permission> entities = new ArrayList<Permission>();
        List<Permission> objects = new ArrayList<Permission>();
        for (Permission p : permissions) {
            if (p.getResource() instanceof EntityDataResource) {
                objects.add(p);
            } else {
                entities.add(p);
            }
        }
        ret.add(entities);
        ret.add(objects);
        return ret;
    }
    
    public static Map<Resource, Collection<Permission>> groupByParents(Collection<Permission> permissions) {
        if (permissions.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Resource, Collection<Permission>> result = new HashMap<Resource, Collection<Permission>>();
        
        for (Permission permission : permissions) {
            Resource key = permission.getResource().getParent();
            Collection<Permission> children = result.get(key);
            
            if (children == null) {
                children = new ArrayList<Permission>();
                result.put(key, children);
            }
            
            children.add(permission);
        }
        
        return result;
    }
    
}
