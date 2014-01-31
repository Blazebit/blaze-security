package com.blazebit.security.web.integration.service;

import javax.inject.Inject;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.web.context.UserSession;
import com.blazebit.security.web.context.WebUserContext;

public class PropertyDataAccessImpl {

	@Inject
	private UserSession userSession;

	@Inject
	private WebUserContext userContext;

	public static final String USER_LEVEL = "USER_LEVEL";
	public static final String FIELD_LEVEL = "FIELD_LEVEL";
	public static final String OBJECT_LEVEL = "OBJECT_LEVEL";
	public static final String GROUP_HIERARCHY = "GROUP_HIERARCHY";
	public static final String ACT_AS_USER = "ACT_AS_USER";

	public String getPropertyValue(String propertyId) {
		if (userContext.getUser() != null
				&& userSession.getSelectedCompany() != null) {
			if (propertyId.equals(Company.USER_LEVEL)) {
				return String.valueOf(userSession.getSelectedCompany()
						.isUserLevelEnabled());

			}
			if (propertyId.equals(Company.FIELD_LEVEL)) {
				return String.valueOf(userSession.getSelectedCompany()
						.isFieldLevelEnabled());

			}
			if (propertyId.equals(Company.OBJECT_LEVEL)) {
				return String.valueOf(userSession.getSelectedCompany()
						.isObjectLevelEnabled());

			}
			if (propertyId.equals(Company.GROUP_HIERARCHY)) {
				return String.valueOf(userSession.getSelectedCompany()
						.isGroupHierarchyEnabled());

			}
			if (propertyId.equals(Company.ACT_AS_USER)) {
				return String.valueOf(userSession.getSelectedCompany()
						.isActAsUser());

			}
		}
		return String.valueOf(false);
	}
}
