package com.blazebit.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PermissionHandling {

    /**
     * Special contains method for permissions. Subject check eliminated, concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public boolean contains(Collection<Permission> permissions, Permission permission);

    /**
     * Special contains method for permissions. Subject check eliminated, concentrates on action and resource comparison with
     * the option to ignore permission and data permission type match.
     * 
     * @param permissions
     * @param permission
     * @param resourceTypeMatch
     * @return
     */
    public boolean contains(Collection<Permission> permissions, Permission permission, boolean resourceTypeMatch);

    /**
     * special 'containsAll' method for permissions. subject check eliminated, concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public boolean containsAll(Collection<Permission> permissions, Collection<Permission> selectedPermissions);

    /**
     * 
     * @param granted
     * @param revoked
     * @return
     */
    public Set<Permission> eliminateRevokeConflicts(Set<Permission> granted, Set<Permission> revoked);

    /**
     * find permission (action and resource match) with optional resource type matching
     * 
     * @param permissions
     * @param givenPermission
     * @param resourceTypeMatch
     * @return
     */
    public Permission findPermission(Collection<Permission> permissions, Permission givenPermission, boolean resourceTypeMatch);

    /**
     * Separates the grantable and the not grantable permissions of a collection considering the current permissions collection.
     * Selected permissions may contain already existing permissions.
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public List<Set<Permission>> getGrantable(Collection<Permission> permissions, Collection<Permission> toBeGranted);

    /**
     * Merges child permissions into parent permissions if all child permissions are given and eliminates permissions that imply
     * one another.
     * 
     * @param permissions
     * @return set of non redundant permissions
     */
    public Set<Permission> getNormalizedPermissions(Collection<Permission> permissions);

    /**
     * replaceable permissions when granting
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public Set<Permission> getReplacedByGranting(Collection<Permission> permissions, Collection<Permission> granted);

    /**
     * replaceable permissions when revoking
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public Set<Permission> getReplacedByRevoking(Collection<Permission> permissions, Collection<Permission> revoked);

    public List<Set<Permission>> getRevokableFromRevoked(Collection<Permission> permissions, Collection<Permission> toBeRevoked);

    public List<Set<Permission>> getRevokableFromRevoked(Collection<Permission> permissions, Collection<Permission> toBeRevoked, boolean force);

    /**
     * Separates the revokable and the not revokable permissions of a collection considering the current permissions collection
     * and a collection of selected permissions.
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public List<Set<Permission>> getRevokableFromSelected(Collection<Permission> permissions, Collection<Permission> selectedPermissions);

    /**
     * Based on the current permission list and the selected ones decides which ones have been revoked.
     * 
     * @param user
     * @param permissions
     * @param selectedPermissions
     * @return
     */
    public List<Set<Permission>> getRevokedAndGrantedAfterMerge(Collection<Permission> current, Set<Permission> revoked, Set<Permission> granted);

    /**
     * decides whether any permission in the given collection implies given permission
     * 
     * @param permissions
     * @param givenPermission
     * @return
     */
    public boolean implies(Collection<Permission> permissions, Permission givenPermission);

    /**
     * removes given permission from a permission collection (collection can contain user and usergroup permissions as well)
     * 
     * @param permissions
     * @param permission
     * @return
     */
    public Collection<Permission> remove(Collection<Permission> permissions, Permission permission);

    /**
     * returns a new collection without the permissions which have matching action and resource of the given collection
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    public Collection<Permission> remove(Collection<Permission> permissions, Permission permission, boolean resourceTypeMatch);

    /**
     * returns a new collection without the permissions which have matching action and resource of the given collection
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    public Collection<Permission> removeAll(Collection<Permission> permissions, Collection<Permission> permissionsToRemove);

    /**
     * returns a new collection without the permissions which have matching action and resource of the given collection
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    public Collection<Permission> removeAll(Collection<Permission> permissions, Collection<Permission> permissionsToRemove, boolean resourceTypeMatch);

    /**
     * decides whether any permission in the given collection will be revoked when revoking given permission
     * 
     * @param permissions
     * @param givenPermission
     * @return
     */
    public boolean replaces(Collection<Permission> permissions, Permission givenPermission);

    /**
     * 
     * @param permissions
     * @return
     */
    public Set<Permission> getParentPermissions(Collection<Permission> permissions);

    List<Set<Permission>> getSeparatedParentAndChildPermissions(Collection<Permission> permissions);

    List<List<Permission>> getSeparatedPermissions(Collection<Permission> permissions);

}
