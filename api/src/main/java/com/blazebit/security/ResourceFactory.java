package com.blazebit.security;

public interface ResourceFactory {

    /**
     * resource of a subject
     * 
     * @param subject
     * @return
     */
    public Resource createResource(Subject subject);

    /**
     * resource of a role
     * 
     * @param role
     * @return
     */
    public Resource createResource(Role role);

    /**
     * resource of an action
     * 
     * @param action
     * @return
     */
    public Resource createResource(Action action);

}
