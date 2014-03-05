package com.blazebit.security.showcase.impl.data;

import javax.inject.Inject;

import com.blazebit.security.model.Company;
import com.blazebit.security.model.Features;
import com.blazebit.security.showcase.context.ShowcaseUserContext;
import com.blazebit.security.showcase.data.PropertyDataAccess;

public class PropertyDataAccessImpl implements PropertyDataAccess {

	@Inject
	private ShowcaseUserContext userContext;

	@Override
    public String getPropertyValue(String propertyId) {
		if (userContext.getUser() != null
				&& userContext.getCompany() != null) {
		    Company company = userContext.getCompany();
		    
			if (propertyId.equals(Features.USER_LEVEL)) {
				return String.valueOf(company
						.isUserLevelEnabled());

			}
			if (propertyId.equals(Features.FIELD_LEVEL)) {
				return String.valueOf(company
						.isFieldLevelEnabled());

			}
			if (propertyId.equals(Features.OBJECT_LEVEL)) {
				return String.valueOf(company
						.isObjectLevelEnabled());

			}
			if (propertyId.equals(Features.GROUP_HIERARCHY)) {
				return String.valueOf(company
						.isGroupHierarchyEnabled());

			}
			if (propertyId.equals(Features.ACT_AS_USER)) {
				return String.valueOf(company
						.isActAsUser());

			}
		}
		return String.valueOf(false);
	}
}
