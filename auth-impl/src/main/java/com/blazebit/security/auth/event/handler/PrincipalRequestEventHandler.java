package com.blazebit.security.auth.event.handler;

import java.io.Serializable;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.blazebit.security.auth.event.PrincipalRequestEvent;
import com.blazebit.security.impl.service.resource.UserDataAccess;

public class PrincipalRequestEventHandler implements Serializable {

    private static final long serialVersionUID = 6115693978159110310L;

    @Inject
    private UserDataAccess userDataAccess;

    public void observePrincipalRequestEvent(@Observes PrincipalRequestEvent event) {
        event.setPrincipal(userDataAccess.findUser(Integer.valueOf(event.getUserId())));
    }
}
