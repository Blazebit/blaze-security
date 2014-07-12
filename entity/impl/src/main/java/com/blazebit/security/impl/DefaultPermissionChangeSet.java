package com.blazebit.security.impl;

import java.util.Set;

import com.blazebit.security.model.Permission;
import com.blazebit.security.model.PermissionChangeSet;

public final class DefaultPermissionChangeSet implements PermissionChangeSet {

    private final Set<Permission> revokes;
    private final Set<Permission> unaffected;
    private final Set<Permission> grants;

    public DefaultPermissionChangeSet(Set<Permission> revokes, Set<Permission> unaffected, Set<Permission> grants) {
        this.revokes = revokes;
        this.unaffected = unaffected;
        this.grants = grants;
    }

    @Override
    public Set<Permission> getRevokes() {
        return revokes;
    }

    @Override
    public Set<Permission> getUnaffected() {
        return unaffected;
    }

    @Override
    public Set<Permission> getGrants() {
        return grants;
    }
}
