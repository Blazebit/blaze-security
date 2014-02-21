package com.blazebit.security.web.integration.auth;

import java.io.Serializable;
import java.security.Principal;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.blazebit.security.auth.event.PrincipalRequestEvent;
import com.blazebit.security.integration.service.UserDataAccess;

public class PrincipalRequestEventHandler implements Serializable {

	private static final long serialVersionUID = 6115693978159110310L;

	@Inject
	private UserDataAccess userDataAccess;

	public void observePrincipalRequestEvent(
			@Observes PrincipalRequestEvent event) {
		event.setPrincipal( userDataAccess.findUser(Integer
				.valueOf(event.getUserId())));
	}
}
