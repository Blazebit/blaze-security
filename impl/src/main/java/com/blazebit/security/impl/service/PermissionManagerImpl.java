/*
 * 
 * Copyright 2013 Blazebit.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.blazebit.security.impl.service;

import java.util.Collection;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserPermission;

/**
 * 
 * @author cuszk
 */
@Stateless
public class PermissionManagerImpl implements PermissionManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public <P extends Permission> P save(P permission) {
        entityManager.persist(permission);
        return permission;
    }

    @Override
    public void flush() {
        entityManager.flush();

    }

    @SuppressWarnings("unchecked")
    @Override
    public <P extends Permission> List<P> getAllPermissions(Subject<?> subject) {
        return (List<P>) entityManager
            .createQuery("SELECT permission FROM " + Permission.class.getName() + " permission WHERE permission.id.subject.id='" + ((IdHolder) subject).getId()
                             + "' ORDER BY permission.id.entity, permission.id.field, permission.id.actionName")
            .getResultList();

    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <P extends Permission> List<P> getPermissions(Subject<?> subject) {
        return (List<P>) entityManager
            .createQuery("SELECT permission FROM " + UserPermission.class.getName() + " permission WHERE permission.id.subject.id='" + ((IdHolder) subject).getId()
                             + "' ORDER BY permission.id.entity, permission.id.field, permission.id.actionName")
            .getResultList();

    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <P extends Permission> List<P> getDataPermissions(Subject<?> subject) {
        return (List<P>) entityManager
            .createQuery("SELECT permission FROM " + UserDataPermission.class.getName() + " permission WHERE permission.id.subject.id='" + ((IdHolder) subject).getId()
                             + "' ORDER BY permission.id.entity, permission.id.field, permission.id.actionName")
            .getResultList();

    }

    @Override
    public void removeAllPermissions(Subject<?> subject) {
        entityManager
            .createQuery("DELETE FROM " + UserPermission.class.getName() + " permission WHERE permission.id.subject.id='" + ((IdHolder) subject).getId() + "'")
            .executeUpdate();

    }

    @SuppressWarnings("unchecked")
    @Override
    public <P extends Permission> List<P> getAllPermissions(Role<?> role) {
        return (List<P>) entityManager
            .createQuery("SELECT permission FROM " + Permission.class.getName() + " permission WHERE permission.id.subject.id='" + ((IdHolder) role).getId()
                             + "'  ORDER BY permission.id.entity, permission.id.field, permission.id.actionName")
            .getResultList();
    }

    @Override
    public <P extends Permission> void remove(P permission) {
        entityManager.remove(permission);
        entityManager.flush();

    }

    @Override
    public <P extends Permission> void remove(Collection<P> permissions) {
        for (Permission p : permissions) {
            remove(p);
        }
    }

    // TODO alternative to fetch all the permissions. problem with getAllPermissions method that it invokes a flush before the
    // query. When this is invoked from the flush interceptor it causes to invoke the flush interceptor again and again.
    // See forum: https://hibernate.atlassian.net/browse/HB-1480
    @Override
    public Subject reloadSubjectWithPermissions(Subject subject) {
        User user = entityManager.find(User.class, ((IdHolder) subject).getId());
        user.getAllPermissions();
        return user;
    }

    @Override
    public Role reloadSubjectWithPermissions(Role role) {
        UserGroup group = entityManager.find(UserGroup.class, ((IdHolder) role).getId());
        group.getAllPermissions();
        return group;
    }

}