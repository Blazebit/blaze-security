/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.service.api.RoleService;

/**
 * 
 * @author cuszk
 */
@Stateless
public class UserServiceImpl implements UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private PermissionManager permissionManager;

    @Inject
    private RoleService roleService;

    @Override
    public User createUser(Company company, String name) {
        User user = new User(name);
        user.setCompany(company);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    @Override
    public void delete(User user) {
        User reloadedUser = loadUser(user);
        for (UserGroup userGroup : reloadedUser.getUserGroups()) {
            roleService.removeSubjectFromRole(reloadedUser, userGroup);
        }
        permissionManager.remove(reloadedUser.getAllPermissions());
        entityManager.remove(reloadedUser);
        entityManager.flush();
    }
    
  
    @Override
    public List<User> findUsers(Company company) {
        return entityManager
            .createQuery("select user from " + User.class.getCanonicalName() + " user where user.company.id='" + company.getId() + "' order by user.id", User.class)
            .getResultList();
    }

    @Override
    public User loadUser(User user) {
        User reloadedUser = entityManager.find(User.class, user.getId());
        reloadedUser.getUserGroups();
        reloadedUser.getAllPermissions();
        return reloadedUser;
    }

    @Override
    public User findUser(String username, Company company) {
        try {
            return entityManager.createQuery("select u from " + User.class.getCanonicalName() + " u where u.username='" + username + "' and u.company.id=" + company.getId(),
                                             User.class).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

}
