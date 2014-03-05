/**
 * 
 */
package com.blazebit.security.showcase.impl.data;

import javax.enterprise.context.RequestScoped;

import com.blazebit.security.entity.UserContext;
import com.blazebit.security.model.Subject;

/**
 * 
 * @author Christian
 *
 */
@RequestScoped
public class DefaultUserContext implements UserContext {

    private static final long serialVersionUID = -7359439832518365341L;

    private Subject user;

    @Override
    public Subject getUser() {
        return user;
    }

    public void setUser(Subject user) {
        this.user = user;
    }

}
