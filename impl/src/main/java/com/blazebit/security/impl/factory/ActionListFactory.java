package com.blazebit.security.impl.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.factory.ActionFactory;
import com.blazebit.security.model.Action;

public class ActionListFactory {

    @Inject
    private ActionFactory actionFactory;

    /**
     * list of possible actions for an entity resource
     * 
     * @return
     */
    public List<Action> getActionsForEntity() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.DELETE));
        ret.add(actionFactory.createAction(ActionConstants.GRANT));
        ret.add(actionFactory.createAction(ActionConstants.REVOKE));
        return ret;
    }

    /**
     * list of possible actions for an entity resource
     * 
     * @return
     */
    public List<Action> getCommonActionsForEntityAndFields() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.CREATE));
        ret.add(actionFactory.createAction(ActionConstants.READ));
        return ret;
    }

    public List<Action> getUpdateActionsForCollectionField() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.ADD));
        ret.add(actionFactory.createAction(ActionConstants.REMOVE));
        return ret;
    }

    /**
     * list of possible actions for an object
     * 
     * @return
     */
    public List<Action> getActionsForEntityObject() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.UPDATE));
        ret.add(actionFactory.createAction(ActionConstants.DELETE));
        ret.add(actionFactory.createAction(ActionConstants.READ));
        ret.add(actionFactory.createAction(ActionConstants.GRANT));
        ret.add(actionFactory.createAction(ActionConstants.REVOKE));
        return ret;
    }

    /**
     * 
     * @param primitiveFields
     * @param collectionFields
     * @return action and field associations
     */
    public LinkedHashMap<Action, List<String>> getActionFieldsCombinations(List<String> primitiveFields, List<String> collectionFields) {
        LinkedHashMap<Action, List<String>> ret = new LinkedHashMap<Action, List<String>>();
        List<String> allFields = new ArrayList<String>();
        allFields.addAll(primitiveFields);
        allFields.addAll(collectionFields);
        List<String> emptyFieldList = new ArrayList<String>();
        ret.put(actionFactory.createAction(ActionConstants.CREATE), allFields);
        ret.put(actionFactory.createAction(ActionConstants.DELETE), emptyFieldList);
        
        ret.put(actionFactory.createAction(ActionConstants.READ), allFields);
        ret.put(actionFactory.createAction(ActionConstants.UPDATE), primitiveFields);
        
        ret.put(actionFactory.createAction(ActionConstants.ADD), collectionFields.isEmpty() ? null : collectionFields);
        ret.put(actionFactory.createAction(ActionConstants.REMOVE), collectionFields.isEmpty() ? null : collectionFields);
        
        ret.put(actionFactory.createAction(ActionConstants.GRANT), emptyFieldList);
        ret.put(actionFactory.createAction(ActionConstants.REVOKE), emptyFieldList);
        return ret;
    }

    public Collection<Action> getActionsForPrimitiveField() {
        List<Action> ret = new ArrayList<Action>();
        ret.add(actionFactory.createAction(ActionConstants.CREATE));
        ret.add(actionFactory.createAction(ActionConstants.UPDATE));
        ret.add(actionFactory.createAction(ActionConstants.READ));
        return ret;
    }
}
