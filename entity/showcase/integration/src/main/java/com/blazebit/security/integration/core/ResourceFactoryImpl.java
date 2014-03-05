package com.blazebit.security.integration.core;


import javax.inject.Inject;

import com.blazebit.security.entity.EntityResourceFactory;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.EntityField;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.spi.ResourceFactory;

/**
 * 
 */
public class ResourceFactoryImpl implements ResourceFactory {

    @Inject
    private EntityResourceFactory entityResourceFactory;

    @Override
    public Resource createResource(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        if (!(subject instanceof User)) {
            throw new IllegalArgumentException("Invalid subject of type: " + subject.getClass());
        }
        return entityResourceFactory.createResource(subject.getClass(), ((User) subject).getId());
    }

    @Override
    public Resource createResource(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (!(role instanceof UserGroup)) {
            throw new IllegalArgumentException("Invalid role of type: " + role.getClass());
        }
        return entityResourceFactory.createResource(role.getClass(), ((UserGroup) role).getId());
    }

    @Override
    public Resource createResource(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        return new EntityField(action.getClass().getSimpleName(), action.getName());
    }
}
