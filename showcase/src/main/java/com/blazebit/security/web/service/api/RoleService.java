package com.blazebit.security.web.service.api;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;


public interface RoleService {

    public void addGroupToGroup(UserGroup userGroup1, UserGroup userGroup2);

    public void removeGroupFromGroup(UserGroup userGroup1, UserGroup userGroup2);

    public boolean canUserBeRemovedFromRole(User user, UserGroup group);

    public boolean canUserBeAddedToRole(User user, UserGroup group);

    public void addSubjectToRole(User selectedUser, UserGroup userGroup);

    public void removeSubjectFromRole(User selectedUser, UserGroup userGroup);

}
