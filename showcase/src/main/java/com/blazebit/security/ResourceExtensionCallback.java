package com.blazebit.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ResourceExtensionCallback {

    public Map<EntityResource, List<ResourceDefinition>> handle(Collection<Class<?>> collection);
}
