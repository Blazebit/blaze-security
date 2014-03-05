package com.blazebit.security.integration.entity;

import java.io.Serializable;

import com.blazebit.security.entity.EntityIdAccessor;
import com.blazebit.security.model.IdHolder;


public class EntityIdAccessorImpl implements EntityIdAccessor{

    @Override
    public Serializable getId(Object entity) {
        if (entity == null) {
            throw new NullPointerException("entity");
        }
        if (!(entity instanceof IdHolder<?>)) {
            throw new IllegalArgumentException("Invalid entity given. Not a subtype of IdHolder: " + entity.getClass());
        }
        
        return ((IdHolder<?>) entity).getId();
    }


}
