/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.api;

import java.util.List;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

/**
 * 
 * @author cuszk
 */
public interface UserGroupService {

    public List<UserGroup> getGroupsForUser(User user);

    public List<UserGroup> getGroupsForGroup(UserGroup group);

    public List<User> getUsersFor(UserGroup group);

    public UserGroup saveGroup(UserGroup group);

    public UserGroup createUserGroup(Company company, String name);

    public List<UserGroup> getAllParentGroups(Company company);

    public void delete(UserGroup userGroup);

    public UserGroup loadUserGroup(UserGroup userGroup);

}
