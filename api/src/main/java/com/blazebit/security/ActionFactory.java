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
     * @return list of possible actions for an entity
     */
    public <A extends Action> List<A> getActionsForEntity();

    /**
     * 
     * @return list of possible actions for a field of an entity
     */
    public <A extends Action> List<A> getActionsForField();

    /**
     * 
     * @return list of exceptional actions
     */
    public <A extends Action> List<A> getExceptionalActions();

    /**
     * 
     * @return list of special actions
     */
    public <A extends Action> List<A> getSpecialActions();

    /**
     * 
     * @return list of possible action for an object
     */
    public <A extends Action> List<A> getActionsForEntityObject();

}
