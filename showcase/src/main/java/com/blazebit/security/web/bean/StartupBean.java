/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.impl.UserGroupService;
import com.blazebit.security.web.service.impl.UserService;

/**
 * 
 * @author cuszk
 */
@Singleton
@Startup
// @ManagedBean(name="startup")
public class StartupBean {

    // @Inject
    // private UserSession userSession;
    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private RoleService roleService;
    @PersistenceContext
    private EntityManager entityManager;
    @Inject
    private ActionFactory actionFactory;
    @Inject
    private EntityFieldFactory entityFieldFactory;

    @PostConstruct
    public void startup() {
        User admin = userService.createUser("admin");
        userService.createUser("user1");
        userService.createUser("user2");

        List<UserGroup> groups = new ArrayList<UserGroup>();
        groups.add(userGroupService.createUserGroup("UserGroup1"));
        groups.add(userGroupService.createUserGroup("UserGroup2"));
        groups.add(userGroupService.createUserGroup("UserGroup3"));
        groups.add(userGroupService.createUserGroup("UserGroup4"));
        groups.add(userGroupService.createUserGroup("UserGroup5"));
        groups.add(userGroupService.createUserGroup("UserGroup6"));
        groups.add(userGroupService.createUserGroup("UserGroup7"));
        groups.add(userGroupService.createUserGroup("UserGroup8"));
        groups.add(userGroupService.createUserGroup("UserGroup9"));
        groups.add(userGroupService.createUserGroup("UserGroup10"));
        groups.add(userGroupService.createUserGroup("UserGroup11"));
        groups.add(userGroupService.createUserGroup("UserGroup12"));

        // groups.get(1).setParent(groups.get(0));
        // entityManager.merge(groups.get(1));
        // entityManager.flush();
        roleService.addGroupToGroup(groups.get(1), groups.get(0));
        roleService.addGroupToGroup(groups.get(2), groups.get(0));
        roleService.addGroupToGroup(groups.get(3), groups.get(0));
        
        roleService.addGroupToGroup(groups.get(4), groups.get(1));
        roleService.addGroupToGroup(groups.get(5), groups.get(1));
        
        roleService.addGroupToGroup(groups.get(6), groups.get(2));
        roleService.addGroupToGroup(groups.get(7), groups.get(2));
        roleService.addGroupToGroup(groups.get(8), groups.get(3));
        
        roleService.addGroupToGroup(groups.get(9), groups.get(3));
        roleService.addGroupToGroup(groups.get(10), groups.get(3));
        //
        EntityAction grantAction = actionFactory.createAction(ActionConstants.GRANT);
        EntityAction revokeAction = actionFactory.createAction(ActionConstants.REVOKE);
        EntityAction createAction = actionFactory.createAction(ActionConstants.CREATE);
        EntityAction deleteAction = actionFactory.createAction(ActionConstants.DELETE);
        EntityAction updateAction = actionFactory.createAction(ActionConstants.UPDATE);
        EntityAction readAction = actionFactory.createAction(ActionConstants.READ);
        //
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(User.class)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(User.class)));
        entityManager.persist(permissionFactory.create(admin, updateAction, entityFieldFactory.createResource(User.class)));

        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(UserGroup.class)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(UserGroup.class)));
        entityManager.persist(permissionFactory.create(admin, updateAction, entityFieldFactory.createResource(UserGroup.class)));
        

        // allow admin to create permissions, then activate interceptor <--- not needed now because only update interceptor is
        // activated
        entityManager.persist(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(User.class)));
        entityManager.persist(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserGroup.class)));
        entityManager.persist(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserPermission.class)));
        entityManager.persist(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserDataPermission.class)));
        entityManager.persist(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserGroupPermission.class)));
        entityManager.persist(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserGroupDataPermission.class)));

        entityManager.persist(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserPermission.class)));
        entityManager.persist(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserDataPermission.class)));
        entityManager.persist(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserGroupPermission.class)));
        entityManager.persist(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserGroupDataPermission.class)));

        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Carrier.class)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Party.class)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Contact.class)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(CarrierGroup.class)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Document.class)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Email.class)));

        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Carrier.class)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Party.class)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Contact.class)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(CarrierGroup.class)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Document.class)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Email.class)));

        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(grantAction)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(revokeAction)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(grantAction)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(revokeAction)));

        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(deleteAction)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(createAction)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(updateAction)));
        entityManager.persist(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(readAction)));

        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(deleteAction)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(createAction)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(updateAction)));
        entityManager.persist(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(readAction)));

        UserGroup group = groups.get(0);
        entityManager.persist(permissionFactory.create(group, createAction, entityFieldFactory.createResource(CarrierGroup.class)));
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(CarrierGroup.class)));
        entityManager.persist(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(CarrierGroup.class)));
        entityManager.persist(permissionFactory.create(group, readAction, entityFieldFactory.createResource(CarrierGroup.class)));
        group = groups.get(1);
        entityManager.persist(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Party.class)));
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Party.class)));
        entityManager.persist(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Party.class)));
        entityManager.persist(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));
        group = groups.get(2);
        entityManager.persist(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Carrier.class)));
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Carrier.class)));
        entityManager.persist(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Carrier.class)));
        entityManager.persist(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));
        group = groups.get(3);
        entityManager.persist(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Contact.class)));
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Contact.class)));
        entityManager.persist(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Contact.class)));
        entityManager.persist(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));
        group = groups.get(4);
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Contact.class, "contactField")));
        group = groups.get(6);
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Party.class, "partyField1")));
        group = groups.get(5);
        entityManager.persist(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Document.class)));
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class)));
        entityManager.persist(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Document.class)));
        entityManager.persist(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Document.class)));
        group = groups.get(8);
        entityManager.persist(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Email.class)));
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Email.class)));
        entityManager.persist(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Email.class)));
        entityManager.persist(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Document.class)));
        group = groups.get(7);
        entityManager.persist(permissionFactory.create(group, createAction, entityFieldFactory.createResource(User.class)));
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(User.class)));
        entityManager.persist(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(User.class)));
        entityManager.persist(permissionFactory.create(group, readAction, entityFieldFactory.createResource(User.class)));
        
        group = groups.get(9);
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class, "content")));
        group = groups.get(10);
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class, "title")));
        group = groups.get(11);
        entityManager.persist(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class, "size")));
        

        ChangeInterceptor.activate();
    }
}
