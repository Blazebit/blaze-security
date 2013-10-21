/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

import java.util.List;

/**
 * 
 * @author cuszk
 */
public interface UserGroupService {

    public List<UserGroup> getGroupsForUser(User user);

    public List<UserGroup> getGroupsForGroup(UserGroup group);

    public List<User> getUsersFor(UserGroup group);

    public UserGroup saveGroup(UserGroup group);

    UserGroup createUserGroup(Company company, String name);

    List<UserGroup> getAllParentGroups(Company company);

}
