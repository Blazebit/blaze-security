package com.blazebit.security.spi;

import com.blazebit.security.model.Action;

/**
 * A factory for creating action objects by name.
 * 
 */
public interface ActionFactory {

	/**
	 * Creates an action object for the given action parameter.
	 * 
	 * @param action The name of the action to create
	 * @return The action object for the given action
	 * @see Action
	 */
	public Action createAction(String action);

}
