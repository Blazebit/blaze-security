package com.blazebit.security.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.blazebit.security.PermissionUtils;
import com.blazebit.security.data.PermissionHandling;
import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.entity.EntityActionFactory;
import com.blazebit.security.entity.EntityPermissionUtils;
import com.blazebit.security.entity.EntityResource;
import com.blazebit.security.entity.EntityResourceMetamodel;
import com.blazebit.security.entity.UserContext;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Subject;
import com.blazebit.security.spi.ActionFactory;
import com.blazebit.security.spi.PermissionFactory;

public class PermissionHandlingImpl implements PermissionHandling {

    @Inject
    private PermissionFactory permissionFactory;

    @Inject
    private UserContext userContext;

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private ActionFactory actionFactory;

    @Inject
    private EntityResourceMetamodel resourceMetamodel;
    @Inject
    private EntityActionFactory actionUtils;

    private Action getGrantAction() {
        return actionFactory.createAction(Action.GRANT);
    }

    private Action getRevokeAction() {
        return actionFactory.createAction(Action.REVOKE);
    }

    private boolean isGranted(Subject subject, Action action, Resource resource) {
        List<Permission> permissions = permissionManager.getPermissions(subject);
        for (Permission permission : permissions) {
            if (permission.getAction().equals(action) && permission.getResource().implies(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Permission> eliminateRevokeConflicts(Set<Permission> granted, Set<Permission> revoked) {
        Set<Permission> modifiedRevoked = new HashSet<Permission>(revoked);
        Set<Permission> dontRevoke = new HashSet<Permission>();
        for (Permission permission : revoked) {
            if (PermissionUtils.implies(granted, permission)) {
                dontRevoke.add(permission);
            }

        }
        modifiedRevoked.removeAll(dontRevoke);
        return modifiedRevoked;
    }

    @Override
    public List<Set<Permission>> getGrantable(Collection<Permission> permissions, Collection<Permission> toBeGranted) {
        return getGrantable(userContext.getUser(), permissions, toBeGranted);
    }

    @Override
    public List<Set<Permission>> getGrantable(Subject authorizer, Collection<Permission> permissions, Collection<Permission> toBeGranted) {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> granted = new HashSet<Permission>();
        Set<Permission> notGranted = new HashSet<Permission>();

        for (Permission selectedPermission : toBeGranted) {
            if (isGranted(authorizer, getGrantAction(), selectedPermission.getResource())
                && PermissionUtils.isGrantable(new ArrayList<Permission>(permissions), selectedPermission.getAction(),
                                               selectedPermission.getResource())) {
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
     * Removes permissions from the given collection of permissions which imply each other.
     * 
     * @param permissions
     * @return
     */
    private Set<Permission> getMergedPermissions(Collection<Permission> permissions) {
        Set<Permission> ret = new HashSet<Permission>();
        List<List<Permission>> separatedPermissions = EntityPermissionUtils.getSeparatedPermissionsByResource(permissions);
        for (List<Permission> permissionList : separatedPermissions) {
            ret.addAll(mergePermissions(permissionList));
        }
        return ret;
    }

    /**
     * Filters out permissions that imply each other. The given collection of permissions belongs to a subject or a role.
     * 
     * @param permissions
     * @return
     */
    private static Set<Permission> getNonRedundantPermissions(Collection<Permission> permissions) {
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        Set<Permission> currentPermissions = new HashSet<Permission>(permissions);
        for (Permission permission : currentPermissions) {
            if (!PermissionUtils.contains(redundantPermissions, permission)) {
                // remove current one
                currentPermissions = new HashSet<Permission>(permissions);
                currentPermissions.remove(permission);
                // check if any other permission is implied by this permission
                if (PermissionUtils.implies(currentPermissions, permission)) {
                    redundantPermissions.add(permission);
                }
            }
        }
        currentPermissions = new HashSet<Permission>(permissions);
        currentPermissions.removeAll(redundantPermissions);
        return currentPermissions;
    }

    @Override
    public Set<Permission> getNormalizedPermissions(Collection<Permission> permissions) {
        return getNonRedundantPermissions(getMergedPermissions(permissions));
    }

    /**
     * 
     * @param permissions
     * @return
     */
    @Override
    public Set<Permission> getParentPermissions(Collection<Permission> permissions) {
        Set<Permission> ret = new HashSet<Permission>();
        for (Permission permission : permissions) {
            if (!actionUtils.getUpdateActionsForCollectionField().contains(permission.getAction())) {
                ret.add(permissionFactory.create(permission.getAction(), permission.getResource().getParent()));
            }
        }
        return getNormalizedPermissions(ret);
    }

    @Override
    public Set<Permission> getReplacedByGranting(Collection<Permission> permissions, Collection<Permission> granted) {
        return getReplacedByGranting(userContext.getUser(), permissions, granted);
    }

    @Override
    public Set<Permission> getReplacedByGranting(Subject authorizer, Collection<Permission> permissions, Collection<Permission> granted) {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        Set<Permission> revoked = new HashSet<Permission>();

        for (Permission selectedPermission : granted) {
            Set<Permission> toRevoke = PermissionUtils.getReplaceablePermissions(new ArrayList<Permission>(permissions),
                                                                                 selectedPermission.getAction(),
                                                                                 selectedPermission.getResource());
            revoked.addAll(toRevoke);
        }
        return revoked;
    }

    @Override
    public Set<Permission> getReplacedByRevoking(Collection<Permission> permissions, Collection<Permission> revoked) {
        Set<Permission> replaceable = new HashSet<Permission>();
        for (Permission grantedPermission : revoked) {
            replaceable.addAll(PermissionUtils.getRevokeImpliedPermissions(new ArrayList<Permission>(permissions),
                                                                           grantedPermission.getAction(),
                                                                           grantedPermission.getResource()));
        }
        return replaceable;
    }

    @Override
    public List<Set<Permission>> getRevokableFromRevoked(Collection<Permission> permissions, Collection<Permission> toBeRevoked) {
        return getRevokableFromRevoked(userContext.getUser(), permissions, toBeRevoked);
    }

    @Override
    public List<Set<Permission>> getRevokableFromRevoked(Subject authorizer, Collection<Permission> permissions, Collection<Permission> toBeRevoked) {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();

        Set<Permission> revoked = new HashSet<Permission>();
        Set<Permission> notRevoked = new HashSet<Permission>();

        for (Permission selectedPermission : toBeRevoked) {
            if (isGranted(authorizer, getRevokeAction(), selectedPermission.getResource())
                && PermissionUtils.isRevokable(new ArrayList<Permission>(permissions), selectedPermission.getAction(),
                                               selectedPermission.getResource())) {
                revoked.add(selectedPermission);
            } else {
                notRevoked.add(selectedPermission);
            }
        }
        ret.add(revoked);
        ret.add(notRevoked);
        return ret;
    }

    @Override
    public List<Set<Permission>> getRevokableFromRevoked(Collection<Permission> permissions, Collection<Permission> toBeRevoked, boolean force) {
        return getRevokableFromRevoked(userContext.getUser(), permissions, toBeRevoked, force);
    }

    @Override
    public List<Set<Permission>> getRevokableFromRevoked(Subject authorizer, Collection<Permission> permissions, Collection<Permission> toBeRevoked, boolean force) {
        if (authorizer == null) {
            throw new IllegalArgumentException("Authorizer cannot be null");
        }
        if (!force) {
            return getRevokableFromRevoked(permissions, toBeRevoked);
        } else {
            List<Set<Permission>> ret = new ArrayList<Set<Permission>>();

            Set<Permission> revoked = new HashSet<Permission>();
            Set<Permission> notRevoked = new HashSet<Permission>();
            Set<Permission> granted = new HashSet<Permission>();

            for (Permission selectedPermission : toBeRevoked) {
                if (isGranted(authorizer, getRevokeAction(), selectedPermission.getResource())
                    && PermissionUtils.isRevokable(new ArrayList<Permission>(permissions), selectedPermission.getAction(),
                                                   selectedPermission.getResource())) {
                    revoked.addAll(PermissionUtils.getRevokeImpliedPermissions(new ArrayList<Permission>(permissions),
                                                                               selectedPermission.getAction(),
                                                                               selectedPermission.getResource()));
                } else {
                    if (isGranted(authorizer, getRevokeAction(), selectedPermission.getResource())) {
                        if (PermissionUtils.findPermission(granted, selectedPermission) != null) {
                            granted = new HashSet<Permission>(PermissionUtils.remove(granted, selectedPermission));
                        } else {
                            Resource resource = selectedPermission.getResource();
                            if (!resource.getParent().equals(resource)) {
                                // if parent permission present
                                Permission parentPermission = PermissionUtils.findPermission(new ArrayList<Permission>(
                                    permissions), selectedPermission.getAction(), resource.getParent());
                                if (parentPermission != null) {
                                    // revoke parent entity

                                    Set<Permission> childPermissions = getAvailableChildPermissions(parentPermission);
                                    childPermissions = new HashSet<Permission>(PermissionUtils.remove(childPermissions,
                                                                                                      selectedPermission));

                                    boolean notAllowed = false;
                                    for (Permission childPermission : childPermissions) {
                                        if (!isGranted(authorizer, getGrantAction(), childPermission.getResource())) {
                                            notAllowed = true;
                                        }
                                        if (!notAllowed) {
                                            granted.addAll(childPermissions);
                                            revoked.add(parentPermission);
                                        } else {
                                            notRevoked.add(selectedPermission);
                                        }
                                    }
                                } else {
                                    notRevoked.add(selectedPermission);
                                }
                            } else {
                                notRevoked.add(selectedPermission);
                            }
                        }
                    }

                }
            }

            ret.add(revoked);
            ret.add(notRevoked);
            ret.add(granted);

            return ret;
        }
    }

    @Override
    public List<Set<Permission>> getRevokableFromSelected(Collection<Permission> permissions, Collection<Permission> selectedPermissions) {
        Set<Permission> toBeRevoked = getRevoked(permissions, selectedPermissions);
        return getRevokableFromRevoked(permissions, toBeRevoked);
    }

    private static Set<Permission> getRevoked(Collection<Permission> permissions, Collection<Permission> selectedPermissions) {
        Set<Permission> ret = new HashSet<Permission>();

        for (Permission currentPermission : permissions) {
            if (!PermissionUtils.implies(selectedPermissions, currentPermission)) {
                ret.add(currentPermission);
            }
        }
        return ret;
    }

    /**
     * Reconsiders permissions to be granted and revoked based on the current permissions. For example: if subject/role has
     * entity field permissions and the missing entity field permissions are granted -> the existing entity field permissions
     * will be revoked and the entity permission will be granted.
     * 
     * @param permissions
     * @return set of non redundant permissions
     */
    @Override
    public List<Set<Permission>> getRevokedAndGrantedAfterMerge(Collection<Permission> current, Set<Permission> revoked, Set<Permission> granted) {
        List<Permission> currentPermissions = new ArrayList<Permission>(current);
        Set<Permission> finalGranted = new HashSet<Permission>(granted);
        Set<Permission> finalRevoked = new HashSet<Permission>(revoked);
        // simulate revoke
        for (Permission revoke : revoked) {
            Set<Permission> removablePermissions = PermissionUtils.getRevokeImpliedPermissions(new ArrayList<Permission>(
                current), revoke.getAction(), revoke.getResource());
            for (Permission permission : removablePermissions) {
                currentPermissions.remove(permission);
            }
        }
        // simulate grant
        for (Permission grant : granted) {
            Set<Permission> removablePermissions = PermissionUtils
                .getReplaceablePermissions(new ArrayList<Permission>(current), grant.getAction(), grant.getResource());

            for (Permission existingPermission : removablePermissions) {
                currentPermissions.remove(existingPermission);
            }
            currentPermissions.add(grant);
        }

        List<Permission> actualAfterGrantAndRevoke = new ArrayList<Permission>(currentPermissions);
        currentPermissions = new ArrayList<Permission>(actualAfterGrantAndRevoke);

        Set<Permission> merged = getMergedPermissions(currentPermissions);

        Set<Permission> expectedAfterGrantAndRevoke = getNonRedundantPermissions(merged);

        for (Permission permission : expectedAfterGrantAndRevoke) {
            if (!PermissionUtils.contains(actualAfterGrantAndRevoke, permission)) {
                finalGranted.add(permission);
            } else {
                actualAfterGrantAndRevoke.remove(permission);
            }
        }

        if (!actualAfterGrantAndRevoke.isEmpty()) {
            finalRevoked.addAll(actualAfterGrantAndRevoke);
        }

        Set<Permission> common = new HashSet<Permission>(finalGranted);
        common.retainAll(finalRevoked);
        finalGranted.removeAll(common);
        finalRevoked.removeAll(common);

        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        ret.add(finalRevoked);
        ret.add(finalGranted);
        return ret;
    }

    /**
     * @deprecated This implementation leaked out to api users, we will have to reevaluate how we can model this
     */
    private static class PermissionFamily {

        public Permission parent;
        public Set<Permission> children = new HashSet<Permission>();

        public PermissionFamily() {
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((children == null) ? 0 : children.hashCode());
            result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
     * Separates the parent and the child permissions of permission collection. Requirement: all permissions belong to the same
     * resource name.
     * 
     * @deprecated This implementation leaked out to api users, we will have to reevaluate how we can model this
     * @param permissions
     * @return
     */
    private static PermissionFamily getSeparatedParentAndChildEntityPermissions(Collection<Permission> permissions) {
        PermissionFamily family = new PermissionFamily();
        Set<Permission> children = new HashSet<Permission>();

        if (!permissions.isEmpty()) {
            String firstResourceName = ((EntityResource) permissions.iterator().next().getResource()).getEntity();

            for (Permission permission : permissions) {
                String currentResourceName = ((EntityResource) permission.getResource()).getEntity();
                if (!currentResourceName.equals(firstResourceName)) {
                    throw new IllegalArgumentException("Resourcenames must match");
                }
                if (permission.getResource().getParent().equals(permission.getResource())) {
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
     * 
     * @param action
     * @param resource
     * @return
     */
    @Override
    public Set<Permission> getAvailableChildPermissions(Permission parentPermission) {
        Set<Permission> grant = new HashSet<Permission>();
        EntityResource EntityField = (EntityResource) parentPermission.getResource();
        Action action = parentPermission.getAction();
        if (!EntityField.getParent().equals(EntityField)) {
            throw new IllegalArgumentException("Permission must be a parent resource permission");
        }
        try {
            List<String> fields = new ArrayList<String>();
            if (actionUtils.getUpdateActionsForCollectionField().contains(action)) {
                fields = resourceMetamodel.getCollectionFields(EntityField.getEntity());
            } else {
                if (actionUtils.getActionsForPrimitiveField().contains(action)) {
                    fields = resourceMetamodel.getPrimitiveFields(EntityField.getEntity());
                }
            }
            // add rest of the fields
            for (String field : fields) {
                grant.add(permissionFactory.create(action, EntityField.withField(field)));
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
    private Set<Permission> mergePermissions(Collection<Permission> permissions) {
        Set<Permission> remove = new HashSet<Permission>();
        Set<Permission> add = new HashSet<Permission>();

        Set<Permission> ret = new HashSet<Permission>(permissions);

        Map<String, List<Permission>> permissionsByEntity = EntityPermissionUtils.groupPermissionsByResourceName(permissions);

        for (String entity : permissionsByEntity.keySet()) {
            Map<Action, List<Permission>> entityPermissionsByAction = EntityPermissionUtils
                .groupResourcePermissionsByAction(permissionsByEntity.get(entity));

            for (Action action : entityPermissionsByAction.keySet()) {
                PermissionFamily permissionFamily = getSeparatedParentAndChildEntityPermissions(entityPermissionsByAction
                    .get(action));
                if (permissionFamily.parent != null) {
                    // both entity resource and some or all entity fields exist -> merge into entity resource permission
                    remove.addAll(permissionFamily.children);
                } else {
                    if (!permissionFamily.children.isEmpty()) {
                        Permission parentPermission = permissionFactory.create(action, permissionFamily.children
                            .iterator()
                            .next()
                            .getResource()
                            .getParent());
                        // all PRIMITIVE entity fields are listed -> merge into entity resource permission
                        Set<Permission> childPermissions = getAvailableChildPermissions(parentPermission);
                        boolean foundMissingField = !PermissionUtils.containsAll(permissionFamily.children, childPermissions);
                        if (!foundMissingField) {
                            remove.addAll(permissionFamily.children);
                            add.add(parentPermission);
                        }

                    }
                }
            }
        }
        ret.removeAll(remove);
        ret.addAll(add);
        return ret;
    }

    @Override
    public boolean replaces(Collection<Permission> permissions, Permission givenPermission) {
        return !PermissionUtils.getReplaceablePermissions(new ArrayList<Permission>(permissions), givenPermission.getAction(),
                                                          givenPermission.getResource()).isEmpty();
    }

}
