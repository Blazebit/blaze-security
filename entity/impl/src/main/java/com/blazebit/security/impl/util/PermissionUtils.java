package com.blazebit.security.impl.util;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

public final class PermissionUtils {

    public static void checkParameters(Subject subject, Action action, Resource resource) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null!");
        }
        checkParameters(action, resource);
    }

    public static void checkParameters(Action action, Resource resource) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null!");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null!");
        }
    }

    public static void checkParameters(Role role, Action action, Resource resource) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        checkParameters(action, resource);
    }

}
