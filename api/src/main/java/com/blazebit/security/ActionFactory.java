package com.blazebit.security;

import com.blazebit.security.constants.ActionConstants;

public interface ActionFactory {

    /**
     * 
     * @param action
     * @return action created from given constant
     */
    public Action createAction(ActionConstants action);

}
