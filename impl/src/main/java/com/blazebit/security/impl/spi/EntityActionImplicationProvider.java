package com.blazebit.security.impl.spi;

import java.util.ArrayList;
import java.util.List;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.spi.ActionImplicationProvider;

@ServiceProvider(ActionImplicationProvider.class)
public class EntityActionImplicationProvider implements ActionImplicationProvider {

    @Override
    public List<Action> getActionsWhichImply(Action action) {
        List<Action> ret = new ArrayList<Action>();
        if (action == null) {
            return ret;
        }
        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);

        if (actionFactory.createAction(ActionConstants.READ).equals(action)) {
            ret.add(actionFactory.createAction(ActionConstants.UPDATE));
        }
        return ret;
    }
    
    @Override
    public List<Action> getActionsImpledBy(Action action) {
        List<Action> ret = new ArrayList<Action>();
        if (action == null) {
            return ret;
        }
        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);

        if (actionFactory.createAction(ActionConstants.CREATE).equals(action)) {
            ret.add(actionFactory.createAction(ActionConstants.UPDATE));
            ret.add(actionFactory.createAction(ActionConstants.DELETE));
        }
        return ret;
    }

}
