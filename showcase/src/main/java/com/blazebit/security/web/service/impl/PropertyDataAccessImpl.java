package com.blazebit.security.web.service.impl;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.service.api.PropertyDataAccess;

@Stateless
public class PropertyDataAccessImpl implements PropertyDataAccess {

    @Inject
    private UserSession userSession;

    @Override
    public String getPropertyValue(String propertyId) {
        if (propertyId.equals(Company.USER_LEVEL)) {
            return String.valueOf(userSession.getSelectedCompany().isUserLevelEnabled());

        }
        if (propertyId.equals(Company.FIELD_LEVEL)) {
            return String.valueOf(userSession.getSelectedCompany().isFieldLevelEnabled());

        }
        if (propertyId.equals(Company.OBJECT_LEVEL)) {
            return String.valueOf(userSession.getSelectedCompany().isObjectLevelEnabled());

        }
        if (propertyId.equals(Company.GROUP_HIERARCHY)) {
            return String.valueOf(userSession.getSelectedCompany().isGroupHierarchyEnabled());

        }
        return String.valueOf(false);
    }
}
