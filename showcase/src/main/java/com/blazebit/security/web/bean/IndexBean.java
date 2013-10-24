/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.inject.Inject;

import org.primefaces.event.SelectEvent;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.service.api.CompanyService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "indexBean")
@ViewScoped
public class IndexBean implements Serializable {

    @Inject
    private UserService userService;
    @Inject
    private UserSession userSession;
    @Inject
    private PermissionManager permissionManager;

    private List<User> users = new ArrayList<User>();
    private Company selectedCompany;
    List<Company> companies;

    @Inject
    private CompanyService companyService;

    @PostConstruct
    public void init() {
        companies = companyService.findCompanies();
        if (!companies.isEmpty()) {
            setSelectedCompany(companyService.findCompanies().get(0));
            users = userService.findUsers(selectedCompany);
        }
    }

    public void logInAs(User user) throws IOException {
        userSession.setUser(user);
        userSession.setAdmin(userService.findUser("admin", selectedCompany));
        // FacesContext.getCurrentInstance().getExternalContext().redirect("user/users.xhtml");
        // FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    public void beUser(User user) throws IOException {
        userSession.setSecondLoggedInUser(user);
        // FacesContext.getCurrentInstance().getExternalContext().redirect("user/users.xhtml");
        // FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    public void reset(User user) {
        permissionManager.removeAllPermissions(user);
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<Company> getCompanies() {
        return companies;
    }

    public Company getSelectedCompany() {
        return selectedCompany;
    }

    public void setSelectedCompany(Company selectedCompany) {
        this.selectedCompany = selectedCompany;
        userSession.setSelectedCompany(selectedCompany);
        users = userService.findUsers(selectedCompany);
    }

    public void changeCompany(ValueChangeEvent event) {
        Company newCompany = (Company) event.getNewValue();
        setSelectedCompany(newCompany);
    }
}
