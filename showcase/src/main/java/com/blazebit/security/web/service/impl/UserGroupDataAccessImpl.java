package com.blazebit.security.web.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.service.api.UserGroupDataAccess;

public class UserGroupDataAccessImpl implements UserGroupDataAccess {

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;

    @Override
    public List<User> collectUsers(Set<UserGroup> groups, boolean inherit) {
        Set<User> users = new HashSet<User>();
        if (!inherit) {
            for (UserGroup group : groups) {
                users.addAll(getUsersFor(group));
            }
        } else {
            for (UserGroup group : groups) {
                collectUsers(group, users);
            }
        }
        List<User> sortedUsers = new ArrayList<User>(users);
        Collections.sort(sortedUsers, new Comparator<User>() {

            @Override
            public int compare(User o1, User o2) {
                return o1.getUsername().compareToIgnoreCase(o2.getUsername());
            }

        });
        return sortedUsers;
    }
    

    public void collectUsers(UserGroup group, Set<User> users) {
        for (User user : getUsersFor(group)) {
            users.add(user);
        }
        for (UserGroup child : getGroupsForGroup(group)) {
            collectUsers(child, users);
        }

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
        return entityManager.createQuery("select ug from " + UserGroup.class.getCanonicalName() + " ug where ug.company.id='" + company.getId() + "' order by ug.name",
                                         UserGroup.class).getResultList();
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
    public UserGroup loadUserGroup(UserGroup userGroup) {
        UserGroup reloadedUserGroup = entityManager.find(UserGroup.class, userGroup.getId());
        reloadedUserGroup.getUserGroups();
        reloadedUserGroup.getAllPermissions();
        return reloadedUserGroup;
    }

}
