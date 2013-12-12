package com.blazebit.security.auth;

import java.security.Permission;
import java.security.PermissionCollection;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContextException;

public class ShowcasePolicyConfiguration implements PolicyConfiguration {

    @Override
    public String getContextID() throws PolicyContextException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addToRole(String roleName, PermissionCollection permissions) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToRole(String roleName, Permission permission) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToUncheckedPolicy(PermissionCollection permissions) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToUncheckedPolicy(Permission permission) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToExcludedPolicy(PermissionCollection permissions) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToExcludedPolicy(Permission permission) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeRole(String roleName) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeUncheckedPolicy() throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeExcludedPolicy() throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void linkConfiguration(PolicyConfiguration link) throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete() throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public void commit() throws PolicyContextException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean inService() throws PolicyContextException {
        // TODO Auto-generated method stub
        return false;
    }

}
