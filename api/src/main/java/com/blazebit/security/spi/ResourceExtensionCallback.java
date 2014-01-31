package com.blazebit.security.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ResourceExtensionCallback {
	/**
	 * 
	 * @param collection
	 * @return
	 */
	public Map<EntityResource, List<ResourceDefinition>> handle(
			Collection<Class<?>> collection);
}
