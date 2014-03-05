package com.blazebit.security.impl.spi;

import java.util.ArrayList;
import java.util.List;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.model.Action;
import com.blazebit.security.spi.ActionFactory;
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

        if (actionFactory.createAction(Action.READ).equals(action)) {
            ret.add(actionFactory.createAction(Action.UPDATE));
        }
        return ret;
    }

    @Override
    public List<Action> getActionsWhichImply(Action action, boolean fieldLevelEnabled) {
        if (fieldLevelEnabled) {
            return getActionsWhichImply(action);
        }
        List<Action> ret = new ArrayList<Action>();
        ret.addAll(getActionsWhichImply(action));

        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);

        if (actionFactory.createAction(Action.ADD).equals(action)) {
            ret.add(actionFactory.createAction(Action.UPDATE));
        }
        if (actionFactory.createAction(Action.REMOVE).equals(action)) {
            ret.add(actionFactory.createAction(Action.UPDATE));
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

        if (actionFactory.createAction(Action.CREATE).equals(action)) {
            ret.add(actionFactory.createAction(Action.UPDATE));
            ret.add(actionFactory.createAction(Action.DELETE));
        }
        return ret;
    }

}
