/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.jboss.logging.Logger;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.auth.model.Credentials;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.service.resource.UserDataAccess;
import com.blazebit.security.web.context.UserSession;
import com.blazebit.security.web.service.api.CompanyService;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "indexBean")
@ViewScoped
public class IndexBean implements Serializable {

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

    @Inject
    private UserDataAccess userDataAccess;

    private List<User> users = new ArrayList<User>();
    private Company selectedCompany;
    List<Company> companies;

    @Inject
    private CompanyService companyService;

    @Inject
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
        credentials.setLogin(String.valueOf(user.getId()));
        credentials.setPassword("");
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        if (request.getUserPrincipal() == null || request.getUserPrincipal().getName().equals("guest")) {
            // if (getLoggedInPrincipal(loginContext.getSubject()) == null) {
            try {
                loginContext.login();
                if (loginContext.getSubject() != null) {
                    request.logout();
                    request.authenticate((HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse());
                    if (request.getUserPrincipal() != null) {
                        log.info("Logged in  " + getLoggedInPrincipal(loginContext.getSubject()));
                        log.info("Logged in  " + request.getUserPrincipal().getName());
                    }

                    userSession.setAdmin(userService.findUser("superAdmin", selectedCompany));
                    userSession.setSelectedCompany(selectedCompany);
                } else {
                    throw new LoginException("Failed to get logged in subject");
                }
            } catch (LoginException e) {
                log.error("Login " + credentials.getLogin() + " failed", e);
            } catch (IOException e) {
            } catch (ServletException e) {
            }
        } else {
            log.warn("Already logged in: " + getLoggedInPrincipal(loginContext.getSubject()));
        }
    }

    /**
     * 
     * @param subject
     * @return logged in user name
     */
    private String getLoggedInPrincipal(Subject subject) {
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            if (!principals.isEmpty()) {
                Principal principal = principals.iterator().next();
                if (principal != null) {
                    return principal.getName();
                }
            }
        }
        return null;
    }

    public void logout() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        if (((HttpServletRequest) externalContext.getRequest()).getUserPrincipal() != null) {
            // if (loginContext.getSubject() != null) {
            HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            try {
                // LoginModule's logout. Clears subject and roles.
                if (loginContext.getSubject() != null) {
                    loginContext.logout();
                }
                // logout clear the user principal
                request.logout();
            } catch (LoginException e) {
                log.error("Logout " + request.getUserPrincipal() + " failed", e);
            } catch (ServletException e) {
                log.error("Logout " + request.getUserPrincipal() + " failed", e);
            }
            HttpSession session = (HttpSession) externalContext.getSession(false);
            if (session != null) {
                session.invalidate();
                externalContext.getSession(true);
            }
        } else {
            log.warn("Already logged out.");
        }
    }

    public void beUser(User user) {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        if (((HttpServletRequest) externalContext.getRequest()).getUserPrincipal() != null && ((HttpServletRequest) externalContext.getRequest()).getUserPrincipal().getName() != null) {
            // if (loginContext.getSubject() != null) {
            // String prevLoggedInUser = getLoggedInPrincipal(loginContext.getSubject());
            String prevLoggedInUser = ((HttpServletRequest) externalContext.getRequest()).getUserPrincipal().getName();
            logout();
            logInAs(user);
            // store previous logged in user in session
            externalContext.getSessionMap().put("user", Integer.valueOf(prevLoggedInUser));
        } else {
            log.warn("No one is logged in.");
        }
    }

    public void resetLoggedInUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);
        Integer userId = (Integer) session.getAttribute("user");
        User user = userDataAccess.findUser(userId);

        if (user != null) {
            logout();
            logInAs(user);
        } else {
            log.warn("No logged in user to restore.");
        }
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

    public void handleCompanyChange(AjaxBehaviorEvent event) {
        System.out.println("change");
    }

    public void saveCompanyConfiguration() {
        Company company = companyService.saveCompany(selectedCompany);

        // userSession.setSelectedCompany(company);
    }

}
