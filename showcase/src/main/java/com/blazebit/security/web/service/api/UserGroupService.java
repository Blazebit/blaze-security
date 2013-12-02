/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.api;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.UserGroup;

/**
 * 
 * @author cuszk
 */
public interface UserGroupService {

    /**
     * 
     * @param group
     * @return
     */
    public UserGroup save(UserGroup group);

    /**
     * 
     * @param company
     * @param name
     * @return
     */
    public UserGroup create(Company company, String name);

    /**
     * 
     * @param userGroup
     */
    public void delete(UserGroup userGroup);

}
