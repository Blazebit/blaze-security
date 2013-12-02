/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.service.impl;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.service.api.UserGroupDataAccess;
import com.blazebit.security.web.service.api.UserGroupService;

/**
 * 
 * @author cuszk
 */
@Stateless
public class UserGroupServiceImpl implements UserGroupService {

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;
    @Inject
    private PermissionManager permissionManager;

    @Inject
    private UserGroupDataAccess userGroupDataAccess;

    @Override
    public UserGroup create(Company company, String name) {
        UserGroup ug = new UserGroup(name);
        ug.setCompany(company);
        entityManager.persist(ug);
        return ug;
    }

    @Override
    public void delete(UserGroup userGroup) {
        UserGroup reloadedUserGroup = userGroupDataAccess.loadUserGroup(userGroup);
        permissionManager.remove(reloadedUserGroup.getAllPermissions());
        permissionManager.flush();
        for (UserGroup ug : reloadedUserGroup.getUserGroups()) {
            ug.setParent(null);
            entityManager.merge(ug);
        }
        entityManager.remove(reloadedUserGroup);
        entityManager.flush();
    }

    @Override
    public UserGroup save(UserGroup ug) {
        UserGroup ret = entityManager.merge(ug);
        entityManager.flush();
        return ret;
    }
}
