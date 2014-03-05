package com.blazebit.security.model;

import java.io.Serializable;


public interface IdHolder<I extends Serializable> extends Serializable {

    public I getId();
    
    public <T extends IdHolder<I>> Class<T> getRealClass();
}
