package com.blazebit.security.impl.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Role;
import com.blazebit.security.RoleManager;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

public class RoleManagerImpl implements RoleManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Subject> getSubjects(Role role) {
        return entityManager.createQuery("SELECT subject FROM " + User.class.getName() + " user WHERE user.groups.id='" + ((IdHolder) role).getId() + "'").getResultList();
    }

    @Override
    public List<Role> getRoles(Role role) {
        return entityManager.createQuery("SELECT group FROM " + UserGroup.class.getName() + " group WHERE group.parent.id='" + ((IdHolder) role).getId() + "'").getResultList();
    }

}
