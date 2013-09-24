/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author cuszk
 */
@Stateless
public class UserGroupServiceImpl implements UserGroupService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserGroup createUserGroup(String name) {
        UserGroup ug = new UserGroup(name);
        entityManager.persist(ug);
        return ug;
    }

    @Override
    public List<UserGroup> getGroupsForUser(User user) {
        User reloadedUser = entityManager.find(User.class, user.getId());
        return new ArrayList<UserGroup>(reloadedUser.getUserGroups());
    }

    @Override
    public List<UserGroup> getAllParentGroups(/*TODO add companyId or something*/) {
        return entityManager.createQuery("select ug from " + UserGroup.class.getCanonicalName() + " ug where ug.parent is null", UserGroup.class).getResultList();
    }

    @Override
    public List<UserGroup> getGroupsForGroup(UserGroup group) {
        return entityManager.createQuery("select ug from " + UserGroup.class.getCanonicalName() + " ug where ug.parent.id  = " + group.getId(), UserGroup.class).getResultList();
    }

    @Override
    public List<User> getUsersFor(UserGroup group) {
        return entityManager.createQuery("select users from " + UserGroup.class.getCanonicalName() + " ug JOIN ug.users users where ug.id  = " + group.getId(), User.class).getResultList();
    }
}
