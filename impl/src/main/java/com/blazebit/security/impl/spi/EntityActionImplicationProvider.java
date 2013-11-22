package com.blazebit.security.impl.spi;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.spi.ActionImplicationProvider;

@ServiceProvider(ActionImplicationProvider.class)
public class EntityActionImplicationProvider implements ActionImplicationProvider {

    @Override
    public boolean isImplied(Action action, Action impliedAction) {
        if (action == null || impliedAction == null) {
            return false;
        }

        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);

        if (actionFactory.createAction(ActionConstants.UPDATE).equals(action)) {
            return actionFactory.createAction(ActionConstants.READ).equals(impliedAction);
        }

        return false;
    }

}
