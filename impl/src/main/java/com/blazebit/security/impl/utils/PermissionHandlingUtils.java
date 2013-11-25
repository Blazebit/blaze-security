package com.blazebit.security.impl.utils;

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
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.metamodel.ResourceMetamodel;

public class PermissionHandlingUtils {

    @Inject
    private PermissionDataAccess permissionDataAccess;

    @Inject
    private ResourceMetamodel resourceMetamodel;

    @Inject
    private PermissionFactory permissionFactory;

    /**
     * special 'containsAll' method for permissions. subject check eliminated, concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public boolean containsAll(Collection<Permission> permissions, Collection<Permission> selectedPermissions) {
        boolean contains = true;
        for (Permission permission : selectedPermissions) {
            contains = contains(permissions, permission, true);
            if (!contains) {
                break;
            }
        }
        return contains;
    }

    /**
     * special 'contains' method for permissions. subject check eliminated, concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public boolean contains(Collection<Permission> permissions, Permission permission) {
        return contains(permissions, permission, true);
    }

    /**
     * special 'contains' method for permissions. subject check eliminated, concentrates on action and resource comparison with
     * the option to ignore permission and data permission type match.
     * 
     * @param permissions
     * @param permission
     * @param resourceTypeMatch
     * @return
     */
    public boolean contains(Collection<Permission> permissions, Permission permission, boolean resourceTypeMatch) {
        return findActionAndResourceMatch(permissions, permission, resourceTypeMatch) != null;
    }

    /**
     * decides whether any permission in the given collection implies given permission
     * 
     * @param permissions
     * @param givenPermission
     * @return
     */
    public boolean implies(Collection<Permission> permissions, Permission givenPermission) {
        // for (Permission permission : permissions) {
        // if (permission.getAction().implies(givenPermission.getAction()) &&
        // permission.getResource().implies(givenPermission.getResource())) {
        // return true;
        // }
        // }
        // return false;
        return !permissionDataAccess.isGrantable(new ArrayList<Permission>(permissions), givenPermission.getAction(), givenPermission.getResource());
    }

    /**
     * decides whether any permission in the given collection will be revoked when revoking given permission
     * 
     * @param permissions
     * @param givenPermission
     * @return
     */
    public boolean replaces(Collection<Permission> permissions, Permission givenPermission) {
        return !permissionDataAccess
            .getRevokablePermissionsWhenGranting(new ArrayList<Permission>(permissions), givenPermission.getAction(), givenPermission.getResource())
            .isEmpty();
    }

    /**
     * find permission (action and resource match) independent from entity id
     * 
     * @param permissions
     * @param givenPermission
     * @param resourceTypeMatch
     * @return
     */
    public Permission findActionAndResourceMatch(Collection<Permission> permissions, Permission givenPermission, boolean resourceTypeMatch) {
        for (Permission permission : permissions) {
            if (resourceTypeMatch) {
                if (givenPermission.getAction().equals(permission.getAction()) && givenPermission.getResource().equals(permission.getResource())) {
                    return permission;
                }
            } else {
                if (givenPermission.getAction().equals(permission.getAction())) {
                    if (givenPermission.getResource().getClass().equals(permission.getResource().getClass())) {
                        if (givenPermission.getResource().equals(permission.getResource())) {
                            return permission;
                        }
                    } else {
                        // dont compare ID field!
                        if (((EntityField) permission.getResource()).getEntity().equals(((EntityField) givenPermission.getResource()).getEntity())
                            && ((EntityField) permission.getResource()).getField().equals(((EntityField) givenPermission.getResource()).getField())) {
                            return permission;
                        }
                    }

                }
            }
        }
        return null;
    }

