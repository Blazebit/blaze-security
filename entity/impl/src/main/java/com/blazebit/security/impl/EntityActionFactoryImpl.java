package com.blazebit.security.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import com.blazebit.security.entity.EntityActionFactory;
import com.blazebit.security.model.Action;
import com.blazebit.security.spi.ActionFactory;

public class EntityActionFactoryImpl implements EntityActionFactory {

    @Inject
    private ActionFactory actionFactory;

    /* (non-Javadoc)
     * @see com.blazebit.security.impl.EntityActionFactory#getActionsForEntity()
     */
    @Override
    public List<Action> getActionsForEntity() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(Action.DELETE));
        ret.add(actionFactory.createAction(Action.GRANT));
        ret.add(actionFactory.createAction(Action.REVOKE));
        return ret;
    }

    /* (non-Javadoc)
     * @see com.blazebit.security.impl.EntityActionFactory#getCommonActionsForEntityAndFields()
     */
    @Override
    public List<Action> getCommonActionsForEntityAndFields() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(Action.CREATE));
        ret.add(actionFactory.createAction(Action.READ));
        return ret;
    }

    /* (non-Javadoc)
     * @see com.blazebit.security.impl.EntityActionFactory#getUpdateActionsForCollectionField()
     */
    @Override
    public List<Action> getUpdateActionsForCollectionField() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(Action.ADD));
        ret.add(actionFactory.createAction(Action.REMOVE));
        return ret;
    }

    /* (non-Javadoc)
     * @see com.blazebit.security.impl.EntityActionFactory#getActionsForEntityObject()
     */
    @Override
    public List<Action> getActionsForEntityObject() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(Action.UPDATE));
        ret.add(actionFactory.createAction(Action.DELETE));
        ret.add(actionFactory.createAction(Action.READ));
        ret.add(actionFactory.createAction(Action.GRANT));
        ret.add(actionFactory.createAction(Action.REVOKE));
        return ret;
    }

    /* (non-Javadoc)
     * @see com.blazebit.security.impl.EntityActionFactory#getActionFieldsCombinations(java.util.List, java.util.List)
     */
    @Override
    public LinkedHashMap<Action, List<String>> getActionFieldsCombinations(List<String> primitiveFields, List<String> collectionFields) {
        LinkedHashMap<Action, List<String>> ret = new LinkedHashMap<Action, List<String>>();
        List<String> allFields = new ArrayList<String>();
        allFields.addAll(primitiveFields);
        allFields.addAll(collectionFields);
        List<String> emptyFieldList = new ArrayList<String>();
        ret.put(actionFactory.createAction(Action.CREATE), allFields);
        ret.put(actionFactory.createAction(Action.DELETE), emptyFieldList);
        
        ret.put(actionFactory.createAction(Action.READ), allFields);
        ret.put(actionFactory.createAction(Action.UPDATE), primitiveFields);
        
        ret.put(actionFactory.createAction(Action.ADD), collectionFields.isEmpty() ? null : collectionFields);
        ret.put(actionFactory.createAction(Action.REMOVE), collectionFields.isEmpty() ? null : collectionFields);
        
        ret.put(actionFactory.createAction(Action.GRANT), emptyFieldList);
        ret.put(actionFactory.createAction(Action.REVOKE), emptyFieldList);
        return ret;
    }

    /* (non-Javadoc)
     * @see com.blazebit.security.impl.EntityActionFactory#getActionsForPrimitiveField()
     */
    @Override
    public Collection<Action> getActionsForPrimitiveField() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(Action.CREATE));
        ret.add(actionFactory.createAction(Action.UPDATE));
        ret.add(actionFactory.createAction(Action.READ));
        return ret;
    }
}
