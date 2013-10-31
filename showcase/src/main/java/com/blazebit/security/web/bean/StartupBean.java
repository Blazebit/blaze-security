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

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.service.api.ActionUtils;
import com.blazebit.security.web.service.api.CompanyService;
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
    private PermissionManager permissionManager;
    @Inject
    private RoleService roleService;

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;
    @Inject
    private ActionUtils actionUtils;
    @Inject
    private ActionFactory actionFactory;
    @Inject
    private EntityResourceFactory entityFieldFactory;
    @Inject
    private CompanyService companyService;

    @PostConstruct
    public void startup() {
        if (companyService.findCompanies().isEmpty()) {
            Company company = new Company("Company 1");
            entityManager.persist(company);

            Company comp2 = new Company("Company 2");
            entityManager.persist(comp2);
            User user2 = userService.createUser(comp2, "admin");
            for (Permission p : getInitialAdminRights(user2)) {
                entityManager.persist(p);
            }

            Company comp3 = new Company("Company 3");
            entityManager.persist(comp3);
            User user3 = userService.createUser(comp3, "admin");
            for (Permission p : getInitialAdminRights(user3)) {
                entityManager.persist(p);
            }

            Company comp4 = new Company("Company 4");
            entityManager.persist(comp4);
            User user4 = userService.createUser(comp4, "admin");
            for (Permission p : getInitialAdminRights(user4)) {
                entityManager.persist(p);
            }

            User admin = userService.createUser(company, "admin");
            for (Permission p : getInitialAdminRights(admin)) {
                entityManager.persist(p);
            }

            userService.createUser(company, "user1");
            userService.createUser(company, "user2");

            List<UserGroup> groups = new ArrayList<UserGroup>();
            groups.add(userGroupService.createUserGroup(company, "UserGroup1"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup2"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup3"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup4"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup5"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup6"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup7"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup8"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup9"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup10"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup11"));
            groups.add(userGroupService.createUserGroup(company, "UserGroup12"));
            groups.add(userGroupService.createUserGroup(company, "CarrierPage"));
            groups.add(userGroupService.createUserGroup(company, "GrantGroup"));
            groups.add(userGroupService.createUserGroup(company, "RevokeGroup"));

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
            Action grantAction = actionFactory.createAction(ActionConstants.GRANT);
            Action revokeAction = actionFactory.createAction(ActionConstants.REVOKE);
            Action createAction = actionFactory.createAction(ActionConstants.CREATE);
            Action deleteAction = actionFactory.createAction(ActionConstants.DELETE);
            Action updateAction = actionFactory.createAction(ActionConstants.UPDATE);
            Action readAction = actionFactory.createAction(ActionConstants.READ);

            UserGroup group = groups.get(0);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(CarrierGroup.class)));
            group = groups.get(1);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));
            group = groups.get(2);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Carrier.class)));
            group = groups.get(3);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Contact.class)));
            group = groups.get(4);
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Contact.class, "contactField")));
            group = groups.get(6);
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Party.class, "partyField1")));
            group = groups.get(5);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Document.class)));
            group = groups.get(8);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Email.class)));
            group = groups.get(7);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(User.class)));

            group = groups.get(9);
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class, "content")));
            group = groups.get(10);
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class, "title")));
            group = groups.get(11);
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class, "size")));

            group = groups.get(12);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Contact.class)));

            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(User.class)));

            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(UserPermission.class)));
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(UserDataPermission.class)));

            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(UserPermission.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(UserDataPermission.class)));

            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Contact.class)));

            group = groups.get(13);
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(User.class)));

            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(UserPermission.class)));
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(UserDataPermission.class)));

            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(CarrierGroup.class)));

            group = groups.get(14);
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(User.class)));

            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(UserPermission.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(UserDataPermission.class)));

            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(CarrierGroup.class)));

        }
        ChangeInterceptor.activate();
    }

    private List<Permission> getInitialAdminRights(User admin) {
        List<Permission> ret = new ArrayList<Permission>();
        Action grantAction = actionFactory.createAction(ActionConstants.GRANT);
        Action revokeAction = actionFactory.createAction(ActionConstants.REVOKE);
        Action createAction = actionFactory.createAction(ActionConstants.CREATE);
        Action deleteAction = actionFactory.createAction(ActionConstants.DELETE);
        Action updateAction = actionFactory.createAction(ActionConstants.UPDATE);
        //
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(User.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(User.class)));
        ret.add(permissionFactory.create(admin, updateAction, entityFieldFactory.createResource(User.class)));
        ret.add(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(User.class)));
        ret.add(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(User.class)));

        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(UserGroup.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(UserGroup.class)));
        ret.add(permissionFactory.create(admin, updateAction, entityFieldFactory.createResource(UserGroup.class)));
        ret.add(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserGroup.class)));
        ret.add(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserGroup.class)));

        ret.add(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserPermission.class)));
        ret.add(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserDataPermission.class)));
        ret.add(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserGroupPermission.class)));
        ret.add(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(UserGroupDataPermission.class)));

        ret.add(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserPermission.class)));
        ret.add(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserDataPermission.class)));
        ret.add(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserGroupPermission.class)));
        ret.add(permissionFactory.create(admin, deleteAction, entityFieldFactory.createResource(UserGroupDataPermission.class)));

        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(UserPermission.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(UserDataPermission.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(UserGroupPermission.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(UserGroupDataPermission.class)));

        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(UserPermission.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(UserDataPermission.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(UserGroupPermission.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(UserGroupDataPermission.class)));

        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Carrier.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Party.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Contact.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(CarrierGroup.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Document.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Email.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Comment.class)));

        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Carrier.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Party.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Contact.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(CarrierGroup.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Document.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Email.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Comment.class)));

        // ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(grantAction)));
        // ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(revokeAction)));
        // ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(grantAction)));
        // ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(revokeAction)));
        //
        // ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(deleteAction)));
        // ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(createAction)));
        // ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(updateAction)));
        // ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(readAction)));
        //
        // ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(deleteAction)));
        // ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(createAction)));
        // ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(updateAction)));
        // ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(readAction)));

        return ret;
    }
}
