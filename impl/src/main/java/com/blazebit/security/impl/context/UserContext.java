/**
 *
 */
package com.blazebit.security.impl.context;

import java.io.Serializable;

import javax.security.auth.Subject;

import com.blazebit.security.impl.model.User;

/**
 * This interface defines the user context which can be used by the carrier data access and services without the need to define
 * all user context related attributes as method parameters. DataAccess an Services can access user context related attributes
 * without knowing who is providing the UserContext. The UserContext represents the logged user and provides access to necessary
 * user data.
 * 
 * @author Thomas Herzog <t.herzog@curecomp.com>
 * 
 * @company Curecomp Gmbh
 * @date 22.02.2012
 */
public interface UserContext extends Serializable {

    /**
     * Gets the logged user.
     * 
     * @return the user entity representing the logged user.
     */
    public User getUser();

}
