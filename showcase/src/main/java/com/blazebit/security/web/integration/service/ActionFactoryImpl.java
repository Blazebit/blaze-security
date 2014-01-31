package com.blazebit.security.web.integration.service;

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.factory.ActionFactory;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.model.Action;

public class ActionFactoryImpl implements ActionFactory {

    @Override
    public Action createAction(ActionConstants action) {
        EntityAction a = new EntityAction();
        a.setActionName(action.name());
        return a;
    }

}
