/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.service.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.service.api.RoleManager;

/**
 * 
 * @author cuszk
 */
@Stateless
public class RoleManagerImpl implements RoleManager {

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;

    @Override
    public List<Role> getSubjectRoles(Subject subject) {
        // TODO check why this isnt working with user.roles
        return entityManager
            .createQuery("select roles from " + Subject.class.getCanonicalName() + " user JOIN user.userGroups roles where user= :user order by roles.name", Role.class)
            .setParameter("user", subject)
            .getResultList();
    }

    @Override
    public List<Role> getRoleRoles(Role role) {
        return entityManager
            .createQuery("select role from " + Role.class.getCanonicalName() + " role where role.parent  = :role", Role.class)
            .setParameter("role", role)
            .getResultList();
    }

}
