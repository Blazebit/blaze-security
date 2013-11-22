package com.blazebit.security.web.service.impl;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.service.api.PropertyDataAccess;

@Stateless
public class PropertyDataAccessImpl implements PropertyDataAccess {

    @Inject
    private UserSession userSession;

    private static final String USER_LEVEL = "user_level";
    private static final String FIELD_LEVEL = "field_level";
    private static final String OBJECT_LEVEL = "object_level";
    private static final String GROUP_HIERARCHY = "group_hierarchy";

    @Override
    public Boolean getPropertyValue(String propertyId) {
        if (propertyId.equals(USER_LEVEL)) {
            return userSession.getSelectedCompany().isUserLevelEnabled();

        }
        if (propertyId.equals(FIELD_LEVEL)) {
            return userSession.getSelectedCompany().isFieldLevelEnabled();

        }
        if (propertyId.equals(OBJECT_LEVEL)) {
            return userSession.getSelectedCompany().isObjectLevelEnabled();

        }
        if (propertyId.equals(GROUP_HIERARCHY)) {
            return userSession.getSelectedCompany().isGroupHierarchyEnabled();

        }
        return false;
    }
}
