/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.service.api;

import java.util.List;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;

/**
 * 
 * @author cuszk
 */
public interface UserService {

    public User createUser(Company company, String name);

    public User loadUser(User user);

    public void delete(User user);

    public User findUser(String username, Company company);

    public List<User> findUsers(Company company);
}
