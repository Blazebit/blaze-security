package com.blazebit.security.auth.event;

import java.security.Principal;

public class PrincipalRequestEvent {

    private Principal principal;
    private String userId;

    public PrincipalRequestEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

}
