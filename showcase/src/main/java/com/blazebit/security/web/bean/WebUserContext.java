/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * 
 * @author Christian
 */
@Named
@RequestScoped
public class WebUserContext implements UserContext {

    @Inject
    private UserSession userSession;

    @Override
    public User getUser() {
        return userSession.getUser();
    }

}
