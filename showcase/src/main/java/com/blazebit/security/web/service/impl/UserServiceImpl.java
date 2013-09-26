/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import com.blazebit.security.impl.model.User;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * 
 * @author cuszk
 */
@Stateless
public class UserServiceImpl implements UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public User createUser(String name) {
        User user = new User(name);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    @Override
    public void delete(User user) {
        entityManager.remove(user);
        entityManager.flush();
    }

    @Override
    public List<User> findUsers() {
        return entityManager.createQuery("select u from " + User.class.getCanonicalName() + " u order by u.id", User.class).getResultList();
    }

    @Override
    public User loadUser(User user) {
        User reloadedUser = entityManager.find(User.class, user.getId());
        reloadedUser.getUserGroups();
        reloadedUser.getAllPermissions();
        return reloadedUser;
    }
}
