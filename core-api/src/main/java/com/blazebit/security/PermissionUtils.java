package com.blazebit.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

public final class PermissionUtils {

    private PermissionUtils() {

    }

    /**
     * Special contains method for permissions. Subject check eliminated,
     * concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public static boolean contains(Collection<Permission> permissions, Permission permission) {
        return findPermission(permissions, permission) != null;
    }

    /**
     * special 'containsAll' method for permissions. subject check eliminated,
     * concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public static boolean containsAll(Collection<Permission> permissions, Collection<Permission> selectedPermissions) {
        for (Permission permission : selectedPermissions) {
            if (!PermissionUtils.contains(permissions, permission)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * removes given permission from a permission collection (collection can contain user and usergroup permissions as well)
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public static Collection<Permission> remove(Collection<Permission> permissions, Permission permission) {
        Permission found = PermissionUtils.findPermission(permissions, permission);
        Collection<Permission> result = new HashSet<Permission>(permissions);
        result.remove(found);
        return result;
    }

    /**
     * returns a new collection without the permissions which have matching
     * action and resource of the given collection
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    public static Collection<Permission> removeAll(Collection<Permission> permissions, Collection<Permission> permissionsToRemove) {
        Set<Permission> ret = new HashSet<Permission>(permissions);
        for (Permission permission : permissionsToRemove) {
            Permission found = PermissionUtils.findPermission(ret, permission);
            ret.remove(found);
        }
        return ret;
    }

    public static Permission findPermission(Collection<Permission> permissions, Permission givenPermission) {
        if (givenPermission == null) {
            return null;
        }
        for (Permission permission : permissions) {
            if (givenPermission.getAction().equals(permission.getAction())
                && givenPermission.getResource().equals(permission.getResource())) {
                return permission;
            }

        }
        return null;
    }

    public static Permission findPermission(List<Permission> permissions, Action action, Resource resource) {
        checkParameters(action, resource);
        for (Permission permission : permissions) {
            if (permission.getResource().equals(resource) && permission.getAction().equals(action)) {
                return permission;
            }
        }
        return null;
    }
    public static Set<Permission> getImpliedBy(List<Permission> permissions,
                                        Action action, Resource resource) {
        Set<Permission> ret = new HashSet<Permission>();
        Collection<Resource> connectedResources = resource.connectedResources();
        for (Resource connectedResource : connectedResources) {
            Permission connectedPermission = PermissionUtils.findPermission(permissions, action, connectedResource);
            if (connectedPermission != null) {
                ret.add(connectedPermission);
            }
        }
        return ret;
    }

    /**
     * decides whether any permission in the given collection implies given
     * permission
     * 
     * @param permissions
     * @param givenPermission
     * @return
     */
    public static boolean implies(Collection<Permission> permissions, Permission givenPermission) {
        Action action = givenPermission.getAction();
        Resource resource = givenPermission.getResource();
        PermissionUtils.checkParameters(action, resource);
        if (!resource.isApplicable(action)) {
            return false;
        }
        return !PermissionUtils.getImpliedBy(new ArrayList<Permission>(permissions), action, resource).isEmpty();
    }
    

    /**
     * Separates the parent and the child permissions of permission collection.
     * 
     * @param permissions
     * @return
     */
    public static List<Set<Permission>> getSeparatedParentAndChildPermissions(Collection<Permission> permissions) {
        Set<Permission> parents = new HashSet<Permission>();
        Set<Permission> children = new HashSet<Permission>();
        for (Permission permission : permissions) {
            if (permission.getResource().getParent().equals(permission.getResource())) {
                parents.add(permission);
            } else {
                children.add(permission);
            }
        }
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        ret.add(parents);
        ret.add(children);
        return ret;
    }

    public static void checkParameters(Role role, Action action, Resource resource) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        checkParameters(action, resource);
    }

    public static void checkParameters(Action action, Resource resource) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null!");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null!");
        }
    }

    public static void checkParameters(Subject subject, Action action, Resource resource) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null!");
        }
        checkParameters(action, resource);
    }
}
