package com.blazebit.security.integration.service;

import java.util.List;
import java.util.Set;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

public interface UserGroupDataAccess {

    /**
     * 
     * @param user
     * @return
     */
    public List<UserGroup> getGroupsForUser(User user);

    /**
     * 
     * @param company
     * @return
     */
    public List<UserGroup> getAllParentGroups(Company company);

    /**
     * 
     * @param group
     * @return
     */
    public List<User> getUsersFor(UserGroup group);

    /**
     * 
     * @param group
     * @return
     */
    public List<UserGroup> getGroupsForGroup(UserGroup group);

    /**
     * 
     * @param company
     * @return
     */
    public List<UserGroup> getAllGroups(Company company);

    /**
     * 
     * @param userGroup
     * @param inherit
     * @return users of groups
     */
    public List<User> collectUsers(UserGroup userGroup, boolean inherit);

    /**
     * 
     * @param user
     * @return
     */
    public Set<UserGroup> collectGroups(User user, boolean ingerit);

    /**
     * 
     * @param groups
     * @param inherit
     * @return
     */
    public List<User> collectUsers(Set<UserGroup> groups, boolean inherit);

    /**
     * 
     * @param userGroup
     * @return
     */
    public UserGroup loadUserGroup(UserGroup userGroup);

    List<Set<UserGroup>> getAddedAndRemovedUserGroups(User user, Set<UserGroup> selectedGroups);

}
