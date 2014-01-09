/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.context;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;

/**
 * 
 * @author Christian
 */
@Named
@SessionScoped
public class UserSession implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private User selectedUser;
    private UserGroup selectedUserGroup;

    private Company company;
    private User admin;

    public User getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(User selectedUser) {
        this.selectedUserGroup = null;
        this.selectedUser = selectedUser;
    }

    public UserGroup getSelectedUserGroup() {
        return selectedUserGroup;
    }

    public void setSelectedUserGroup(UserGroup selectedUserGroup) {
        this.selectedUser = null;
        this.selectedUserGroup = selectedUserGroup;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        this.admin = admin;
    }

    public User getPrevLoggedInUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);
        return (User) session.getAttribute("user");
    }

    public void setSelectedCompany(Company selectedCompany) {
        this.company = selectedCompany;
    }

    public Company getSelectedCompany() {
        return company;
    }

}
