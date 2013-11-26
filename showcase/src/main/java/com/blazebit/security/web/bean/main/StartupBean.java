/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main;

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
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.service.api.CompanyService;
import com.blazebit.security.web.service.api.RoleService;
import com.blazebit.security.web.service.api.UserGroupService;
import com.blazebit.security.web.service.api.UserService;

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

            groups.add(userGroupService.createUserGroup(company, "UserGroup"));
            groups.add(userGroupService.createUserGroup(company, "User"));
            groups.add(userGroupService.createUserGroup(company, "Admin"));

            groups.add(userGroupService.createUserGroup(company, "Base"));
            groups.add(userGroupService.createUserGroup(company, "Carrier"));
            groups.add(userGroupService.createUserGroup(company, "CarrierGroup"));

            groups.add(userGroupService.createUserGroup(company, "Document"));

            groups.add(userGroupService.createUserGroup(company, "Comment"));
            
            groups.add(userGroupService.createUserGroup(company, "Company"));

            roleService.addGroupToGroup(groups.get(1), groups.get(0));
            roleService.addGroupToGroup(groups.get(2), groups.get(1));

            roleService.addGroupToGroup(groups.get(4), groups.get(3));
            roleService.addGroupToGroup(groups.get(5), groups.get(4));

            //
            Action grantAction = actionFactory.createAction(ActionConstants.GRANT);
            Action revokeAction = actionFactory.createAction(ActionConstants.REVOKE);
            Action createAction = actionFactory.createAction(ActionConstants.CREATE);
            Action deleteAction = actionFactory.createAction(ActionConstants.DELETE);
            Action updateAction = actionFactory.createAction(ActionConstants.UPDATE);
            Action readAction = actionFactory.createAction(ActionConstants.READ);
            Action addAction = actionFactory.createAction(ActionConstants.ADD);
            Action removeAction = actionFactory.createAction(ActionConstants.REMOVE);

            // Usergroup
            UserGroup group = groups.get(0);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, addAction, entityFieldFactory.createResource(UserGroup.class, "users")));
            permissionManager.save(permissionFactory.create(group, removeAction, entityFieldFactory.createResource(UserGroup.class, "users")));
            group = groups.get(1);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, addAction, entityFieldFactory.createResource(User.class, "userGroups")));
            permissionManager.save(permissionFactory.create(group, removeAction, entityFieldFactory.createResource(User.class, "userGroups")));
            group = groups.get(2);
            // admin
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(UserGroup.class)));

            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource("Carrier_Party")));

            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(UserGroup.class)));

            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource("Carrier_Party")));

            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Comment.class)));

            // Base
            group = groups.get(3);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));

            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Contact.class)));

            group = groups.get(4);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Carrier.class)));

            permissionManager.save(permissionFactory.create(group, addAction, entityFieldFactory.createResource(Carrier.class, "contacts")));
            permissionManager.save(permissionFactory.create(group, removeAction, entityFieldFactory.createResource(Carrier.class, "contacts")));
            
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource("Carrier_Party")));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource("Carrier_Party")));

            group = groups.get(5);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(CarrierGroup.class)));

            group = groups.get(6);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Document.class)));

            group = groups.get(7);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Comment.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Comment.class, "user")));
            group = groups.get(8);
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Company.class)));
            
            
            roleService.addSubjectToRole(admin, groups.get(2));
            permissionManager.flush();
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
        Action readAction = actionFactory.createAction(ActionConstants.READ);
        Action addAction = actionFactory.createAction(ActionConstants.ADD);
        Action removeAction = actionFactory.createAction(ActionConstants.REMOVE);
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

        ret.add(permissionFactory.create(admin, updateAction, entityFieldFactory.createResource(Company.class)));

        ret.add(permissionFactory.create(admin, addAction, entityFieldFactory.createResource(UserGroup.class, "users")));
        ret.add(permissionFactory.create(admin, removeAction, entityFieldFactory.createResource(UserGroup.class, "users")));
        ret.add(permissionFactory.create(admin, addAction, entityFieldFactory.createResource(User.class, "userGroups")));
        ret.add(permissionFactory.create(admin, removeAction, entityFieldFactory.createResource(User.class, "userGroups")));

        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Carrier.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource("Carrier_Party")));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Party.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Contact.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(CarrierGroup.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Document.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Email.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Comment.class)));
        ret.add(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Company.class)));

        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Carrier.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Party.class)));
        // TODO safer?
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource("Carrier_Party")));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Contact.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(CarrierGroup.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Document.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Email.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Comment.class)));
        ret.add(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Company.class)));

        ret.add(permissionFactory.create(admin, readAction, entityFieldFactory.createResource(Carrier.class)));
        ret.add(permissionFactory.create(admin, readAction, entityFieldFactory.createResource(Party.class)));
        ret.add(permissionFactory.create(admin, readAction, entityFieldFactory.createResource(Contact.class)));
        ret.add(permissionFactory.create(admin, readAction, entityFieldFactory.createResource(CarrierGroup.class)));
        ret.add(permissionFactory.create(admin, readAction, entityFieldFactory.createResource(Document.class)));
        ret.add(permissionFactory.create(admin, readAction, entityFieldFactory.createResource(Email.class)));
        ret.add(permissionFactory.create(admin, readAction, entityFieldFactory.createResource(Comment.class)));

        return ret;
    }
}
