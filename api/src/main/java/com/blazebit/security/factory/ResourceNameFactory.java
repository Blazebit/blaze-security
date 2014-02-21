package com.blazebit.security.factory;

import java.io.Serializable;

import com.blazebit.security.model.BaseEntity;
import com.blazebit.security.model.Resource;

/**
 * Produces resources out of any given object
 * 
 * @author cuszk
 * 
 */
public interface ResourceNameFactory {

	/**
	 * Resource created from a given object for a given field
	 * 
	 * @param entityObject
	 * @param field
	 * @return
	 */
	public Resource createResource(BaseEntity<Serializable> entityObject, String field);

	/**
	 * Resource created from the given object
	 * 
	 * @param entity
	 * @return
	 */
	public Resource createResource(BaseEntity<Serializable> entity);

}
