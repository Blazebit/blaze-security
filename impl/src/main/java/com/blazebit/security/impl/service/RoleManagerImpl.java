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
    public <R extends Role<R>, S extends Subject<?>> List<S> getSubjects(Role<R> role) {
        return entityManager.createQuery("SELECT user FROM " + User.class.getName() + " user JOIN user.userGroups groups WHERE groups.id='" + ((IdHolder) role).getId() + "'").getResultList();
    }

    @Override
    public <R extends Role<R>> List<R> getRoles(Role<R> role) {
        return entityManager.createQuery("SELECT usergroup FROM " + UserGroup.class.getName() + " usergroup WHERE usergroup.parent.id='" + ((IdHolder) role).getId() + "'").getResultList();
    }

    @Override
    public <R extends Role<R>> List<R> getRoles(Subject<R> subject) {
        return entityManager.createQuery("SELECT usergroup FROM " + UserGroup.class.getName() + " usergroup JOIN usergroup.users users WHERE users.id='" + ((IdHolder) subject).getId() + "'").getResultList();
    }

}
