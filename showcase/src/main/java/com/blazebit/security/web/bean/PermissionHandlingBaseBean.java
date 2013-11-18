/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.lang.reflect.Field;
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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.TreeNode;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.ResourceFactory;
import com.blazebit.security.Role;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.bean.ResourceNameExtension.EntityResource;
import com.blazebit.security.web.service.api.ActionUtils;
import com.blazebit.security.web.util.FieldUtils;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "permissionHandlingBaseBean")
@Named
public class PermissionHandlingBaseBean extends TreeHandlingBaseBean {

    @Inject
    protected PermissionFactory permissionFactory;
    @Inject
    protected UserSession userSession;
    @Inject
    protected PermissionDataAccess permissionDataAccess;
    @Inject
    protected PermissionManager permissionManager;
    @Inject
    protected EntityResourceFactory entityFieldFactory;
    @Inject
    protected ResourceFactory resourceFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;
    @Inject
    protected ActionUtils actionUtils;
    @Inject
    protected ResourceNameExtension resourceNameExtension;

    private List<Permission> notGranted = new ArrayList<Permission>();
    private List<Permission> notRevoked = new ArrayList<Permission>();

    /**
     * special 'contains' method for permissions. subject check eliminated, concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @return
     */
    protected boolean contains(Collection<Permission> permissions, Permission permission) {
        return contains(permissions, permission, true);
    }

    /**
     * special 'contains' method for permissions. subject check eliminated, concentrates on action and resource comparison.
     * 
     * @param permissions
     * @param permission
     * @param resourceTypeMatch
     * @return
     */
    protected boolean contains(Collection<Permission> permissions, Permission permission, boolean resourceTypeMatch) {
        return findActionAndResourceMatch(permissions, permission, resourceTypeMatch) != null;
    }

    protected boolean implies(Collection<Permission> permissions, Permission givenPermission) {
        for (Permission permission : permissions) {
            if (permission.getAction().implies(givenPermission.getAction()) && permission.getResource().implies(givenPermission.getResource())) {
                return true;
            }
        }
        return false;
    }

    protected boolean revokes(Collection<Permission> permissions, Permission givenPermission) {
        for (Permission permission : permissions) {
            if (permission.getAction().equals(givenPermission.getAction()) && permission.getResource().isReplaceableBy(givenPermission.getResource())) {
                return true;
            }
        }
        return false;
    }

