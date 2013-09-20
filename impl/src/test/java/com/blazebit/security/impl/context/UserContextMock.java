/**
 *
 */
package com.blazebit.security.impl.context;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import javax.enterprise.context.RequestScoped;

/**
 * This is the mock used for the test where no logged user is present. This
 * implementation uses the dataManager directly and returns the instances by
 * merging them with the persistence context. The instances are retrieved new
 * when their id changes.
 *
 * @author Thomas Herzog <t.herzog@curecomp.com>
 *
 * @company Curecomp Gmbh
 * @date 29.11.2012
 */
@RequestScoped
public class UserContextMock implements UserContext {

    private static final long serialVersionUID = -7359439832518365341L;
    private User user;

    /**
     * Empty Constructor
     */
    public UserContextMock() {
        super();
    }

    @Override
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public User getSelectedUser() {
        return null;
    }

    @Override
    public UserGroup getSelectedGroup() {
        return null;
    }
}
