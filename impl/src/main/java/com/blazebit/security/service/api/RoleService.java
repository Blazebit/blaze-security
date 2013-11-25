package com.blazebit.security.service.api;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

public interface RoleService {

    /**
     * adds userGroup1 to userGroup2
     * 
     * @param userGroup1
     * @param userGroup2
     */
    public void addGroupToGroup(UserGroup userGroup1, UserGroup userGroup2);

    /**
     * adds user to group
     * 
     * @param selectedUser
     * @param userGroup
     */
    public void addSubjectToRole(User selectedUser, UserGroup userGroup);

    /**
     * removes user from group
     * 
     * @param selectedUser
     * @param userGroup
     */
    public void removeSubjectFromRole(User selectedUser, UserGroup userGroup);

}
