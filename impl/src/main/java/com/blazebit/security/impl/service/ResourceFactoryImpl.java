package com.blazebit.security.impl.service;

import javax.inject.Inject;

import com.blazebit.security.Action;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.IdHolder;
import com.blazebit.security.Resource;
import com.blazebit.security.ResourceFactory;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;

public class ResourceFactoryImpl implements ResourceFactory {

    @Inject
    private EntityResourceFactory entityResourceFactory;

    @Override
    public Resource createResource(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        return entityResourceFactory.createResource(subject.getClass(), ((IdHolder) subject).getId());

    }

    @Override
    public Resource createResource(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        return entityResourceFactory.createResource(role.getClass(), ((IdHolder) role).getId());
    }

    @Override
    public Resource createResource(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        return new EntityField(action.getClass().getSimpleName(), ((EntityAction) action).getActionName());
    }
}
