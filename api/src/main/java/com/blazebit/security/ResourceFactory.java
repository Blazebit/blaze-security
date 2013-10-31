package com.blazebit.security;

public interface ResourceFactory {

    Resource createResource(Subject subject);

    Resource createResource(Role role);

    Resource createResource(Action action);

}
