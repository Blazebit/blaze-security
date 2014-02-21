package com.blazebit.security.integration.service;


import javax.enterprise.inject.Default;
import javax.inject.Inject;

import com.blazebit.security.factory.EntityResourceFactory;
import com.blazebit.security.factory.ResourceFactory;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.BaseEntity;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * 
 * @author Kinga Szabo <k.szabo@curecomp.com>
 * @company curecomp
 * @date 06.02.2014
 */
@Default
public class ResourceFactoryImpl implements ResourceFactory {

    /**
     * 
     */
    private static final long serialVersionUID = 4016482778550112196L;
    @Inject
    private EntityResourceFactory entityResourceFactory;

    @Override
    public Resource createResource(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        return entityResourceFactory.createResource(subject.getClass(), ((BaseEntity<Integer>) subject).getId());

    }

    @Override
    public Resource createResource(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        return entityResourceFactory.createResource(role.getClass(), ((BaseEntity<Integer>) role).getId());
    }

    @Override
    public Resource createResource(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        return new EntityField(action.getClass().getSimpleName(), ((EntityAction) action).getActionName());
    }
}
