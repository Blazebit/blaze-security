/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.showcase.impl.service;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.data.PermissionHandling;
import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.model.Company;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.service.PermissionService;
import com.blazebit.security.showcase.data.GroupPermissionDataAccess;
import com.blazebit.security.showcase.data.UserGroupDataAccess;
import com.blazebit.security.showcase.service.UserGroupService;

/**
 * 
 * @author cuszk
 */
@Stateless
public class UserGroupServiceImpl implements UserGroupService {

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;
    @Inject
    private PermissionManager permissionManager;

    @Inject
    private UserGroupDataAccess userGroupDataAccess;

    @Inject
    private GroupPermissionDataAccess groupPermissionHandlingUtils;

    @Inject
    private PermissionService permissionService;

    @Inject
    private PermissionHandling permissionHandling;

    @Override
    public UserGroup create(Company company, String name) {
        UserGroup ug = new UserGroup(name);
        ug.setCompany(company);
        entityManager.persist(ug);
        return ug;
    }

    @Override
    public void delete(UserGroup userGroup) {
        UserGroup reloadedUserGroup = userGroupDataAccess.loadUserGroup(userGroup);
        permissionManager.remove(permissionManager.getPermissions(reloadedUserGroup));
        for (UserGroup ug : reloadedUserGroup.getUserGroups()) {
            ug.setParent(null);
            entityManager.merge(ug);
        }
        entityManager.remove(reloadedUserGroup);
        entityManager.flush();
    }

    @Override
    public UserGroup save(UserGroup ug) {
        UserGroup ret = entityManager.merge(ug);
        entityManager.flush();
        return ret;
    }

    @Override
    public boolean addGroupToGroup(UserGroup userGroup1, UserGroup userGroup2) {
        if (!userGroup2.getRoles().contains(userGroup1)) {
            userGroup1.setParent(userGroup2);
            entityManager.merge(userGroup1);
            entityManager.flush();
            return true;
        }
        return false;
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
    public boolean addUserToGroup(User selectedUser, UserGroup userGroup) {
        if (canUserBeAddedToRole(selectedUser, userGroup)) {
            UserGroup group = entityManager.find(UserGroup.class, userGroup.getId());
            group.getUsers().add(selectedUser);
            entityManager.merge(group);
            entityManager.flush();
            return true;
        }
        return false;
    }

    @Override
    public boolean addUserToGroup(User authorizer, User selectedUser, UserGroup userGroup, boolean propagate) {
        if (!addUserToGroup(selectedUser, userGroup)) {
            return false;
        }
        Set<UserGroup> groups = new HashSet<UserGroup>();
        groups.add(userGroup);
        Set<Permission> permissions = groupPermissionHandlingUtils.getGroupPermissions(groups, true);
        Set<Permission> grant = permissionHandling.getGrantable(authorizer, permissionManager.getPermissions(selectedUser), permissions).get(0);
        permissionService.grant(authorizer, selectedUser, grant);
        return propagate;

    }

    @Override
    public boolean removeUserFromGroup(User selectedUser, UserGroup userGroup) {
        if (canUserBeRemovedFromRole(selectedUser, userGroup)) {
            UserGroup group = entityManager.find(UserGroup.class, userGroup.getId());
            group.getUsers().remove(selectedUser);
            entityManager.merge(group);
            entityManager.flush();
            return true;
        }
        return false;

    }

}
