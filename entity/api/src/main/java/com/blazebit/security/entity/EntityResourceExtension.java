package com.blazebit.security.entity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementations are invoked at application startup and can provide additional resource definitions based on the found entity classes.
 * 
 */
public interface EntityResourceExtension {
	/**
	 * 
	 * @param collection
	 * @return
	 */
	public Map<String, List<EntityResourceDefinition>> handle(Collection<Class<?>> collection);
}
