package com.blazebit.security.spi;

import com.blazebit.security.Action;


public interface ActionImplicationProvider {

    boolean isImplied(Action action, Action impliedAction);
}
