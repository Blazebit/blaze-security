package com.blazebit.security.web.integration.auth;

import java.io.Serializable;

import javax.enterprise.event.Observes;

import com.blazebit.security.auth.event.GroupRequestEvent;
import com.blazebit.security.model.UserModule;

public class GroupRequestEventHandler implements Serializable {

    private static final long serialVersionUID = -7232983713632414948L;

    public void observeGroupRequestEvent(@Observes GroupRequestEvent event) {
        event.setUserModule(new UserModule(event.getName()));
    }

}
