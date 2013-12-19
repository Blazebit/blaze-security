/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.logging.Logger;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.auth.model.Credentials;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.bean.base.PermissionHandlingBaseBean;
import com.blazebit.security.web.context.UserSession;
import com.blazebit.security.web.service.api.CompanyService;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "indexBean")
@ViewScoped
public class IndexBean extends PermissionHandlingBaseBean implements Serializable {

    protected Logger log = Logger.getLogger(IndexBean.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
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

    @Inject
    @RequestScoped
    private LoginContext loginContext;

    @Inject
    private Credentials credentials;

    @PostConstruct
    public void init() {
        companies = companyService.findCompanies();
        if (!companies.isEmpty()) {
            setSelectedCompany(companyService.findCompanies().get(0));
            users = userService.findUsers(selectedCompany);
            users.add(0, userService.findUser("superAdmin", selectedCompany));
        }
    }

    public void logInAs(User user) {
        userSession.setUser(user);
        userSession.setAdmin(userService.findUser("superAdmin", selectedCompany));
        // programatic login
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();

        if (request.getUserPrincipal() == null) {
            try {
                credentials.setLogin(String.valueOf(user.getId()));
                credentials.setPassword("");
                // request.login(credentials.getLogin(), credentials.getPassword());
                loginContext.login();
                System.out.println(loginContext.getSubject().getPrincipals().iterator().next().getName());
                request.authenticate((HttpServletResponse) externalContext.getResponse());
                log.info("Logged in  " + credentials.getLogin());

                System.out.println(request.isUserInRole("DM"));
            } catch (LoginException e) {
                log.error("Login " + credentials.getLogin() + " failed", e);
            } catch (ServletException e1) {
                log.error("Login " + credentials.getLogin() + " failed", e1);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                loginContext.logout();
                // request.logout();
                logInAs(user);
            } catch (LoginException e) {
                log.error("Logout " + request.getUserPrincipal() + " failed");

                // } catch (ServletException e) {
                // log.error("Logout " + request.getUserPrincipal() + " failed");
            }
        }
        // dont redirect
        // FacesContext.getCurrentInstance().getExternalContext().redirect("user/users.xhtml");
        // FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    public void logout() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        FacesContext.getCurrentInstance().getApplication().getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, "/index.xhtml");
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
        users.add(0, userService.findUser("superAdmin", selectedCompany));
    }

    public void changeCompany(ValueChangeEvent event) {
        Company newCompany = (Company) event.getNewValue();
        setSelectedCompany(newCompany);
    }

    public void saveCompanyConfiguration() {
        Company company = companyService.saveCompany(userSession.getSelectedCompany());
        userSession.setSelectedCompany(company);
        userSession.getUser().setCompany(company);
    }

}
