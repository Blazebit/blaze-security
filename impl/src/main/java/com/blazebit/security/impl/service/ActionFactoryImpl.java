package com.blazebit.security.impl.service;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;

public class ActionFactoryImpl implements ActionFactory {

    @Override
    public Action createAction(ActionConstants action) {
        EntityAction a = new EntityAction();
        a.setActionName(action.name());
        return a;
    }

}
