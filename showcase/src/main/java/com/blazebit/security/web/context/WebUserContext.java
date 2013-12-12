/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.context;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.service.resource.UserDataAccess;

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
    private UserDataAccess userDataAccess;

    @Override
    public User getUser() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        User user = userDataAccess.findUser(Integer.valueOf(request.getUserPrincipal().getName()));
        return user;
        // return userSession.getUser();
    }

}