package com.blazebit.security.model;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collection;

/**
 * 
 * @author cuszk
 * 
 */
public interface Module extends Principal, Group {

	/**
	 * 
	 * @return subjects of a role
	 */
	public Collection<Principal> getMembers();

}
