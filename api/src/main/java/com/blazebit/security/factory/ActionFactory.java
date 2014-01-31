package com.blazebit.security.factory;

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.model.Action;

/**
 * Produces Action objects
 * 
 * @author cuszk
 * 
 */
public interface ActionFactory {

	/**
	 * 
	 * @param action
	 * @return action created from given constant
	 */
	public Action createAction(ActionConstants action);

}
