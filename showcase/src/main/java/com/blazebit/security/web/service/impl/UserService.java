/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import com.blazebit.security.impl.model.User;
import java.util.List;

/**
 *
 * @author cuszk
 */
public interface UserService {

    public User createUser(String name);

    public List<User> findUsers();

    public User loadUser(User user);
}
