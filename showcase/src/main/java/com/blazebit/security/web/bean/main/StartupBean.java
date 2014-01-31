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

import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.factory.ActionFactory;
import com.blazebit.security.factory.EntityResourceFactory;
import com.blazebit.security.factory.PermissionFactory;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.metamodel.ResourceMetamodel;
import com.blazebit.security.model.Action;
import com.blazebit.security.spi.ResourceDefinition;
import com.blazebit.security.web.service.api.CompanyService;
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

    @Inject
    private UserService userService;
    @Inject
    private UserGroupService userGroupService;
    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private PermissionManager permissionManager;

    @PersistenceContext(unitName = "TestPU")
    private EntityManager entityManager;
    @Inject
    private ActionFactory actionFactory;
    @Inject
    private EntityResourceFactory entityFieldFactory;
    @Inject
    private CompanyService companyService;

    @Inject
    private ResourceMetamodel metamodel;

    @PostConstruct
    public void startup() {
        if (companyService.findCompanies().isEmpty()) {
            //
            Action grantAction = actionFactory.createAction(ActionConstants.GRANT);
            Action revokeAction = actionFactory.createAction(ActionConstants.REVOKE);
            Action createAction = actionFactory.createAction(ActionConstants.CREATE);
            Action deleteAction = actionFactory.createAction(ActionConstants.DELETE);
            Action updateAction = actionFactory.createAction(ActionConstants.UPDATE);
            Action readAction = actionFactory.createAction(ActionConstants.READ);
            Action addAction = actionFactory.createAction(ActionConstants.ADD);
            Action removeAction = actionFactory.createAction(ActionConstants.REMOVE);

            Company company = new Company("Company 1");
            entityManager.persist(company);
            User superAdmin = userService.createUser(company, "superAdmin");
            User admin = userService.createUser(company, "admin");
            userService.createUser(company, "user1");
            userService.createUser(company, "user2");

            Company comp2 = new Company("Company 2");
            entityManager.persist(comp2);
            User admin2 = userService.createUser(comp2, "superAdmin");

            Company comp3 = new Company("Company 3");
            entityManager.persist(comp3);
            User admin3 = userService.createUser(comp3, "superAdmin");

            Company comp4 = new Company("Company 4");
            entityManager.persist(comp4);
            User admin4 = userService.createUser(comp4, "superAdmin");

            for (ResourceDefinition def : metamodel.getResourceDefinitions().keySet()) {
                permissionManager.save(permissionFactory.create(superAdmin, grantAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin2, grantAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin3, grantAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin4, grantAction, entityFieldFactory.createResource(def.getResourceName())));

                permissionManager.save(permissionFactory.create(superAdmin, revokeAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin2, revokeAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin3, revokeAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin4, revokeAction, entityFieldFactory.createResource(def.getResourceName())));

                permissionManager.save(permissionFactory.create(superAdmin, readAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin2, readAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin3, readAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin4, readAction, entityFieldFactory.createResource(def.getResourceName())));

                permissionManager.save(permissionFactory.create(superAdmin, updateAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin2, updateAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin3, updateAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin4, updateAction, entityFieldFactory.createResource(def.getResourceName())));

                permissionManager.save(permissionFactory.create(superAdmin, createAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin2, createAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin3, createAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin4, createAction, entityFieldFactory.createResource(def.getResourceName())));

                permissionManager.save(permissionFactory.create(superAdmin, deleteAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin2, deleteAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin3, deleteAction, entityFieldFactory.createResource(def.getResourceName())));
                permissionManager.save(permissionFactory.create(admin4, deleteAction, entityFieldFactory.createResource(def.getResourceName())));

            }

            UserGroup adminGroup = userGroupService.create(company, "Admin");

            List<UserGroup> groups = new ArrayList<UserGroup>();

            groups.add(userGroupService.create(company, "Company"));
            groups.add(userGroupService.create(company, "UserGroup"));
            groups.add(userGroupService.create(company, "User"));
            groups.add(adminGroup);

            groups.add(userGroupService.create(company, "Base"));
            groups.add(userGroupService.create(company, "Carrier"));

            groups.add(userGroupService.create(company, "Empty"));
            groups.add(userGroupService.create(company, "Document"));
            groups.add(userGroupService.create(company, "Comment"));
            
            groups.add(userGroupService.create(company, "View Users"));

            userGroupService.addGroupToGroup(groups.get(1), groups.get(0));
            userGroupService.addGroupToGroup(groups.get(2), groups.get(1));
            userGroupService.addGroupToGroup(groups.get(3), groups.get(2));

            userGroupService.addGroupToGroup(groups.get(5), groups.get(4));

            UserGroup group = groups.get(0);
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Company.class)));
            // Usergroup
            group = groups.get(1);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(UserGroup.class)));
            
            permissionManager.save(permissionFactory.create(group, addAction, entityFieldFactory.createResource(UserGroup.class, "users")));
            permissionManager.save(permissionFactory.create(group, removeAction, entityFieldFactory.createResource(UserGroup.class, "users")));
            
            permissionManager.save(permissionFactory.create(group, addAction, entityFieldFactory.createResource(UserGroup.class, "userGroups")));
            permissionManager.save(permissionFactory.create(group, removeAction, entityFieldFactory.createResource(UserGroup.class, "userGroups")));
            group = groups.get(2);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, addAction, entityFieldFactory.createResource(User.class, "userGroups")));
            permissionManager.save(permissionFactory.create(group, removeAction, entityFieldFactory.createResource(User.class, "userGroups")));
            group = groups.get(3);
            // admin
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(UserGroup.class)));
//            for (UserGroup gr : groups) {
//                //if (!gr.getName().equals("Admin")) {
//                    permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(gr)));
//                    permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(gr)));
//                //}
//            }

            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource(Document.class)));
            // permissionManager.save(permissionFactory.create(group, grantAction,
            // entityFieldFactory.createResource(Email.class)));
            // permissionManager.save(permissionFactory.create(group, grantAction,
            // entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, grantAction, entityFieldFactory.createResource("Carrier_Party")));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(User.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(UserGroup.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource(Document.class)));
            // permissionManager.save(permissionFactory.create(group, revokeAction,
            // entityFieldFactory.createResource(Email.class)));
            // permissionManager.save(permissionFactory.create(group, revokeAction,
            // entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, revokeAction, entityFieldFactory.createResource("Carrier_Party")));

            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Contact.class)));
            // permissionManager.save(permissionFactory.create(group, readAction,
            // entityFieldFactory.createResource(CarrierGroup.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Document.class)));
            // permissionManager.save(permissionFactory.create(group, readAction,
            // entityFieldFactory.createResource(Email.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Comment.class)));

            // Base
            group = groups.get(4);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Party.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Party.class)));

            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Contact.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Contact.class)));

            group = groups.get(5);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Carrier.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Carrier.class)));

            permissionManager.save(permissionFactory.create(group, addAction, entityFieldFactory.createResource(Carrier.class, "contacts")));
            permissionManager.save(permissionFactory.create(group, removeAction, entityFieldFactory.createResource(Carrier.class, "contacts")));

            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource("Carrier_Party")));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource("Carrier_Party")));

            group = groups.get(6);

            group = groups.get(7);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, deleteAction, entityFieldFactory.createResource(Document.class)));
            permissionManager.save(permissionFactory.create(group, readAction, entityFieldFactory.createResource(Document.class)));

            group = groups.get(8);
            permissionManager.save(permissionFactory.create(group, createAction, entityFieldFactory.createResource(Comment.class)));
            permissionManager.save(permissionFactory.create(group, updateAction, entityFieldFactory.createResource(Comment.class, "user")));

            userGroupService.addUserToGroup(superAdmin, admin, adminGroup, true);
        }
        ChangeInterceptor.activate();
    }

}
