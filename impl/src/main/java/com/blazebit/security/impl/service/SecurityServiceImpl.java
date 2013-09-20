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
package com.blazebit.security.impl.service;

import com.blazebit.security.Action;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Module;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionActionException;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.SecurityService;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityConstants;
import com.blazebit.security.impl.model.SubjectRoleConstants;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.impl.utils.EntityUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author cuszk
 */
@Stateless
public class SecurityServiceImpl implements SecurityService {

    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private PermissionDataAccess permissionDataAccess;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public <R extends Role<R>> boolean isGranted(Subject<R> subject, Action action, Resource resource) {
        subject = entityManager.find(subject.getClass(), ((IdHolder) subject).getId());
        for (Permission permission : subject.getAllPermissions()) {
            if (permission.getAction().matches(action) && permission.getResource().matches(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <R extends Role<R>> void grant(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource) throws PermissionException, PermissionActionException {
//        if (authorizer.equals(subject)) {
//            throw new SecurityException("Authorizer and subject are the same");
//        }
        subject = entityManager.find(subject.getClass(), ((IdHolder) subject).getId());
        if (!isGranted(authorizer, getGrantAction(), EntityUtils.getEntityObjectFieldFor(subject.getClass(), "", ((IdHolder) subject).getEntityId()))) {
            throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!permissionDataAccess.isGrantable(subject, action, resource)) {
            throw new PermissionActionException("Permission for " + subject + ", " + action + "," + resource + " cannot be granted");
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenGranting(subject, action, resource);
        for (Permission existingPermission : removablePermissions) {
            entityManager.remove(existingPermission);
        }
        Permission permission = permissionFactory.create(subject, action, resource);
        entityManager.persist(permission);
        entityManager.flush();
    }

    @Override
    public <R extends Role<R>> void revoke(Subject<R> authorizer, Subject<R> subject, Action action, Resource resource) throws PermissionException, PermissionActionException {
//        if (authorizer.equals(subject)) {
//            throw new SecurityException("Authorizer and subject are the same");
//        }
        subject = entityManager.find(subject.getClass(), ((IdHolder) subject).getId());
        if (!isGranted(authorizer, getRevokeAction(), EntityUtils.getEntityObjectFieldFor(subject.getClass(), "", ((IdHolder) subject).getEntityId()))) {
            throw new PermissionException(authorizer + " is not allowed to " + action + " to " + resource);
        }
        if (!permissionDataAccess.isRevokable(subject, action, resource)) {
            throw new PermissionActionException("Permission : " + subject + ", " + action + ", "
                    + resource + " cannot be revoked");
        }
        Set<Permission> removablePermissions = permissionDataAccess.getRevokablePermissionsWhenRevoking(subject, action, resource);
        for (Permission permission : removablePermissions) {
            entityManager.remove(permission);
        }
        entityManager.flush();
    }

    @Override
    public <R extends Role<R>> Collection<Action> getAllowedActions(Subject<R> subject, Resource resource) {
        Set<Action> actions = new HashSet<Action>();
        subject = entityManager.find(subject.getClass(), ((IdHolder) subject).getId());
        for (Permission permission : subject.getAllPermissions()) {
            if (permission.getResource().matches(resource)) {
                actions.add(permission.getAction());
            }
        }
        return actions;
    }

    @Override
    public Action getGrantAction() {
        return ActionUtils.getAction(ActionUtils.ActionConstants.GRANT);
    }

    @Override
    public Action getRevokeAction() {
        return ActionUtils.getAction(ActionUtils.ActionConstants.REVOKE);
    }

    @Override
    public Collection<Resource> getAllResources() {
        Set<Resource> ret = new HashSet<Resource>();
        for (EntityConstants entity : EntityConstants.values()) {
            ret.add(EntityUtils.getEntityFieldFor(entity, ""));
        }
        for (SubjectRoleConstants entity : SubjectRoleConstants.values()) {
            ret.add(EntityUtils.getEntityFieldFor(entity, ""));
        }
        return ret;
    }

    /**
     *
     * @param module
     * @return
     */
    @Override
    public Collection<Resource> getAllResources(Module module) {
        Set<Resource> ret = new HashSet<Resource>();
        for (Enum entity : module.getEntities()) {
            ret.add(EntityUtils.getEntityFieldFor((EntityConstants) entity, ""));
        }
        for (SubjectRoleConstants entity : SubjectRoleConstants.values()) {
            ret.add(EntityUtils.getEntityFieldFor(entity, ""));
        }
        return ret;

    }
}
