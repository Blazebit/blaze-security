/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl;

import com.blazebit.lang.StringUtils;
import com.blazebit.security.Action;
import com.blazebit.security.Actor;
import com.blazebit.security.EntityObjectResource;
import com.blazebit.security.EntityResource;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.SecurityActionException;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserDataPermissionId;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupDataPermissionId;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserGroupPermissionId;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.model.UserPermissionId;
import com.blazebit.security.impl.utils.ActionUtils;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author cuszk
 */
@Stateless
public class PermissionDataAccessImpl implements PermissionDataAccess {

    @PersistenceContext
    private EntityManager entityManager;
    private static Set<Action> exceptionalActions = initExceptionalActions();
    private static final String EMPTY_FIELD = "";

    private static Set<Action> initExceptionalActions() {
        exceptionalActions = new HashSet<Action>();
        exceptionalActions.add(ActionUtils.getGrantAction());
        exceptionalActions.add(ActionUtils.getRevokeAction());
        return exceptionalActions;
    }

    private boolean isExceptionalAction(Action action) {
        for (Action _action : exceptionalActions) {
            if (_action.matches(action)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRevokable(Actor actor, Action action, Resource resource) {
        return !getRevokablePermissionsWhenRevoking(actor, action, resource).isEmpty();
    }

    @Override
    public Set<Permission> getRevokablePermissionsWhenRevoking(Actor _actor, Action _action, Resource _resource) {
        Set<Permission> ret = new HashSet<Permission>();
        //look up itself
        Permission permission = findPermission(_actor, _action, _resource);
        if (permission != null) {
            //if exact permission found -> revoke that
            ret.add(permission);
        } else {
            EntityAction action = (EntityAction) _action;
            IdHolder subject = (IdHolder) _actor;

            StringBuilder baseQuery = new StringBuilder();
            baseQuery.append("select up from ").append(" {0} up where ");
            baseQuery.append(" up.id.subject=''").append(subject.getId()).append("'' ");
            baseQuery.append(" and up.id.actionName=''").append(action.getActionName()).append("'' ");

            if (_resource instanceof EntityResource) {
                EntityResource resource = (EntityResource) _resource;
                baseQuery.append(" and up.id.entity=''").append(resource.getEntity()).append("''");

                // if resource is entity object 
                if (_resource instanceof EntityObjectResource) {
                    if (StringUtils.isEmpty(resource.getField())) {
                        //data permissions with ANY field and SPECIFIED id can be revoked
                        StringBuilder query = new StringBuilder(baseQuery);
                        query.append(" and up.id.entityId=''").append(((EntityObjectResource) resource).getEntityId()).append("''");
                        query.append(" and up.id.field is not null");
                        ret.addAll(entityManager.createQuery(MessageFormat.format(query.toString(), getDataPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
                    }
                } else {
                    //if field is not specified -> remove permissions with ANY field and data permissions with ANY field and ANY id
                    if (StringUtils.isEmpty(resource.getField())) {
                        //permissions for this entity with ANY field can be revoked
                        StringBuilder query = new StringBuilder(baseQuery);
                        query.append(" and up.id.field is not null");
                        ret.addAll(entityManager.createQuery(MessageFormat.format(query.toString(), getPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
                        //data permission with ANY id and ANY field can be revoked
                        ret.addAll(entityManager.createQuery(MessageFormat.format(baseQuery.toString(), getDataPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
                        //if field is specied -> removed data permissions with SPECIFIED field and ANY id
                    } else {
                        //data permissions with SPECIFIED field and ANY id can be revoked
                        StringBuilder query = new StringBuilder(baseQuery);
                        query.append(" and up.id.field=''").append(resource.getField()).append("''");
                        query.append(" and up.id.entityId is not null");
                        ret.addAll(entityManager.createQuery(MessageFormat.format(query.toString(), getDataPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public boolean isGrantable(Actor subject, Action action, Resource _resource) {
        //cast to entity resource to access fields
        EntityResource resource = (EntityResource) _resource;
        //check whether action is exceptional. if it is then resource cannot have a field specified
        if (isExceptionalAction(action)) {
            if (!StringUtils.isEmpty(resource.getField())) {
                System.err.println("Exceptional action");
                return false;
            }
        }
        //first lookup the exact permission. if it already exists granting is not allowed
        Permission itself = findPermission(subject, action, _resource);
        if (itself != null) {
            System.err.println("Same permission found");
            return false;
        }

        //if field is specified -> find permission for entity with no field specified (means that there exists already a permission for all entities with all fields and ids)
        // for Afi-> find A or for Af-> find A.
        if (!StringUtils.isEmpty(resource.getField())) {
            EntityField resourceWithoutField = new EntityField(resource.getEntity(), EMPTY_FIELD);
            if (findPermission(subject, action, resourceWithoutField) != null) {
                System.err.println("Permission for all fields already exists");
                return false;
            }
        }
        // if resource has id
        if (_resource instanceof EntityObjectResource) {
            EntityObjectResource objectResource = (EntityObjectResource) _resource;
            //if (!StringUtils.isEmpty(resource.getField())) {
            EntityField resourceWithField = new EntityField(resource.getEntity(), resource.getField());
            //if field and id is specified -> look for permission for entity with given field
            if (findPermission(subject, action, resourceWithField) != null) {
                System.err.println("Permission for all entities with this fields already exists");
                return false;
            }
            //if field and id is specified -> look for permission for entity with given id
            EntityObjectField resourceObjectWithoutField = new EntityObjectField(objectResource.getEntity(), EMPTY_FIELD, objectResource.getEntityId());
            if (findPermission(subject, action, resourceObjectWithoutField) != null) {
                System.err.println("Data Permission for all entities with this id for all fields already exists");
                return false;
            }
            //}
        }
        return true;
    }

    @Override
    public Permission findPermission(Actor actor, Action action, Resource resource) {
        if (actor instanceof Subject) {
            return findPermission((Subject) actor, action, resource);
        } else {
            if (actor instanceof Role) {
                return findPermission((Role) actor, action, resource);
            } else {
                throw new IllegalArgumentException("Not supported actor type");
            }
        }
    }

    private Permission findPermission(Subject subject, Action action, Resource resource) {
        if (resource instanceof EntityObjectResource) {
            return entityManager.find(UserDataPermission.class, new UserDataPermissionId((User) subject, (EntityObjectResource) resource, (EntityAction) action));
        } else {
            if (resource instanceof EntityResource) {
                return entityManager.find(UserPermission.class, new UserPermissionId((User) subject, (EntityResource) resource, (EntityAction) action));
            } else {
                throw new IllegalArgumentException("not supported resource type");
            }
        }
    }

    private Permission findPermission(Role subject, Action action, Resource resource) {
        if (resource instanceof EntityObjectField) {
            return entityManager.find(UserGroupDataPermission.class, new UserGroupDataPermissionId((UserGroup) subject, (EntityObjectField) resource, (EntityAction) action));
        } else {
            if (resource instanceof EntityField) {
                return entityManager.find(UserGroupPermission.class, new UserGroupPermissionId((UserGroup) subject, (EntityField) resource, (EntityAction) action));
            } else {
                throw new IllegalArgumentException("not supported resource type");
            }
        }
    }

    @Override
    public Set<Permission> getRevokablePermissionsWhenGranting(Actor _actor, Action _action, Resource _resource) {
        Set<Permission> ret = new HashSet<Permission>();
        EntityAction entityAction = (EntityAction) _action;
        IdHolder subject = (IdHolder) _actor;

        StringBuilder query = new StringBuilder();
        query.append("select up from ").append("{0} up where ");
        query.append(" up.id.subject=''").append(subject.getId()).append("'' ");
        query.append(" and up.id.actionName=''").append(entityAction.getActionName()).append("'' ");


        //if resource is entity object
        if (_resource instanceof EntityObjectResource) {
            EntityObjectResource resource = (EntityObjectResource) _resource;
            query.append(" and up.id.entity=''").append(resource.getEntity()).append("'' ");
            //if permission field is empty find entity objects with SPECIFIED id and ANY field
            if (StringUtils.isEmpty(resource.getField())) {
                query.append(" and up.id.entityId=''").append(resource.getEntityId()).append("'' ");
                query.append(" and up.id.field is not null ");
                ret.addAll(entityManager.createQuery(MessageFormat.format(query.toString(), getDataPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
            }
        } else {
            //if resource is entity
            EntityResource resource = (EntityResource) _resource;
            query.append("' and up.id.entity=''").append(resource.getEntity()).append("'' ");
            // if field is empty find all permissions with ANY field and dataPermissions with ANY field
            if (StringUtils.isEmpty(resource.getField())) {
                StringBuilder withField = new StringBuilder(query);
                withField.append(" and up.id.field is not null ");
                ret.addAll(entityManager.createQuery(MessageFormat.format(withField.toString(), getPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
                ret.addAll(entityManager.createQuery(MessageFormat.format(query.toString(), getDataPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
            } else {
                //if field is specified find data permissions with SPECIFIED field and ANY id
                query.append("' and up.id.field=''").append(resource.getField()).append("'' ");
                ret.addAll(entityManager.createQuery(MessageFormat.format(query.toString(), getDataPermissionTypeFor(_actor.getClass()).getCanonicalName())).getResultList());
            }

        }
        return ret;
    }

    private Class<?> getPermissionTypeFor(Class<?> clazz) {
        if (clazz.equals(User.class)) {
            return UserPermission.class;
        } else {
            if (clazz.equals(UserGroup.class)) {
                return UserGroupPermission.class;
            }
        }
        return null;
    }

    private Class<?> getDataPermissionTypeFor(Class<?> clazz) {
        if (clazz.equals(User.class)) {
            return UserDataPermission.class;
        } else {
            if (clazz.equals(UserGroup.class)) {
                return UserGroupDataPermission.class;
            }
        }
        return null;
    }
}