package com.blazebit.security;


public interface ResourceNameFactory {

    Resource createResource(IdHolder entityObject, String field);

    Resource createResource(IdHolder entityObject);

}
