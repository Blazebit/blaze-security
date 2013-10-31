package com.blazebit.security.web.bean;

import javax.inject.Inject;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.IdHolder;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Resource;
import com.blazebit.security.constants.ActionConstants;

public class SecurityBaseBean {

    @Inject
    protected EntityResourceFactory entityFieldFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;
    @Inject
    protected UserSession userSession;

    /**
     * authorization check for resource name with field. usage from EL
     * 
     * @param actionConstant
     * @param resourceName
     * @param field
     * @return
     */
    public boolean isAuthorized(ActionConstants actionConstant, String resourceName, String field) {
        try {
            return isAuthorized(actionConstant, entityFieldFactory.createResource(Class.forName(resourceName), field));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * authorization check for resource name. usage from EL
     * 
     * @param actionConstant
     * @param resourceName
     * @return
     */
    public boolean isAuthorized(ActionConstants actionConstant, String resourceName) {
        try {
            return isAuthorized(actionConstant, entityFieldFactory.createResource(Class.forName(resourceName)));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * authorization check for a concrete object
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(ActionConstants actionConstant, Object entityObject) {
        if (entityObject != null && entityObject instanceof IdHolder) {
            Integer id = null;
            if (((IdHolder) entityObject).getId() != null) {
                id = ((IdHolder) entityObject).getId();
            }
            return isAuthorized(actionConstant, entityFieldFactory.createResource(entityObject.getClass(), id));
        } else {
            return false;
            // throw new IllegalArgumentException("entityobject empty for " + actionConstant);
        }
    }

    /**
     * authorization check for a field of a concrete object
     * 
     * @param actionConstant
     * @param entityObject
     * @return
     */
    public boolean isAuthorizedResource(ActionConstants actionConstant, Object entityObject, String field) {
        if (entityObject != null && entityObject instanceof IdHolder) {
            return isAuthorized(actionConstant, entityFieldFactory.createResource(entityObject.getClass(), field, ((IdHolder) entityObject).getId()));
        } else {
            // throw new IllegalArgumentException("entityobject empty for " + actionConstant + "field: " + field);
            return false;
        }
    }

    protected boolean isAuthorized(ActionConstants actionConstant, Resource resource) {
        return permissionService.isGranted(userSession.getUser(), actionFactory.createAction(actionConstant), resource);
    }

}
