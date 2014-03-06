package com.blazebit.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

public final class PermissionUtils {
    private static final Logger LOG = Logger.getLogger(PermissionUtils.class.getName());

    private PermissionUtils() {

    }

    /**
     * Special contains method for permissions. Subject check eliminated, concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public static boolean contains(Collection<Permission> permissions, Permission permission) {
        return findPermission(permissions, permission) != null;
    }

    /**
     * special 'containsAll' method for permissions. subject check eliminated, concentrates on action and resource comparison.
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
     * returns a new collection without the permissions which have matching action and resource of the given collection
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

    public static boolean impliedByAny(List<Permission> permissions, Action action, Resource resource) {
        if (PermissionUtils.checkParameters(permissions, action, resource)) {
            return false;
        }
        
        for (Permission permission : permissions) {
            if (permission.getAction().equals(action) && permission.getResource().implies(resource)) {
                return true;
            }
        }

        return false;
    }

    public static boolean impliedByAny(Collection<Permission> permissions, Collection<Action> actions, Resource resource) {
        if (checkParameters(permissions, actions, resource)) {
            return false;
        }
        for (Permission permission : permissions) {
            for (Action action : actions) {
                if (permission.getAction().equals(action) && permission.getResource().implies(resource)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Set<Permission> getImpliedBy(List<Permission> permissions, Action action, Resource resource) {
        PermissionUtils.checkParameters(action, resource);
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

    public static Set<Permission> getReplaceablePermissions(List<Permission> permissions, Action action, Resource resource) {
        PermissionUtils.checkParameters(action, resource);
        Set<Permission> ret = new HashSet<Permission>();
        for (Permission permission : permissions) {
            if (permission.getResource().isReplaceableBy(resource) && permission.getAction().equals(action)) {
                ret.add(permission);
            }
        }
        return ret;
    }
    
    /**
     * 
     * @param permissions
     * @param action
     * @param resource
     * @return true if permission to be revoked for the given action and resource can be revoked ( it exists or its "subset"
     *         exists)
     */
    public static boolean isRevokable(List<Permission> permissions, Action action, Resource resource) {
        return !PermissionUtils.getRevokeImpliedPermissions(permissions, action, resource).isEmpty();
    }

    /**
     * 
     * @param permissions given permissions
     * @param subject
     * @param action
     * @param resource
     * @return true if permission to be created from the given action and resource can be granted to the subject with the given
     *         permissions
     */
    public static boolean isGrantable(List<Permission> permissions, Action action, Resource resource) {
        PermissionUtils.checkParameters(action, resource);
        if (!resource.isApplicable(action)) {
            LOG.warning("Action " + action + " cannot be applied to " + resource);
            return false;
        }
        return PermissionUtils.getImpliedBy(permissions, action, resource).isEmpty();
    }

    /**
     * 
     * @param permissions
     * @param action
     * @param resource
     * @return permission to be removed when revoking given permission parameters (removing itself if found or its "subset")
     */
    public static Set<Permission> getRevokeImpliedPermissions(List<Permission> permissions, Action action, Resource resource) {
        PermissionUtils.checkParameters(action, resource);
        Permission permission = PermissionUtils.findPermission(permissions, action, resource);
        
        if (permission != null) {
            // if exact permission found -> revoke that
            return Collections.singleton(permission);
        } else {
            return PermissionUtils.getReplaceablePermissions(permissions, action, resource);
        }
    }

    /**
     * decides whether any permission in the given collection implies given permission
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
    
    /*
     * Parameter checker methods
     */

    public static boolean checkParameters(Collection<Permission> permissions) {
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions cannot be null!");
        }
        return permissions.isEmpty();
    }

    public static boolean checkParameters(Collection<Permission> permissions, Collection<Action> actions) {
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions cannot be null!");
        }
        if (actions == null) {
            throw new IllegalArgumentException("Actions cannot be null!");
        }
        return permissions.isEmpty() || actions.isEmpty();
    }

    public static void checkParameters(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null!");
        }
    }

    public static void checkParameters(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null!");
        }
    }

    public static void checkParameters(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null!");
        }
    }

    public static void checkParameters(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null!");
        }
    }

    public static void checkParameters(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
    }

    public static void checkParameters(Action action, Resource resource) {
        checkParameters(action);
        checkParameters(resource);
    }

    public static void checkParameters(Subject subject, Action action, Resource resource) {
        checkParameters(subject);
        checkParameters(action, resource);
    }

    public static void checkParameters(Role role, Action action, Resource resource) {
        checkParameters(role);
        checkParameters(action, resource);
    }

    public static boolean checkParameters(Collection<Permission> permissions, Action action, Resource resource) {
        checkParameters(action, resource);
        return checkParameters(permissions);
    }

    public static boolean checkParameters(Collection<Permission> permissions, Collection<Action> actions, Resource resource) {
        checkParameters(resource);
        return checkParameters(permissions, actions);
    }
}
