/*
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
package com.blazebit.security.web.service.impl;

import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.PermissionActionException;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.service.api.RoleService;

/**
 * 
 * @author cuszk
 */
@Stateless
public class RoleServiceImpl implements RoleService {

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;

    @Override
    public void addGroupToGroup(UserGroup userGroup1, UserGroup userGroup2) {
        if (!userGroup2.getRoles().contains(userGroup1)) {
            userGroup1.setParent(userGroup2);
            entityManager.merge(userGroup1);
            entityManager.flush();
        } else {
            throw new PermissionActionException("UserGroup " + userGroup1 + " cannot be added to UserGroup " + userGroup2 + " because it is already added");
        }
    }

    private boolean canUserBeAddedToRole(User user, UserGroup group) {
        User reloadedUser = entityManager.find(User.class, user.getId());
        Set<UserGroup> groups = reloadedUser.getUserGroups();
        for (UserGroup currentGroup : groups) {
            // subject cannot be added to the same role where he already belongs
            if (currentGroup.equals(group)) {
                return false;
            }
            // // subject cannot be added to the parent roles of the roles where he belongs
            // UserGroup parent = currentGroup.getParent();
            // while (parent != null) {
            // if (parent.equals(group)) {
            // return false;
            // }
            // parent = parent.getParent();
            // }

        }
        return true;
    }

    private boolean canUserBeRemovedFromRole(User user, UserGroup group) {
        // subject can be removed from role if role contains subject
        UserGroup reloadedgroup = entityManager.find(UserGroup.class, group.getId());
        return reloadedgroup.getSubjects().contains(user);
    }

    @Override
    public void addSubjectToRole(User selectedUser, UserGroup userGroup) {
        if (canUserBeAddedToRole(selectedUser, userGroup)) {
            UserGroup group = entityManager.find(UserGroup.class, userGroup.getId());
            group.getUsers().add(selectedUser);
            entityManager.merge(group);
            entityManager.flush();
        }

    }

    @Override
    public void removeSubjectFromRole(User selectedUser, UserGroup userGroup) {
        if (canUserBeRemovedFromRole(selectedUser, userGroup)) {
            UserGroup group = entityManager.find(UserGroup.class, userGroup.getId());
            group.getUsers().remove(selectedUser);
            entityManager.merge(group);
            entityManager.flush();
        }

    }

}
