/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;

import java.util.List;

/**
 * 
 * @author cuszk
 */
public interface UserService {

    public User createUser(Company company, String name);

    public User loadUser(User user);

    public void delete(User user);

    public User findUser(String username);

    List<User> findUsers(Company company);
}
