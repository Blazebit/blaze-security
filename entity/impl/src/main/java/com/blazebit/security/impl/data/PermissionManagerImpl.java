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
package com.blazebit.security.impl.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.entity.EntityResourceMetamodel;
import com.blazebit.security.entity.Security;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.RolePermission;
import com.blazebit.security.model.Subject;
import com.blazebit.security.model.SubjectPermission;

/**
 * 
 * @author cuszk
 */
@Stateless
public class PermissionManagerImpl implements PermissionManager {

    @Inject
    @Security
    private EntityManager entityManager;

    @Inject
    private EntityResourceMetamodel resourceMetaModel;

    @Override
    public <P extends Permission> P save(P permission) {
        entityManager.persist(permission);
        return permission;
    }

    @Override
    public void remove(Permission permission) {
        entityManager.remove(entityManager.merge(permission));
    }

    @Override
    public void remove(Collection<? extends Permission> permissions) {
        for (Permission p : permissions) {
            remove(p);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Permission> getPermissions(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        return entityManager
            .createQuery("SELECT permission FROM " + SubjectPermission.class.getName()
                             + " permission WHERE permission.id.subject = :subject ORDER BY permission.id.entity, permission.id.field, permission.id.actionName")
            .setParameter("subject", subject)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getPermissionResources(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        return entityManager
            .createQuery("SELECT distinct permission.id.entity FROM " + SubjectPermission.class.getName()
                             + " permission WHERE permission.id.subject = :subject ORDER BY permission.id.entity")
            .setParameter("subject", subject)
            .getResultList();
    }

    @Override
    public Set<String> getPermissionModules(Subject subject) {
        Set<String> ret = new HashSet<String>();
        List<String> resources = getPermissionResources(subject);
        for (String resource : resources) {
            ret.add(resourceMetaModel.getModuleForResource(resource));
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Permission> getPermissions(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        return entityManager
            .createQuery("SELECT permission FROM " + RolePermission.class.getName()
                             + " permission WHERE permission.id.subject= :subject ORDER BY permission.id.entity, permission.id.field, permission.id.actionName")
            .setParameter("subject", role)
            .getResultList();
    }

    @Override
    public void removeAllPermissions(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        entityManager
            .createQuery("DELETE FROM " + SubjectPermission.class.getName() + " permission WHERE permission.id.subject=:subject")
            .setParameter("subject", subject)
            .executeUpdate();

    }

}