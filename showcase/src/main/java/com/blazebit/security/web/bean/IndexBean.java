/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.web.service.impl.UserService;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

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
    private List<User> users = new ArrayList<User>();

    @PostConstruct
    public void init() {
        users = userService.findUsers();
    }

    public void logInAs(User user) throws IOException {
        userSession.setUser(user);
        FacesContext.getCurrentInstance().getExternalContext().redirect("user/users.xhtml");
        FacesContext.getCurrentInstance().setViewRoot(new UIViewRoot());
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
