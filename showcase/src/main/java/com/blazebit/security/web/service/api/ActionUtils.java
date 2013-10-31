package com.blazebit.security.web.service.api;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.constants.ActionConstants;

public class ActionUtils {

    @Inject
    private ActionFactory actionFactory;

    public List<Action> getActionsForEntity() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.CREATE));
        ret.add(actionFactory.createAction(ActionConstants.UPDATE));
        ret.add(actionFactory.createAction(ActionConstants.DELETE));
        ret.add(actionFactory.createAction(ActionConstants.READ));
        ret.add(actionFactory.createAction(ActionConstants.GRANT));
        ret.add(actionFactory.createAction(ActionConstants.REVOKE));
        return ret;
    }

    public List<Action> getActionsForField() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.UPDATE));
        ret.add(actionFactory.createAction(ActionConstants.READ));
        ret.add(actionFactory.createAction(ActionConstants.ADD));
        ret.add(actionFactory.createAction(ActionConstants.REMOVE));
        return ret;
    }

    public List<Action> getActionsForEntityObject() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.UPDATE));
        ret.add(actionFactory.createAction(ActionConstants.DELETE));
        ret.add(actionFactory.createAction(ActionConstants.READ));
        return ret;
    }

}
