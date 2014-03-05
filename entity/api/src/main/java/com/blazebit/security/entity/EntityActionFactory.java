package com.blazebit.security.entity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.blazebit.security.model.Action;

public interface EntityActionFactory {

    /**
     * list of possible actions for an entity resource
     * 
     * @return
     */
    public abstract List<Action> getActionsForEntity();

    /**
     * list of possible actions for an entity resource
     * 
     * @return
     */
    public abstract List<Action> getCommonActionsForEntityAndFields();

    public abstract List<Action> getUpdateActionsForCollectionField();

    /**
     * list of possible actions for an object
     * 
     * @return
     */
    public abstract List<Action> getActionsForEntityObject();

    /**
     * 
     * @param primitiveFields
     * @param collectionFields
     * @return action and field associations
     */
    public abstract LinkedHashMap<Action, List<String>> getActionFieldsCombinations(List<String> primitiveFields, List<String> collectionFields);

    public abstract Collection<Action> getActionsForPrimitiveField();

}