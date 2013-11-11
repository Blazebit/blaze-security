package com.blazebit.security.impl.service;

import com.blazebit.security.Action;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;

public class PermissionCheckBase {

    protected void checkParameters(Subject subject, Action action, Resource resource) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null!");
        }
        checkParameters(action, resource);
    }

    protected void checkParameters(Action action, Resource resource) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null!");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null!");
        }
    }

    protected void checkParameters(Role role, Action action, Resource resource) {
        if (role == null) {
            throw new IllegalArgumentException("Subject cannot be null!");
        }
        checkParameters(action, resource);
    }

}
