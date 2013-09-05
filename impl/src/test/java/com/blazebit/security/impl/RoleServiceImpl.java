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

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.Role;
import com.blazebit.security.RoleService;
import com.blazebit.security.SecurityActionException;
import com.blazebit.security.SecurityService;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.impl.utils.EntityUtils;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author cuszk
 */
@Stateless
public class RoleServiceImpl implements RoleService {

    @Inject
    private SecurityService securityService;
    @Inject
    private PermissionDataAccess permissionDataAccess;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void addRoleToRole(Role role1, Role role2) {
        role1.setParent(role2);
        entityManager.merge(role1);
    }

    @Override
    public void removeRoleFromRole(Role role1, Role role2) {
        role1.setParent(null);
        entityManager.merge(role1);
    }

    @Override
    public void addSubjectToRole(Subject authorizer, Subject subject, Role role, boolean copyPermissions) {
        role = entityManager.find(role.getClass(), ((IdHolder) role).getId());
        if (!securityService.isGranted(authorizer, ActionUtils.getGrantAction(), EntityUtils.getEntityObjectFieldFor(subject))) {
            throw new SecurityException();
        }
        if (!securityService.isGranted(authorizer, ActionUtils.getGrantAction(), EntityUtils.getEntityObjectFieldFor(role))) {
            throw new SecurityException();
        }
        if (!canSubjectBeAddedToRole(subject, role)) {
            throw new SecurityActionException();
        }
        if (copyPermissions) {
            //copy given role's permissions
            for (Object _permission : role.getAllPermissions()) {
                //TODO why needed to cast?
                Permission permission = (Permission) _permission;

                if (permissionDataAccess.isGrantable(subject, permission.getAction(), permission.getResource())) {
                    securityService.grant(authorizer, subject, permission.getAction(), permission.getResource());
                }
            }

            Role current = role.getParent();
            while (current != null) {
                for (Object _permission : current.getAllPermissions()) {
                    //TODO why needed to cast?
                    Permission permission = (Permission) _permission;
                    if (permissionDataAccess.isGrantable(subject, permission.getAction(), permission.getResource())) {
                        securityService.grant(authorizer, subject, permission.getAction(), permission.getResource());
                    }
                }
                current = current.getParent();
            }
        }
        //refresh subject before merging because permissions have been addded?
        subject = entityManager.find(subject.getClass(), ((IdHolder) subject).getId());
        subject.getRoles().add(role);
        entityManager.merge(subject);
        entityManager.flush();
    }

    @Override
    public void removeSubjectFromRole(Subject authorizer, Subject subject, Role role) {
        subject.getRoles().remove(role);
        //entityManager.createNativeQuery("delete from User_Role where users_id=" + user.getId() + " and usergroups_id=" + group.getId()).executeUpdate();
        entityManager.merge(subject);
    }

    @Override
    public boolean canSubjectBeAddedToRole(Subject subject, Role role) {

        role = entityManager.find(role.getClass(), ((IdHolder) role).getId());
        subject = entityManager.find(subject.getClass(), ((IdHolder) subject).getId());

        for (Object _currentRole : subject.getRoles()) {
            Role currentRole = (Role) _currentRole;
            Role parent = currentRole.getParent();
            while (parent != null) {
                if (parent.equals(role)) {
                    return false;
                }
                parent = parent.getParent();
            }

        }
        return true;
    }
}
