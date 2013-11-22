/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.PermissionHandlingBaseBean;
import com.blazebit.security.web.bean.UserSession;
import com.blazebit.security.web.service.api.CompanyService;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.service.api.UserService;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "indexBean")
@ViewScoped
public class IndexBean extends PermissionHandlingBaseBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
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

    public void saveCompanyConfiguration() {
        Company company = companyService.saveCompany(userSession.getSelectedCompany());
        userSession.setSelectedCompany(company);
        userSession.getUser().setCompany(company);
        // fix existing permissions
        if (!userSession.getSelectedCompany().isFieldLevelEnabled()) {
            List<User> users = userService.findUsers(userSession.getSelectedCompany());
            for (User user : users) {
                List<Permission> permissions = permissionManager.getPermissions(user);
                for (Permission permission : permissions) {
                    EntityField entityField = (EntityField) permission.getResource();
                    if (!entityField.isEmptyField()) {
                        if (permissionDataAccess.isGrantable(user, permission.getAction(), entityField.getParent())) {
                            permissionService.grant(userSession.getUser(), user, permission.getAction(), entityField.getParent());
                        }
                    }
                }
            }
            List<UserGroup> groups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
            for (UserGroup parent : groups) {
                replaceGroupPermissions(parent);
            }

        }
        // fix object level permissions
        if (!userSession.getSelectedCompany().isObjectLevelEnabled()) {
            List<User> users = userService.findUsers(userSession.getSelectedCompany());
            for (User user : users) {
                List<Permission> permissions = permissionManager.getPermissions(user);
                for (Permission permission : permissions) {
                    EntityField entityField = (EntityField) permission.getResource();
                    if (entityField instanceof EntityObjectField) {
                        permissionManager.remove(permission);
                    }
                }
            }
            List<UserGroup> groups = userGroupService.getAllParentGroups(userSession.getSelectedCompany());
            for (UserGroup parent : groups) {
                removeGroupObjectPermissions(parent);
            }

        }
        // fix group hierarchy
        if (!userSession.getSelectedCompany().isGroupHierarchyEnabled()) {
            List<UserGroup> groups = userGroupService.getAllGroups(userSession.getSelectedCompany());
            for (UserGroup group : groups) {
                group.setParent(null);
                userGroupService.saveGroup(group);
            }

        }
        // fix user level permissions
        // TODO adjust current user permissions to the groups the user belongs to?
    }

    private void replaceGroupPermissions(UserGroup parent) {
        List<Permission> permissions = permissionManager.getPermissions(parent);
        for (Permission permission : permissions) {
            EntityField entityField = (EntityField) permission.getResource();
            if (!entityField.isEmptyField()) {
                if (permissionDataAccess.isGrantable(parent, permission.getAction(), entityField.getParent())) {
                    permissionService.grant(userSession.getUser(), parent, permission.getAction(), entityField.getParent());
                }
            }
        }
        for (UserGroup child : userGroupService.getGroupsForGroup(parent)) {
            replaceGroupPermissions(child);
        }

    }

    private void removeGroupObjectPermissions(UserGroup parent) {
        List<Permission> permissions = permissionManager.getPermissions(parent);
        for (Permission permission : permissions) {
            EntityField entityField = (EntityField) permission.getResource();
            if (entityField instanceof EntityObjectField) {
                permissionManager.remove(permission);
            }
        }
        for (UserGroup child : userGroupService.getGroupsForGroup(parent)) {
            removeGroupObjectPermissions(child);
        }

    }
}
