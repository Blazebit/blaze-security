package com.blazebit.security.model;

import java.io.Serializable;


public interface IdHolder<I extends Serializable> extends Serializable {

    public I getId();
    
}