    /**
     * removes given permission from a permission collection (collection can contain user and usergroup permissions as well)
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public Collection<Permission> remove(Collection<Permission> permissions, Permission permission) {
        return remove(permissions, permission, true);
    }

    /**
     * removes given permission from a permission collection (collection can contain user and usergroup permissions as well)
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public List<Permission> remove(Collection<Permission> permissions, Permission permission, boolean resourceTypeMatch) {
        Permission found = findActionAndResourceMatch(permissions, permission, resourceTypeMatch);
        permissions.remove(found);
        return new ArrayList<Permission>(permissions);
    }

    /**
     * returns a new collection without the permissions which have matching action and resource of the given collection
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    public Collection<Permission> removeAll(Collection<Permission> permissions, Collection<Permission> permissionsToRemove) {
        return removeAll(permissions, permissionsToRemove, true);
    }

    /**
     * returns a new collection without the permissions which have matching action and resource of the given collection
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    public Collection<Permission> removeAll(Collection<Permission> permissions, Collection<Permission> permissionsToRemove, boolean resourceTypeMatch) {
        Set<Permission> ret = new HashSet<Permission>(permissions);
        for (Permission permission : permissionsToRemove) {
            Permission found = findActionAndResourceMatch(ret, permission, resourceTypeMatch);
            ret.remove(found);
        }
        return ret;
    }

    /**
     * separates permissions and data permissions
     * 
     * @param permissions
     * @return
     */
    public List<List<Permission>> filterPermissions(Collection<Permission> permissions) {
        List<List<Permission>> ret = new ArrayList<List<Permission>>();
        List<Permission> entities = new ArrayList<Permission>();
        List<Permission> objects = new ArrayList<Permission>();
        for (Permission p : permissions) {
            if (p.getResource() instanceof EntityObjectField) {
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
     * replaceable permissions when granting
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public Set<Permission> getReplacedByGranting(Collection<Permission> permissions, Collection<Permission> granted) {
        Set<Permission> revoked = new HashSet<Permission>();

        for (Permission selectedPermission : granted) {
            revoked.addAll(permissionDataAccess.getRevokablePermissionsWhenGranting(new ArrayList<Permission>(permissions), selectedPermission.getAction(),
                                                                                    selectedPermission.getResource()));
        }
        return revoked;
    }

    /**
     * replaceable permissions when revoking
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public Set<Permission> getReplacedByRevoking(Collection<Permission> permissions, Collection<Permission> revoked) {
        Set<Permission> replaceable = new HashSet<Permission>();
        for (Permission grantedPermission : revoked) {
            replaceable.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(new ArrayList<Permission>(permissions), grantedPermission.getAction(),
                                                                                        grantedPermission.getResource()));
        }
        return replaceable;
    }

    /**
     * Separates the grantable and the not grantable permissions of a collection considering the current permissions collection.
     * Selected permissions may contain already existing permissions.
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public List<Set<Permission>> getGrantableFromSelected(Collection<Permission> permissions, Collection<Permission> toBeGranted) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> granted = new HashSet<Permission>();
        Set<Permission> notGranted = new HashSet<Permission>();

        Set<Permission> currentPermissions = new HashSet<Permission>(permissions);
        for (Permission selectedPermission : toBeGranted) {
            // if is grantable
            if (permissionDataAccess.isGrantable(new ArrayList<Permission>(currentPermissions), selectedPermission.getAction(), selectedPermission.getResource())) {
                granted.add(selectedPermission);
            } else {
                notGranted.add(selectedPermission);
            }
        }
        ret.add(granted);
        ret.add(notGranted);
        return ret;

    }

    /**
     * Based on the current permission list and the selected ones decides which ones have been revoked.
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    private Set<Permission> getRevoked(Collection<Permission> permissions, Collection<Permission> selectedPermissions) {
        Set<Permission> ret = new HashSet<Permission>();

        for (Permission currentPermission : permissions) {
            if (!implies(selectedPermissions, currentPermission)) {
                ret.add(currentPermission);
            }
        }
        return ret;
    }

    /**
     * Separates the revokable and the not revokable permissions of a collection considering the current permissions collection
     * and a collection of selected permissions.
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public List<Set<Permission>> getRevokableFromSelected(Collection<Permission> permissions, Collection<Permission> selectedPermissions) {
        Set<Permission> toBeRevoked = getRevoked(permissions, selectedPermissions);
        return getRevokableFromRevoked(permissions, toBeRevoked);
    }

    /**
     * 
     * @param permissions
     * @param toBeRevoked
     * @return
     */
    public List<Set<Permission>> getRevokableFromRevoked(Collection<Permission> permissions, Collection<Permission> toBeRevoked) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();

        Set<Permission> revoked = new HashSet<Permission>();
        Set<Permission> notRevoked = new HashSet<Permission>();

        for (Permission selectedPermission : toBeRevoked) {
            if (permissionDataAccess.isRevokable(new ArrayList<Permission>(permissions), selectedPermission.getAction(), selectedPermission.getResource())) {
                revoked.add(selectedPermission);
            } else {
                notRevoked.add(selectedPermission);
            }
        }
        ret.add(revoked);
        ret.add(notRevoked);
        return ret;
    }

    /**
     * revoked permissions
     * 
     * @param user
     * @param userPermissions
     * @param selectedPermissions
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Set<Permission>> getRevokableFromRevoked(Collection<Permission> permissions, Collection<Permission> selectedPermissions, boolean force) {
        if (!force) {
            return getRevokableFromRevoked(permissions, selectedPermissions);
        } else {
            List<Set<Permission>> ret = new ArrayList<Set<Permission>>();

            Set<Permission> revoked = new HashSet<Permission>();
            Set<Permission> notRevoked = new HashSet<Permission>();
            Set<Permission> granted = new HashSet<Permission>();

            for (Permission selectedPermission : selectedPermissions) {
                if (permissionDataAccess.isRevokable(new ArrayList<Permission>(permissions), selectedPermission.getAction(), selectedPermission.getResource())) {
                    revoked.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(new ArrayList<Permission>(permissions), selectedPermission.getAction(),
                                                                                            selectedPermission.getResource()));
                } else {
                    EntityField resource = (EntityField) selectedPermission.getResource();
                    if (!resource.isEmptyField()) {
                        // if parent permission present
                        Permission parentPermission = permissionDataAccess.findPermission(new ArrayList<Permission>(permissions), selectedPermission.getAction(),
                                                                                          resource.getParent());
                        if (parentPermission != null) {
                            // permissionDataAccess.isRevokable(new ArrayList<Permission>(permissions),
                            // selectedPermission.getAction(), resource.getParent())
                            try {
                                if (findActionAndResourceMatch(granted, selectedPermission, true) == null) {
                                    List<String> fields = resourceMetamodel.getFields(resource.getEntity());
                                    // revoke parent entity
                                    revoked.add(parentPermission);
                                    // add rest of the fields
                                    for (String field : fields) {
                                        if (!field.equals(resource.getField())) {
                                            granted.add(permissionFactory.create(selectedPermission.getAction(), resource.getParent().getChild(field)));
                                        }
                                    }
                                } else {
                                    granted = new HashSet<Permission>(remove(granted, selectedPermission));
                                }
                            } catch (ClassNotFoundException e) {
                            }
                        } else {
                            notRevoked.add(selectedPermission);
                        }
                    } else {
                        notRevoked.add(selectedPermission);
                    }

                }
            }
            ret.add(revoked);
            ret.add(notRevoked);
            ret.add(granted);
            return ret;
        }
    }

    /**
     * 
     * @param granted
     * @param revoked
     * @return
     */
    public Set<Permission> getRevokedByEliminatingConflicts(Set<Permission> granted, Set<Permission> revoked) {
        Set<Permission> modifiedRevoked = new HashSet<Permission>(revoked);
        Set<Permission> dontRevoke = new HashSet<Permission>();
        for (Permission permission : revoked) {
            if (implies(granted, permission)) {
                dontRevoke.add(permission);
            }

        }
        modifiedRevoked.removeAll(dontRevoke);
        return modifiedRevoked;
    }

    /**
     * Reconsideres permissions to be granted and revoked based on the current permissions. For example: if subject/role has
     * entity field permissions and the missing entity field permissions are granted -> the existing entity field permissions
     * will be revoked and the entity permission will be granted.
     * 
     * @param permissions
     * @return set of non redundant permissions
     */
    public List<Set<Permission>> getRevokedAndGrantedAfterMerge(Collection<Permission> current, Set<Permission> revoked, Set<Permission> granted) {
        List<Permission> currentPermissions = new ArrayList<Permission>(current);
        Set<Permission> finalGranted = new HashSet<Permission>(granted);
        Set<Permission> finalRevoked = new HashSet<Permission>(revoked);
        // simulate revoke
        for (Permission revoke : revoked) {
            Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenRevoking(new ArrayList<Permission>(current), revoke.getAction(),
                                                                                                            revoke.getResource());
            for (Permission permission : removablePermissions) {
                currentPermissions.remove(permission);
            }
        }
        // simulate grant
        for (Permission grant : granted) {
            Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenGranting(new ArrayList<Permission>(current), grant.getAction(),
                                                                                                            grant.getResource());

            for (Permission existingPermission : removablePermissions) {
                currentPermissions.remove(existingPermission);
            }

            currentPermissions.add(grant);
        }

        List<Permission> actualAfterGrantAndRevoke = new ArrayList<Permission>(currentPermissions);

        currentPermissions = new ArrayList<Permission>(actualAfterGrantAndRevoke);
        List<List<Permission>> separatedPermissions = filterPermissions(currentPermissions);
        Set<Permission> merged = new HashSet<Permission>();
        merged.addAll(mergeFieldPermissionIntoEntityPermission(separatedPermissions.get(0)));
        merged.addAll(mergeFieldPermissionIntoEntityPermission(separatedPermissions.get(1)));

        Set<Permission> expectedAfterGrantAndRevoke = removeRedundantPermissions(merged);

        for (Permission permission : expectedAfterGrantAndRevoke) {

            if (!contains(actualAfterGrantAndRevoke, permission)) {
                finalGranted.add(permission);
            } else {
                actualAfterGrantAndRevoke.remove(permission);
            }

        }

        if (!actualAfterGrantAndRevoke.isEmpty()) {
            finalRevoked.addAll(actualAfterGrantAndRevoke);
        }

        Set<Permission> common = getCommonPermissions(finalGranted, finalRevoked);
        finalGranted.removeAll(common);
        finalRevoked.removeAll(common);

        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        ret.add(finalRevoked);
        ret.add(finalGranted);
        return ret;
    }

    private Set<Permission> getCommonPermissions(Set<Permission> granted, Set<Permission> revoked) {
        // Prepare a union
        Set<Permission> common = new HashSet<Permission>(granted);
        common.retainAll(revoked);
        return common;
    }

    /**
     * Merges field permissions into entity permissions and eliminates permissions that imply one another.
     * 
     * @param permissions
     * @return set of non redundant permissions
     */
    public Set<Permission> getNormalizedPermissions(Set<Permission> permissions) {
        Set<Permission> ret = new HashSet<Permission>();
        List<List<Permission>> separatedPermissions = filterPermissions(permissions);
        ret.addAll(mergeFieldPermissionIntoEntityPermission(separatedPermissions.get(0)));
        ret.addAll(mergeFieldPermissionIntoEntityPermission(separatedPermissions.get(1)));
        return removeRedundantPermissions(ret);
    }

    /**
     * if all fields present->merges permissions into entity permission
     * 
     * @param permissions
     * @return
     */
    private Set<Permission> mergeFieldPermissionIntoEntityPermission(Collection<Permission> permissions) {
        Set<Permission> remove = new HashSet<Permission>();
        Set<Permission> add = new HashSet<Permission>();

        Set<Permission> ret = new HashSet<Permission>(permissions);

        Map<String, List<Permission>> permissionsByEntity = groupPermissionsByEntity(permissions);

        for (String entity : permissionsByEntity.keySet()) {
            Map<Action, List<Permission>> permissionsByAction = groupPermissionsByAction(permissionsByEntity.get(entity));
            for (Action action : permissionsByAction.keySet()) {
                List<Permission> entityResource = filterPermissionsByEntityField(permissionsByAction.get(action)).get(0);
                List<Permission> entityFieldResource = filterPermissionsByEntityField(permissionsByAction.get(action)).get(1);
                if (!entityResource.isEmpty()) {
                    // both entity resource and some or all entity fields exist -> merge into entity resource permission
                    remove.addAll(entityFieldResource);
                } else {
                    // TODO actually there must be field permissions if entity permissions are missing. check not neccessary
                    if (!entityFieldResource.isEmpty()) {
                        // all entity fields are listed -> merge into entity resource permission
                        try {
                            boolean foundMissingField = false;
                            List<String> fields = resourceMetamodel.getPrimitiveFields(entity);
                            for (String field : fields) {
                                if (!findEntityWithFieldName(entityFieldResource, field)) {
                                    foundMissingField = true;
                                    break;
                                }
                            }
                            if (!foundMissingField) {
                                remove.addAll(entityFieldResource);
                                add.add(permissionFactory.create(action, ((EntityField) entityFieldResource.get(0).getResource()).getParent()));
                            }
                        } catch (ClassNotFoundException e) {
                            // cannot merge permissions
                        }
                    }

                }
            }
        }
        ret.removeAll(remove);
        ret.addAll(add);
        return ret;
    }

    /**
     * Filters out permissions that imply each other. The given collection of permissions belongs to a subject or a role.
     * 
     * @param permissions
     * @return
     */
    private Set<Permission> removeRedundantPermissions(Collection<Permission> permissions) {
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        Set<Permission> currentPermissions = new HashSet<Permission>(permissions);
        for (Permission permission : currentPermissions) {
            // remove current one
            currentPermissions = new HashSet<Permission>(permissions);
            currentPermissions.remove(permission);
            // check if any other permission is implied by this permission
            if (implies(currentPermissions, permission)) {
                redundantPermissions.add(permission);
            }
        }
        currentPermissions = new HashSet<Permission>(permissions);
        currentPermissions.removeAll(redundantPermissions);
        return currentPermissions;
    }

    /**
     * 
     * @param permissions
     * @return
     */
    public SortedMap<String, List<Permission>> groupPermissionsByEntity(Collection<Permission> permissions) {
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
     * 
     * @param permissions
     * @return
     */
    public SortedMap<Action, List<Permission>> groupPermissionsByAction(Collection<Permission> permissions) {
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
     * 
     * @param permissions
     * @return
     */
    public List<List<Permission>> filterPermissionsByEntityField(Collection<Permission> permissions) {
        List<List<Permission>> ret = new ArrayList<List<Permission>>();

        List<Permission> entityResource = new ArrayList<Permission>();
        List<Permission> entityFieldResource = new ArrayList<Permission>();

        for (Permission permission : permissions) {
            EntityField resource = (EntityField) permission.getResource();
            if (resource.isEmptyField()) {
                entityResource.add(permission);
            } else {
                entityFieldResource.add(permission);
            }
        }
        ret.add(entityResource);
        ret.add(entityFieldResource);
        return ret;
    }

    /**
     * 
     * @param permissions
     * @param fieldName
     * @return
     */
    public boolean findEntityWithFieldName(Collection<Permission> permissions, String fieldName) {
        for (Permission permission : permissions) {
            EntityField resource = (EntityField) permission.getResource();
            if (fieldName.equals(resource.getField())) {
                return true;
            }

        }
        return false;
    }

}
