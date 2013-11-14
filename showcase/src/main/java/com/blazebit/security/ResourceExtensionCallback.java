package com.blazebit.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.blazebit.security.web.bean.ResourceNameExtension.EntityResource;
import com.blazebit.security.web.bean.ResourceNameExtension.ResourceDefinition;

public interface ResourceExtensionCallback {

    public Map<EntityResource, List<ResourceDefinition>> handle(Collection<Class<?>> collection);
}
