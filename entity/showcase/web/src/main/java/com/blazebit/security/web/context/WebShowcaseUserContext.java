/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.context;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.blazebit.security.entity.UserContext;
import com.blazebit.security.model.Company;
import com.blazebit.security.model.User;
import com.blazebit.security.showcase.context.ShowcaseUserContext;
import com.blazebit.security.showcase.data.UserDataAccess;

/**
 * 
 * @author Christian
 */
@Named
@RequestScoped
public class WebShowcaseUserContext implements ShowcaseUserContext {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Inject
    private UserDataAccess userDataAccess;
    @Inject
    private UserSession userSession;

    @Override
    public User getUser() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        if (request.getUserPrincipal() == null || request.getUserPrincipal().getName() == "guest") {
            return null;

        }
        User user = userDataAccess.findUser(Integer.valueOf(request.getUserPrincipal().getName()));
        return user;
        // return userSession.getUser();
    }

    public User getLoggedInUser() {
        return getUser();
    }

    public User getPrevLoggedInUser() {
        Integer userId = (Integer) ((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false)).getAttribute("user");
        if (userId != null) {
            return userDataAccess.findUser(userId);
        } else {
            return null;
        }
    }

    public boolean isLoggedInUser() {
        return getUser() != null;
    }

    @Override
    public Company getCompany() {
        return userSession.getSelectedCompany();
    }
}