package com.blazebit.security.showcase.impl.data;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.model.User;
import com.blazebit.security.showcase.data.UserDataAccess;

public class UserDataAccessImpl implements UserDataAccess {

    private static final long serialVersionUID = 1L;
    
    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;

    @Override
    public User findUser(Integer id) {
        return entityManager.find(User.class, id);
    }

    @Override
    public User findUser(String name) {
        return entityManager.createQuery("Select u from " + User.class.getName() + " u where u.username='" + name + "'", User.class).getSingleResult();
    }

}
