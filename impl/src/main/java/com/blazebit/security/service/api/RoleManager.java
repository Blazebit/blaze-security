package com.blazebit.security.service.api;

import java.util.List;

import com.blazebit.security.Role;
import com.blazebit.security.Subject;


public interface RoleManager {

    List<? extends Role> getSubjectRoles(Subject subject);

    List<? extends Role> getRoleRoles(Role role);

}
