package com.blazebit.security.service.impl;

import javax.inject.Inject;

import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.service.api.PropertyDataAccess;

public class PropertyDataAccessImpl implements PropertyDataAccess {

    @Inject
    private UserContext userContext;

    @Override
    public String getPropertyValue(String propertyId) {
        if (propertyId.equals(Company.USER_LEVEL)) {
            return String.valueOf(userContext.getUser().getCompany().isUserLevelEnabled());

        }
        if (propertyId.equals(Company.FIELD_LEVEL)) {
            return String.valueOf(userContext.getUser().getCompany().isFieldLevelEnabled());

        }
        if (propertyId.equals(Company.OBJECT_LEVEL)) {
            return String.valueOf(userContext.getUser().getCompany().isObjectLevelEnabled());

        }
        if (propertyId.equals(Company.GROUP_HIERARCHY)) {
            return String.valueOf(userContext.getUser().getCompany().isGroupHierarchyEnabled());

        }
        return String.valueOf(false);
    }
}
