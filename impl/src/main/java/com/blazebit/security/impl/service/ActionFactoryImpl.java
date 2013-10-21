package com.blazebit.security.impl.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;

public class ActionFactoryImpl implements ActionFactory {

    @Override
    public EntityAction createAction(ActionConstants action) {
        EntityAction a = new EntityAction();
        a.setActionName(action.name());
        return a;
    }

    @Override
    public List<EntityAction> getActionsForEntity() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.add(createAction(ActionConstants.CREATE));
        ret.add(createAction(ActionConstants.UPDATE));
        ret.add(createAction(ActionConstants.DELETE));
        ret.add(createAction(ActionConstants.READ));
        return ret;
    }

    @Override
    public List<EntityAction> getActionsForField() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.add(createAction(ActionConstants.UPDATE));
        ret.add(createAction(ActionConstants.READ));
        return ret;
    }
    
    @Override
    public List<EntityAction> getActionsForEntityObject() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.add(createAction(ActionConstants.UPDATE));
        ret.add(createAction(ActionConstants.DELETE));
        ret.add(createAction(ActionConstants.READ));
        return ret;
    }

    @Override
    public List<EntityAction> getSpecialActions() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.add(createAction(ActionConstants.GRANT));
        ret.add(createAction(ActionConstants.REVOKE));
        return ret;
    }

    @Override
    public List<EntityAction> getExceptionalActions() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.addAll(getSpecialActions());
        ret.add(createAction(ActionConstants.CREATE));
        ret.add(createAction(ActionConstants.DELETE));
        return ret;
    }

}
