/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.showcase.service;

import com.blazebit.security.model.Company;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;

/**
 * 
 * @author cuszk
 */
public interface UserGroupService {

    /**
     * 
     * @param group
     * @return
     */
    public UserGroup save(UserGroup group);

    /**
     * 
     * @param company
     * @param name
     * @return
     */
    public UserGroup create(Company company, String name);

    /**
     * 
     * @param userGroup
     */
    public void delete(UserGroup userGroup);

    public boolean addGroupToGroup(UserGroup userGroup1, UserGroup userGroup2);

    public boolean addUserToGroup(User selectedUser, UserGroup userGroup);

    public boolean removeUserFromGroup(User selectedUser, UserGroup userGroup);

    boolean addUserToGroup(User authorizer, User selectedUser, UserGroup userGroup, boolean propagate);

}
