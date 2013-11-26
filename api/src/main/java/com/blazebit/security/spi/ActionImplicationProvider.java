package com.blazebit.security.spi;

import java.util.List;

import com.blazebit.security.Action;

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
}
