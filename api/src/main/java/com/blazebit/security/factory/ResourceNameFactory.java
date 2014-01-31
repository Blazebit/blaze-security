package com.blazebit.security.factory;

import com.blazebit.security.model.IdHolder;
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
	public Resource createResource(IdHolder entityObject, String field);

	/**
	 * Resource created from the given object
	 * 
	 * @param entity
	 * @return
	 */
	public Resource createResource(IdHolder entity);

}
