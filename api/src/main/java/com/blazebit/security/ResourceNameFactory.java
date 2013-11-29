package com.blazebit.security;

public interface ResourceNameFactory {

    /**
     * 
     * @param entityObject
     * @param field
     * @return
     */
    public Resource createResource(IdHolder entityObject, String field);

    public Resource createResource(IdHolder entity);

}
