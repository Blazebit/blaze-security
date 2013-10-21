package com.blazebit.security.web.bean;

import javax.inject.Inject;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Resource;
import com.blazebit.security.constants.ActionConstants;

public class SecurityBaseBean {

    @Inject
    protected EntityFieldFactory entityFieldFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;
    @Inject
    protected UserSession userSession;

    public boolean isAuthorized(ActionConstants actionConstant, String resourceName, String field) {
        try {
            return isAuthorizedResource(actionConstant, entityFieldFactory.createResource(Class.forName(resourceName), field));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isAuthorized(ActionConstants actionConstant, String field) {
        return isAuthorizedResource(actionConstant, getResource(field));
    }

    public boolean isAuthorized(ActionConstants actionConstant) {
        return isAuthorizedResource(actionConstant, getResource());
    }

    protected boolean isAuthorizedResource(ActionConstants actionConstant, Resource resource) {
        return permissionService.isGranted(userSession.getUser(), actionFactory.createAction(actionConstant), resource);
    }

    public Resource getResource() {
        return null;
    }

    public Resource getResource(String field) {
        return null;
    }

    public Resource getResource(Integer id) {
        return null;
    }

    public Resource getResource(String field, Integer id) {
        return null;
    };

}
