package com.blazebit.security.metamodel;

import java.util.List;
import java.util.Map;

import com.blazebit.security.spi.EntityResource;
import com.blazebit.security.spi.ResourceDefinition;

public interface ResourceMetamodel {
	/**
	 * 
	 * @return the resources grouped by resource name
	 */
	public Map<ResourceDefinition, EntityResource> getResourceDefinitions();

	/**
	 * 
	 * @return the resources grouped by class name
	 */
	public Map<EntityResource, List<ResourceDefinition>> getEntityResources();

	/**
	 * returns the resource names grouped by module
	 * 
	 * @return
	 */
	public Map<String, List<String>> getResourcesByModule();

	/**
	 * Finds the real class name for a resource name
	 * 
	 * @param resourceName
	 * @return
	 */
	public String getEntityClassNameByResourceName(String resourceName);

	/**
	 * Returns the primitive field names of resource name's class
	 * 
	 * @param resourceName
	 * @return list of field names
	 * @throws ClassNotFoundException
	 */
	public List<String> getPrimitiveFields(String resourceName)
			throws ClassNotFoundException;

	/**
	 * Returns the collection type field names of resource name's class
	 * 
	 * @param resourceName
	 * @return list of field names
	 * @throws ClassNotFoundException
	 */
	public List<String> getCollectionFields(String resourceName)
			throws ClassNotFoundException;

	/**
	 * 
	 * @param resourceName
	 * @return
	 * @throws ClassNotFoundException
	 */
	public List<String> getFields(String resourceName)
			throws ClassNotFoundException;

	/**
	 * Returns only the primitive fields of a given class
	 * 
	 * @param entityClass
	 * @return
	 */
	public List<String> getPrimitiveFields(Class<?> entityClass);

	/**
	 * Returns only the collection type fields of a class
	 * 
	 * @param entityClass
	 * @return
	 */
	public List<String> getCollectionFields(Class<?> entityClass);

	/**
	 * Returns the field names of a given class
	 * 
	 * @param entityClass
	 * @return
	 */
	public List<String> getFields(Class<?> entityClass);

	/**
	 * Returns the module name of a resource
	 * 
	 * @param resource
	 * @return
	 */
	public String getModuleForResource(String resource);
}
