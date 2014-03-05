package com.blazebit.security.entity;

import java.io.Serializable;


public interface EntityIdConverter {

    public String getIdAsString(Serializable id);
}
