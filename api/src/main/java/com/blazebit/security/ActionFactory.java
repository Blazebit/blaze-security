package com.blazebit.security;

import java.util.List;

import com.blazebit.security.constants.ActionConstants;

public interface ActionFactory {

    /**
     * 
     * @param action
     * @return action created from given constant
     */
    public <A extends Action> A createAction(ActionConstants action);

    /**
     * 
     * @return list of possible actions  for an entity
     */
    public <A extends Action> List<A> getActionsForEntity();

    /**
     * 
     * @return list of possible actions for a field of an entity
     */
    public <A extends Action> List<A> getActionsForField();

    /**
     * 
     * @return list of possible actions for a role
     */
    public <A extends Action> List<A> getActionsForRole();

    /**
     * 
     * @return list of possible actions for a subject
     */
    public <A extends Action> List<A> getActionsForSubject();

    /**
     * 
     * @return list of exceptional actions
     */
    public <A extends Action> List<A> getExceptionalActions();

}
