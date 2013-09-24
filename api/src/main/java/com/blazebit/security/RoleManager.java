package com.blazebit.security;

import java.util.List;

public interface RoleManager {

    /**
     * 
     * @param role
     * @return subjects of a role
     */
    public List<Subject> getSubjects(Role role);

    /**
     * 
     * @param role
     * @return roles of a role
     */
    public List<Role> getRoles(Role role);
}
