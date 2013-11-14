/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.service.api.UserGroupService;

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

    @Override
    public UserGroup createUserGroup(Company company, String name) {
        UserGroup ug = new UserGroup(name);
        ug.setCompany(company);
        entityManager.persist(ug);
        return ug;
    }

    @Override
    public void delete(UserGroup userGroup) {
        UserGroup reloadedUserGroup = loadUserGroup(userGroup);
        permissionManager.remove(reloadedUserGroup.getAllPermissions());
        for (UserGroup ug : reloadedUserGroup.getUserGroups()) {
            ug.setParent(null);
            entityManager.merge(ug);
        }
        entityManager.remove(reloadedUserGroup);
        entityManager.flush();
    }

    @Override
    public UserGroup loadUserGroup(UserGroup userGroup) {
        UserGroup reloadedUserGroup = entityManager.find(UserGroup.class, userGroup.getId());
        reloadedUserGroup.getUserGroups();
        reloadedUserGroup.getAllPermissions();
        return reloadedUserGroup;
    }

    @Override
    public List<UserGroup> getGroupsForUser(User user) {
        return entityManager
            .createQuery("select groups from " + User.class.getCanonicalName() + " user JOIN user.userGroups groups where user.id= :userId order by groups.name", UserGroup.class)
            .setParameter("userId", user.getId())
            .getResultList();
    }

    @Override
    public List<UserGroup> getAllParentGroups(Company company) {
        return entityManager.createQuery("select ug from " + UserGroup.class.getCanonicalName() + " ug where ug.parent is null and ug.company.id='" + company.getId()
                                             + "' order by ug.name", UserGroup.class).getResultList();
    }
    
    @Override
    public List<UserGroup> getAllGroups(Company company) {
        return entityManager.createQuery("select ug from " + UserGroup.class.getCanonicalName() + " ug where ug.company.id='" + company.getId()
                                             + "' order by ug.name", UserGroup.class).getResultList();
    }

    @Override
    public List<UserGroup> getGroupsForGroup(UserGroup group) {
        return entityManager.createQuery("select ug from " + UserGroup.class.getCanonicalName() + " ug where ug.parent.id  = " + group.getId(), UserGroup.class).getResultList();
    }

    @Override
    public List<User> getUsersFor(UserGroup group) {
        return entityManager.createQuery("select users from " + UserGroup.class.getCanonicalName() + " ug JOIN ug.users users where ug.id  = " + group.getId()
                                             + " order by users.username", User.class).getResultList();
    }

    @Override
    public UserGroup saveGroup(UserGroup ug) {
        UserGroup ret = entityManager.merge(ug);
        entityManager.flush();
        return ret;
    }
}
