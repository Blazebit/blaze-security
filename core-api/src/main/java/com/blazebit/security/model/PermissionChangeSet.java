package com.blazebit.security.model;

import java.util.Set;


public interface PermissionChangeSet {

    public Set<Permission> getRevokes();
    
    public Set<Permission> getGrants();
    
    public Set<Permission> getUnaffected();
}
