package com.blazebit.security.factory;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.Resource;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * Produces resources out of permissions specific entities.
 * 
 * @author cuszk
 * 
 */
public interface ResourceFactory {

	/**
	 * resource of a subject
	 * 
	 * @param subject
	 * @return
	 */
	public Resource createResource(Subject subject);

	/**
	 * resource of a role
	 * 
	 * @param role
	 * @return
	 */
	public Resource createResource(Role role);

	/**
	 * resource of an action
	 * 
	 * @param action
	 * @return
	 */
	public Resource createResource(Action action);

}
