package com.blazebit.security.showcase.impl.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.model.Company;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.showcase.data.UserGroupDataAccess;

public class UserGroupDataAccessImpl implements UserGroupDataAccess {

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;

    @Override
    public List<User> collectUsers(UserGroup group, boolean inherit) {
        Set<UserGroup> groups = new HashSet<UserGroup>();
        groups.add(group);
        return collectUsers(groups, inherit);
    }

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
    public Set<UserGroup> collectGroups(User user, boolean inherit) {
        Set<UserGroup> ret = new HashSet<UserGroup>();
        List<UserGroup> groupsForUser = getGroupsForUser(user);
        for (UserGroup group : groupsForUser) {
            ret.add(group);
            
            if (inherit) {
                UserGroup parent = group.getParent();
                while (parent != null) {
                    ret.add(parent);
                    parent = parent.getParent();
                }
            }
        }
        return ret;
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
        return reloadedUserGroup;
    }

    @Override
    public List<Set<UserGroup>> getAddedAndRemovedUserGroups(User user, Set<UserGroup> selectedGroups) {
        List<Set<UserGroup>> ret = new ArrayList<Set<UserGroup>>();
        Set<UserGroup> added = new HashSet<UserGroup>();
        Set<UserGroup> removed = new HashSet<UserGroup>();
        List<UserGroup> groupsForUser = getGroupsForUser(user);
        // List<UserGroup> groupsForUser = userGroupService.getGroupsForUser(user);
        for (UserGroup group : selectedGroups) {

            if (!groupsForUser.contains(group)) {
                added.add(group);
            }
        }

        for (UserGroup group : groupsForUser) {
            if (!selectedGroups.contains(group)) {
                removed.add(group);
            }
        }
        ret.add(added);
        ret.add(removed);
        return ret;
    }

}
