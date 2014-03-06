package com.blazebit.security.spi;

import java.util.Collection;

import com.blazebit.security.model.Action;

/**
 * Handles the implications between the predefined actions of the system
 * 
 */
public interface ActionImplicationProvider {

	/**
	 * 
	 * @param action
	 * @return
	 */
	public Collection<Action> getActionsWhichImply(Action action);

	/**
	 * 
	 * @param action
	 * @return
	 */
	public Collection<Action> getActionsImpledBy(Action action);
}
