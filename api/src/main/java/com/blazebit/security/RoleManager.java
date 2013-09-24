package com.blazebit.security;

import java.util.List;

public interface RoleManager {

    /**
     * 
     * @param role
     * @return subjects of a role
     */
    public <R extends Role<R>, S extends Subject<?>> List<S> getSubjects(Role<R> role);

    /**
     * 
     * @param role
     * @return roles of a role
     */
    public <R extends Role<R>> List<R> getRoles(Role<R> role);

    /**
     * 
     * @param subject
     * @return
     */
    public <R extends Role<R>> List<R> getRoles(Subject<R> subject);
}
