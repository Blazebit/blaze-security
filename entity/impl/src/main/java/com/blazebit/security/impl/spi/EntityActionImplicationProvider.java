package com.blazebit.security.impl.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.blazebit.security.model.Action;
import com.blazebit.security.spi.ActionFactory;
import com.blazebit.security.spi.ActionImplicationProvider;

@ApplicationScoped
public class EntityActionImplicationProvider implements ActionImplicationProvider {
    
    @Inject
    private ActionFactory actionFactory;
    
    // Immutable state
    private Action readAction;
    private Action addAction;
    private Action removeAction;
    private Action createAction;
    
    @PostConstruct
    private void init() {
        readAction = actionFactory.createAction(Action.READ);
        addAction = actionFactory.createAction(Action.ADD);
        removeAction = actionFactory.createAction(Action.REMOVE);
        createAction = actionFactory.createAction(Action.CREATE);
    }

    @Override
    public List<Action> getActionsWhichImply(Action action) {
        if (action == null) {
            return Collections.emptyList();
        }

        List<Action> result = new ArrayList<Action>();

        if (readAction.equals(action) || addAction.equals(action) || removeAction.equals(action)) {
            result.add(actionFactory.createAction(Action.UPDATE));
        }

        return result;
    }

    @Override
    public List<Action> getActionsImpledBy(Action action) {
        List<Action> ret = new ArrayList<Action>();
        if (action == null) {
            return ret;
        }

        if (createAction.equals(action)) {
            ret.add(actionFactory.createAction(Action.UPDATE));
            ret.add(actionFactory.createAction(Action.DELETE));
        }
        return ret;
    }

}
