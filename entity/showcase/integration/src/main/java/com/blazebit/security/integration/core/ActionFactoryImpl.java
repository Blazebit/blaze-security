package com.blazebit.security.integration.core;

import com.blazebit.security.model.Action;
import com.blazebit.security.model.EntityAction;
import com.blazebit.security.spi.ActionFactory;

public class ActionFactoryImpl implements ActionFactory {

    @Override
    public Action createAction(String action) {
        return new EntityAction(action);
    }

}
