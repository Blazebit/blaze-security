/**
 *
 */
package com.blazebit.security.entity;

import com.blazebit.security.model.Subject;

/**
 * 
 * @author Christian
 *
 */
public interface UserContext {

    /**
     * Gets the logged user.
     * 
     * @return the user entity representing the logged user.
     */
    public Subject getUser();

}
