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
        return entityResourceFactory.createResource(subject.getClass(), EntityField.EMPTY_FIELD, ((IdHolder) subject).getId());

    }

    @Override
    public Resource createResource(Role role) {
        return entityResourceFactory.createResource(role.getClass(), EntityField.EMPTY_FIELD, ((IdHolder) role).getId());
    }

    @Override
    public Resource createResource(Action action) {
        return new EntityField(action.getClass().getSimpleName(), ((EntityAction) action).getActionName());
    }
}
