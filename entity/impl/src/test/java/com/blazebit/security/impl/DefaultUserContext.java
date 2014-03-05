/**
 * 
 */
package com.blazebit.security.impl;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;

import com.blazebit.security.entity.UserContext;
import com.blazebit.security.model.Company;
import com.blazebit.security.model.Subject;
import com.blazebit.security.model.User;
import com.blazebit.security.showcase.context.ShowcaseUserContext;

/**
 * 
 * @author Christian
 *
 */
@RequestScoped
public class DefaultUserContext implements ShowcaseUserContext, Serializable {

    private static final long serialVersionUID = -7359439832518365341L;

    private User user;
    private Company company;

    @Override
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

}