    protected Permission findActionAndResourceMatch(Collection<Permission> permissions, Permission givenPermission, boolean resourceTypeMatch) {
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
     * removes given permission from a permission collection (collection contains user and usergroup permissions)
     * 
     * @param permissions
     * @param permission
     * @return
     */
    protected List<Permission> remove(List<Permission> permissions, Permission permission) {
        Permission found = findActionAndResourceMatch(permissions, permission, true);
        permissions.remove(found);
        return permissions;
    }

    /**
     * removes given permissions from a permission collection (collection contains user and usergroup permissions)
     * 
     * @param permissions
     * @param permissionsToRemove
     * @return
     */
    protected Collection<Permission> removeAll(Collection<Permission> permissions, Collection<Permission> permissionsToRemove) {
        for (Permission permission : permissionsToRemove) {
            Permission found = findActionAndResourceMatch(permissions, permission, true);
            permissions.remove(found);
        }
        return permissions;
    }

    // TODO enough size check?
    protected boolean allChildFieldsListed(TreeNode actionNode, String entityName) {
        if (actionNode.getChildCount() == 0) {
            return true;
        }
        List<String> fields;
        try {
            fields = FieldUtils.getPrimitiveFieldNames(Class.forName(resourceNameExtension.getEntityResourceByResourceName(entityName).getEntityClassName()));
            return actionNode.getChildCount() == fields.size();
        } catch (ClassNotFoundException e) {
        }
        return false;

    }

    /**
     * eliminates permissions that imply one another
     * 
     * @param permissions
     * @return set of non redundant permissions
     */
    protected Set<Permission> mergePermissions(Set<Permission> permissions) {

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
                    // all entity fields are listed -> merge into entity resource permission
                    try {
                        boolean foundMissingField = false;
                        List<Field> fields = FieldUtils.getPrimitiveFields(Class.forName(resourceNameExtension.getEntityResourceByResourceName(entity).getEntityClassName()));
                        for (Field field : fields) {
                            if (!findEntityWithFieldName(entityFieldResource, field.getName())) {
                                foundMissingField = true;
                                break;
                            }
                        }
                        if (!foundMissingField) {
                            remove.addAll(entityFieldResource);
                            add.add(permissionFactory.create(action, entityFieldFactory.createResource(entity)));
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

    protected List<List<Permission>> filterPermissionsByEntityField(List<Permission> permissions) {
        List<List<Permission>> ret = new ArrayList<List<Permission>>();
        List<Permission> entityResorce = new ArrayList<Permission>();
        List<Permission> entityFieldResorce = new ArrayList<Permission>();
        for (Permission permission : permissions) {
            EntityField resource = (EntityField) permission.getResource();
            if (resource.isEmptyField()) {
                entityResorce.add(permission);
            } else {
                entityFieldResorce.add(permission);
            }
        }
        ret.add(entityResorce);
        ret.add(entityFieldResorce);
        return ret;
    }

    protected boolean findEntityWithFieldName(List<Permission> permissions, String fieldName) {
        for (Permission permission : permissions) {
            EntityField resource = (EntityField) permission.getResource();
            if (fieldName.equals(resource.getField())) {
                return true;
            }

        }
        return false;
    }

    protected SortedMap<String, List<Permission>> groupPermissionsByEntity(Collection<Permission> permissions) {
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

    protected SortedMap<Action, List<Permission>> groupPermissionsByAction(Collection<Permission> permissions) {
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

    // protected Set<Permission> getReplaceablePermissions(User user, List<Permission> permissions, Set<Permission>
    // selectedPermissions) {
    // Set<Permission> ret = new HashSet<Permission>();
    // for (Permission permission : selectedPermissions) {
    // Set<Permission> revokable = permissionDataAccess.getRevokablePermissionsWhenGranting(user, permission.getAction(),
    // permission.getResource());
    // for (Permission currentPermission : permissions) {
    // if (contains(revokable, currentPermission)) {
    // ret.add(currentPermission);
    // }
    // }
    // }
    // return ret;
    // }

    // protected Set<Permission> getReplaceablePermissions(UserGroup group, List<Permission> permissions, Set<Permission>
    // selectedPermissions) {
    // Set<Permission> ret = new HashSet<Permission>();
    // for (Permission permission : selectedPermissions) {
    // Set<Permission> revokable = permissionDataAccess.getRevokablePermissionsWhenGranting(group, permission.getAction(),
    // permission.getResource());
    // for (Permission currentPermission : permissions) {
    // if (contains(revokable, currentPermission)) {
    // ret.add(currentPermission);
    // }
    // }
    // }
    // return ret;
    // }

    /**
     * separates permissions and data permissions
     * 
     * @param permissions
     * @return
     */
    protected List<List<Permission>> filterPermissions(List<Permission> permissions) {
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
     * revoked permissions
     * 
     * @param user
     * @param userPermissions
     * @param selectedPermissions
     * @return
     */
    protected List<Set<Permission>> getRevokedPermissions(Collection<Permission> userPermissions, Collection<Permission> selectedPermissions) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> revoked = new HashSet<Permission>();
        Set<Permission> notRevoked = new HashSet<Permission>();
        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        for (Permission currentPermission : currentPermissions) {
            // if the existing permission cannot be found among the selected ones and user has permission to grant it -> it has
            // been revoked
            if (!implies(selectedPermissions, currentPermission) && isGranted(ActionConstants.GRANT, currentPermission.getResource())) {
                if (permissionDataAccess.isRevokable(new ArrayList<Permission>(userPermissions), currentPermission.getAction(), currentPermission.getResource())) {
                    revoked.add(currentPermission);
                } else {
                    notRevoked.add(currentPermission);
                }

            }
        }
        ret.add(revoked);
        ret.add(notRevoked);
        return ret;
    }

    /**
     * replaceable permissions
     * 
     * @param user
     * @param userPermissions
     * @param selectedPermissions
     * @return
     */
    protected Set<Permission> getReplacedPermissions(Collection<Permission> userPermissions, Collection<Permission> selectedPermissions) {
        Set<Permission> grantedPermissions = getGrantedPermission(userPermissions, selectedPermissions).get(0);
        Set<Permission> replaceable = new HashSet<Permission>();
        for (Permission grantedPermission : grantedPermissions) {
            replaceable.addAll(permissionDataAccess.getRevokablePermissionsWhenGranting(new ArrayList<Permission>(userPermissions), grantedPermission.getAction(),
                                                                                        grantedPermission.getResource()));
        }
        return replaceable;
    }

    /**
     * replaceable permissions
     * 
     * @param user
     * @param userPermissions
     * @param selectedPermissions
     * @return
     */
    protected Set<Permission> getReplacedPermissions(Role role, Collection<Permission> grantedPermissions) {
        Set<Permission> replaceable = new HashSet<Permission>();
        for (Permission grantedPermission : grantedPermissions) {
            replaceable.addAll(permissionDataAccess.getRevokablePermissionsWhenGranting(role, grantedPermission.getAction(), grantedPermission.getResource()));
        }
        return replaceable;
    }

    /**
     * newly granted permissions. the newly granted permissions are selected as if the revoked ones are already revoked.
     * 
     * @param user
     * @param userPermissions
     * @param selectedPermissions
     * @return
     */
    protected List<Set<Permission>> getGrantedPermission(Collection<Permission> userPermissions, Collection<Permission> selectedPermissions) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> granted = new HashSet<Permission>();
        Set<Permission> notGranted = new HashSet<Permission>();
        Set<Permission> revokedPermissions = getRevokedPermissions(userPermissions, selectedPermissions).get(0);
        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        currentPermissions.removeAll(revokedPermissions);
        for (Permission selectedPermission : selectedPermissions) {
            if (!implies(currentPermissions, selectedPermission)) {
                if (permissionDataAccess.isGrantable(new ArrayList<Permission>(currentPermissions), selectedPermission.getAction(), selectedPermission.getResource())) {
                    granted.add(selectedPermission);
                } else {
                    notGranted.add(selectedPermission);
                }
            }
        }
        ret.add(granted);
        ret.add(notGranted);
        return ret;
    }

    /**
     * newly granted permissions. the newly granted permissions are selected as if the revoked ones are already revoked. USE
     * THIS WHEN ONLY GRANTING WITHOUT REVOKING
     * 
     * @param user
     * @param userPermissions
     * @param selectedPermissions
     * @return
     */
    protected List<Set<Permission>> getGrantablePermissions(Collection<Permission> userPermissions, Collection<Permission> selectedPermissions) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> granted = new HashSet<Permission>();
        Set<Permission> notGranted = new HashSet<Permission>();
        Set<Permission> currentPermissions = new HashSet<Permission>(userPermissions);
        for (Permission selectedPermission : selectedPermissions) {
            if (!implies(currentPermissions, selectedPermission)) {
                if (permissionDataAccess.isGrantable(new ArrayList<Permission>(currentPermissions), selectedPermission.getAction(), selectedPermission.getResource())) {
                    granted.add(selectedPermission);
                } else {
                    notGranted.add(selectedPermission);
                }
            }
        }
        ret.add(granted);
        ret.add(notGranted);
        return ret;
    }

    public List<Permission> getNotGranted() {
        return this.notGranted;
    }

    public void setNotGranted(Set<Permission> notGranted) {
        List<Permission> ret = new ArrayList<Permission>(notGranted);
        Collections.sort(ret, new Comparator<Permission>() {

            @Override
            public int compare(Permission o1, Permission o2) {
                return ((EntityField) o1.getResource()).getEntity().compareToIgnoreCase(((EntityField) o1.getResource()).getEntity());
            }

        });
        this.notGranted = ret;

    }

    public List<Permission> getNotRevoked() {
        return notRevoked;
    }

    public void setNotRevoked(Set<Permission> notRevoked) {
        List<Permission> ret = new ArrayList<Permission>(notRevoked);
        Collections.sort(ret, new Comparator<Permission>() {

            @Override
            public int compare(Permission o1, Permission o2) {
                return ((EntityField) o1.getResource()).getEntity().compareToIgnoreCase(((EntityField) o1.getResource()).getEntity());
            }

        });
        this.notRevoked = ret;
    }

    protected Set<Permission> getRedundantPermissions(Set<Permission> permissions) {
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
        return redundantPermissions;
    }

    protected Set<Permission> getGrantablePermissions(Collection<Permission> currentPermissions, User user, Collection<Permission> toGrant) {
        Set<Permission> ret = new HashSet<Permission>();
        for (Permission permission : toGrant) {
            if (permissionDataAccess.isGrantable(new ArrayList<Permission>(currentPermissions), permission.getAction(), permission.getResource())) {
                ret.add(permission);
            }
        }
        return ret;
    }

    protected List<Set<Permission>> getRevokedAndGrantedPermissionsWhenRevoking(List<Permission> userPermissions, User user, Set<Permission> revokedPermissions) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> revokeOK = new HashSet<Permission>();
        Set<Permission> revoked = new HashSet<Permission>();
        Set<Permission> granted = new HashSet<Permission>();
        for (Permission permissionToRevoke : revokedPermissions) {
            if (permissionDataAccess.isRevokable(user, permissionToRevoke.getAction(), permissionToRevoke.getResource())) {
                revokeOK.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(user, permissionToRevoke.getAction(), permissionToRevoke.getResource()));
            } else {
                // if entity with field needs to be revoked check for entity
                EntityField resource = (EntityField) permissionToRevoke.getResource();
                if (!resource.isEmptyField()) {
                    Permission entityPermission = permissionDataAccess.findPermission(user, permissionToRevoke.getAction(), resource.getParent());
                    if (entityPermission != null) {
                        revoked.add(entityPermission);
                        EntityResource entityResource = resourceNameExtension.getEntityResourceByResourceName(resource.getEntity());
                        List<Field> primitiveFields;
                        try {
                            primitiveFields = FieldUtils.getPrimitiveFields(Class.forName(entityResource.getEntityClassName()));
                            for (Field field : primitiveFields) {
                                Permission permission = permissionFactory.create(permissionToRevoke.getAction(), resource.getChild(field.getName()));
                                granted.add(permission);
                            }
                        } catch (ClassNotFoundException e) {
                            // TODO what now?
                        }
                    }
                }
            }
        }
        ret.add(revokeOK);
        ret.add(revoked);
        ret.add(granted);
        return ret;

    }

    protected Set<Permission> grantImpliedPermissions(List<Permission> current, Set<Permission> granted) {
        Set<Permission> ret = new HashSet<Permission>(granted);
        for (Permission permission : granted) {
            List<Action> impliedActions = resourceNameExtension.getImpliedActions(permission.getAction());
            if (!impliedActions.isEmpty()) {
                for (Action impliedAction : impliedActions) {
                    Permission impliedPermission = permissionFactory.create(impliedAction, permission.getResource());
                    if (!implies(current, impliedPermission) && !implies(granted, impliedPermission)) {
                        ret.add(impliedPermission);
                    }
                }

            }
        }
        return ret;
    }

    protected Set<Permission> revokeImpliedPermissions(List<Permission> current, Set<Permission> revoked) {
        Set<Permission> ret = new HashSet<Permission>(revoked);
        for (Permission permission : revoked) {
            List<Action> impliedActions = resourceNameExtension.getImpliedActions(permission.getAction());
            if (!impliedActions.isEmpty()) {
                for (Action impliedAction : impliedActions) {
                    Permission impliedPermission = permissionFactory.create(impliedAction, permission.getResource());
                    if (implies(current, impliedPermission)) {
                        ret.addAll(permissionDataAccess.getRevokablePermissionsWhenRevoking(current, impliedPermission.getAction(), impliedPermission.getResource()));
                        // ret.add(impliedPermission);
                    }
                }

            }
        }
        return ret;
    }

    protected List<Set<Permission>> normalizePermissions(Collection<Permission> permissions) {
        List<Set<Permission>> ret = new ArrayList<Set<Permission>>();
        Set<Permission> grant = new HashSet<Permission>();
        Set<Permission> revoke = new HashSet<Permission>();
        Map<String, List<Permission>> entityPermissionsMap = groupPermissionsByEntity(permissions);
        for (String entityName : entityPermissionsMap.keySet()) {
            boolean foundMissingField = false;
            List<Permission> entityPermissions = entityPermissionsMap.get(entityName);
            for (Permission permission : entityPermissions) {
                EntityField resource = (EntityField) permission.getResource();
                if (!resource.isEmptyField()) {
                    List<String> fields;
                    try {
                        fields = FieldUtils.getPrimitiveFieldNames(Class.forName(resourceNameExtension.getEntityResourceByResourceName(entityName).getEntityClassName()));
                        for (String fieldName : fields) {
                            if (!contains(permissions, permissionFactory.create(permission.getAction(), resource.getChild(fieldName)))) {
                                foundMissingField = true;
                                break;
                            }
                        }
                        if (!foundMissingField) {
                            revoke.addAll(entityPermissions);
                            Permission entityPermission = permissionFactory.create(permission.getAction(), resource.getParent());
                            if (!contains(permissions, entityPermission)) {
                                grant.add(entityPermission);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        }
        ret.add(grant);
        ret.add(revoke);
        return ret;
    }
}
