package com.blazebit.security.entity;

import java.io.Serializable;


public interface EntityIdAccessor {

    public Serializable getId(Object entity);
}
