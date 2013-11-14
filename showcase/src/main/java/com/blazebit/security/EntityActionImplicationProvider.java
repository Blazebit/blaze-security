package com.blazebit.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.constants.ActionConstants;

@ServiceProvider(ActionImplicationProvider.class)
public class EntityActionImplicationProvider implements ActionImplicationProvider {

    @Override
    public Map<Action, List<Action>> getActionImplications() {
        Map<Action, List<Action>> ret = new HashMap<Action, List<Action>>();
        List<Action> impliedActions = new ArrayList<Action>();
        ActionFactory actionFactory = BeanProvider.getContextualReference(ActionFactory.class);
        impliedActions.add(actionFactory.createAction(ActionConstants.READ));
        ret.put(actionFactory.createAction(ActionConstants.UPDATE), impliedActions);
        return ret;
    }
}
