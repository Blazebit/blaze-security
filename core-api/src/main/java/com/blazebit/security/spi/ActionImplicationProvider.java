package com.blazebit.security.spi;

import java.util.List;

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
	public List<Action> getActionsWhichImply(Action action);

	/**
	 * 
	 * @param action
	 * @return
	 */
	public List<Action> getActionsImpledBy(Action action);

	/**
	 * Returns actions which imply the given action based on the field level
	 * option
	 * 
	 * @param action
	 * @param fieldLevelEnabled
	 * @return
	 */
	public List<Action> getActionsWhichImply(Action action,
			boolean fieldLevelEnabled);
}
