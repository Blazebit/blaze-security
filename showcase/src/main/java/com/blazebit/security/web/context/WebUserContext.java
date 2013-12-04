/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.context;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.User;

/**
 * 
 * @author Christian
 */
@Named
@RequestScoped
public class WebUserContext implements UserContext {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private UserSession userSession;

    @Override
    public User getUser() {
        return userSession.getUser();
    }

}