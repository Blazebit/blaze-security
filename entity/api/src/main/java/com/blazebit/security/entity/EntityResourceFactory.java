package com.blazebit.security.entity;

import java.io.Serializable;

import com.blazebit.security.model.Resource;

/**
 * Produces Resource objects
 * 
 * @author cuszk
 * 
 */
public interface EntityResourceFactory {

	/**
	 * 
	 * @param clazz
	 * @return resource created for the given class
	 */
	public Resource createResource(Class<?> clazz);

	/**
	 * 
	 * @param clazz
	 * @param field
	 * @return resource created for the given class with the given field
	 */
	public Resource createResource(Class<?> clazz, String field);

	/**
	 * 
	 * @param clazz
	 * @param id
	 * @return resource created for the given class with the given id
	 */
	public Resource createResource(Class<?> clazz, Serializable id);

	/**
	 * 
	 * @param clazz
	 * @param field
	 * @param id
	 * @return resource created for the given class with the given field and id
	 */
	public Resource createResource(Class<?> clazz, String field, Serializable id);

	/**
	 * 
	 * @param entity
	 * @return resource created from the given entity name
	 */
	public Resource createResource(String entity);

	/**
	 * 
	 * @param entity
	 * @param field
	 * @return resource created from the given entity name and field
	 */
	public Resource createResource(String entity, String field);

	/**
	 * 
	 * @param entity
	 * @param id
	 * @return resource created from the given entity name and id
	 */
	public Resource createResource(String entity, Serializable id);

	/**
	 * 
	 * @param entity
	 * @param field
	 * @param id
	 * @return resource from the given entity name field and id
	 */
	public Resource createResource(String entity, String field, Serializable id);

	/**
	 * 
	 * @param entityObject
	 * @return resource of an instance of an entity
	 */
	public Resource createResource(Object entityObject);

    public Resource createResource(Object entityObject, boolean withId);

	/**
	 * 
	 * @param entityObject
	 * @param field
	 * @return resource of an instance of an entity with a field
	 */
	public Resource createResource(Object entityObject, String field);

    public Resource createResource(Object entityObject, String field, boolean withId);

}
