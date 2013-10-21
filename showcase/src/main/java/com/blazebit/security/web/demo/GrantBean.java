package com.blazebit.security.web.demo;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.PermissionActionException;
import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionService;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.bean.SecurityBaseBean;
import com.blazebit.security.web.service.impl.UserService;

@Named
@ViewScoped
@Stateless
public class GrantBean extends SecurityBaseBean {

    @PersistenceContext
    EntityManager entityManager;

    private Integer id;
    private String resourceName;

    @Inject
    private UserService userService;

    @Inject
    private EntityFieldFactory entityFieldFactory;

    @Inject
    private ActionFactory actionFactory;

    @Inject
    private PermissionService permissionService;

    private List<String> selectedActions = new ArrayList<String>();
    private List<EntityAction> actions = new ArrayList<EntityAction>();

    private List<String> selectedUsers = new ArrayList<String>();
    private List<User> users = new ArrayList<User>();

    private String resultMessage;

    @PostConstruct
    public void init() {
        actions = actionFactory.getActionsForEntityObject();
        users = userService.findUsers(userSession.getSelectedCompany());
        users.remove(userSession.getUser());
    }

    public void grant() {
        StringBuilder ret = new StringBuilder();
        EntityField entityObjectField = null;
        try {
            entityObjectField = (EntityField) entityFieldFactory.createResource(Class.forName(resourceName), id);
        } catch (ClassNotFoundException e) {
            ret.append("Wrong parameters").append("\n");
        }
        if (entityObjectField != null) {
            for (String id : selectedUsers) {
                User user = entityManager.find(User.class, Integer.valueOf(id));
                for (String actionName : selectedActions) {
                    EntityAction action = actionFactory.createAction(ActionConstants.valueOf(actionName));
                    try {
                        permissionService.grant(userSession.getUser(), user, action, entityObjectField);
                        ret.append("Permission granted").append("\n");
                    } catch (Exception e) {
                        ret.append("Permission cannot be granted by the current user").append("\n");
                    }
                    // } catch (PermissionException pe) {
                    // ret.append("Permission cannot be granted by the current user").append("\n");
                    // } catch (PermissionActionException pae) {
                    // ret.append("Permission cannot be granted to the selected user").append("\n");
                    // }
                }
            }
        }
        resultMessage = ret.toString();
    }

    public List<String> getSelectedActions() {
        return selectedActions;
    }

    public void setSelectedActions(List<String> selectedActions) {
        this.selectedActions = selectedActions;
    }

    public List<String> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<String> selectedUsers) {
        this.selectedUsers = selectedUsers;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<EntityAction> getActions() {
        return actions;
    }

    public void setActions(List<EntityAction> actions) {
        this.actions = actions;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

}
