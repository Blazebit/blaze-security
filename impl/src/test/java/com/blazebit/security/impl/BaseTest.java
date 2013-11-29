/*
 * Copyright 2013 Blazebit.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.blazebit.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.blazebit.security.Action;
import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.ResourceFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.context.UserContextMock;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.Company;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.CarrierTeam;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.impl.model.sample.TestCarrier;

/**
 * 
 * @author Christian
 */
@RunWith(TestRunner.class)
public abstract class BaseTest<T extends BaseTest<T>> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    protected Instance<T> self;
    @PersistenceContext(unitName = "TestPU")
    protected EntityManager entityManager;
    @Inject
    protected PermissionFactory permissionFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected EntityResourceFactory entityFieldFactory;
    @Inject
    protected ResourceFactory resourceFactory;
    @Inject
    protected PermissionManager permissionManager;
    @Inject
    private UserContextMock userContext;

    // users
    protected User admin;
    protected User user1;
    protected User user2;
    // groups
    protected UserGroup userGroupA;
    protected UserGroup userGroupB;
    protected UserGroup userGroupC;
    protected UserGroup userGroupD;
    // actions
    protected Action createAction;
    protected Action deleteAction;
    protected Action updateAction;
    protected Action grantAction;
    protected Action revokeAction;
    protected Action readAction;
    // entityfields and entityObjectFields
    protected EntityField userEntity;
    protected EntityField groupEntity;
    protected EntityField documentEntity;
    protected EntityField documentEntityTitleField;
    protected EntityField documentEntityContentField;
    protected EntityField emailEntity;

    protected EntityObjectField user1Entity;
    protected EntityObjectField user2Entity;
    protected EntityObjectField document1Entity;
    protected EntityObjectField document2Entity;
    protected EntityObjectField document1EntityTitleField;
    protected EntityObjectField document1EntityContentField;

    protected final String Title_Field = "title";
    protected final String Content_Field = "content";
    protected final String Subject_Field = "subject";
    protected final String Body_Field = "body";

    protected void initData() {
        ChangeInterceptor.deactivate();
        Company company=new Company();
        company.setUserLevelEnabled(true);
        company.setFieldLevelEnabled(true);
        company.setGroupHierarchyEnabled(true);
        company.setObjectLevelEnabled(true);
        entityManager.persist(company);
        
        user1 = new User("User 1");
        user1.setCompany(company);
        entityManager.persist(user1);

        user2 = new User("User 2");
        user2.setCompany(company);
        entityManager.persist(user2);

        userGroupA = new UserGroup("Usergroup A");
        entityManager.persist(userGroupA);

        userGroupB = new UserGroup("Usergroup B");
        entityManager.persist(userGroupB);

        userGroupC = new UserGroup("Usergroup C");
        entityManager.persist(userGroupC);
        
        userGroupD = new UserGroup("Usergroup D");
        entityManager.persist(userGroupD);

        // create actions
        createAction = actionFactory.createAction(ActionConstants.CREATE);
        deleteAction = actionFactory.createAction(ActionConstants.DELETE);
        updateAction = actionFactory.createAction(ActionConstants.UPDATE);
        grantAction = actionFactory.createAction(ActionConstants.GRANT);
        revokeAction = actionFactory.createAction(ActionConstants.REVOKE);
        readAction = actionFactory.createAction(ActionConstants.READ);
        // create some resources
        userEntity = (EntityField) entityFieldFactory.createResource(User.class);
        groupEntity = (EntityField) entityFieldFactory.createResource(UserGroup.class);
        documentEntity = (EntityField) entityFieldFactory.createResource(Document.class);

        documentEntityTitleField = (EntityField) entityFieldFactory.createResource(Document.class, Title_Field);
        documentEntityContentField = (EntityField) entityFieldFactory.createResource(Document.class, Content_Field);
        emailEntity = (EntityField) entityFieldFactory.createResource(Email.class);
        user1Entity = (EntityObjectField) entityFieldFactory.createResource(user1.getClass(), user1.getId());
        user2Entity = (EntityObjectField) entityFieldFactory.createResource(user2.getClass(), user2.getId());
        document1Entity = (EntityObjectField) entityFieldFactory.createResource(Document.class, 1);
        document2Entity = (EntityObjectField) entityFieldFactory.createResource(Document.class, 2);
        document1EntityTitleField = (EntityObjectField) entityFieldFactory.createResource(Document.class, Title_Field, 1);
        document1EntityContentField = (EntityObjectField) entityFieldFactory.createResource(Document.class, Content_Field, 1);
        // create admin
        admin = new User("Admin");
        admin.setCompany(company);
        entityManager.persist(admin);
        // add permissions to admin
        // admin can grant to users and usergroups
        permissionManager.save(permissionFactory.create(admin, grantAction, userEntity));
        permissionManager.save(permissionFactory.create(admin, revokeAction, userEntity));
        // admin can change user properties
        permissionManager.save(permissionFactory.create(admin, updateAction, userEntity));

        permissionManager.save(permissionFactory.create(admin, grantAction, groupEntity));
        permissionManager.save(permissionFactory.create(admin, revokeAction, groupEntity));
        // admin can change group properties
        permissionManager.save(permissionFactory.create(admin, updateAction, groupEntity));

        // TODO not needed anymore because isGranted service method does not check action resource
        // permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(grantAction)));
        // permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(revokeAction)));
        // permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(grantAction)));
        // permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(revokeAction)));
        //
        // permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(deleteAction)));
        // permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(createAction)));
        // permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(updateAction)));
        // permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(readAction)));
        //
        // permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(deleteAction)));
        // permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(createAction)));
        // permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(updateAction)));
        // permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(readAction)));

        //admin change user_level, field_level, etc
        permissionManager.save(permissionFactory.create(admin, updateAction, entityFieldFactory.createResource(Company.class)));
        // admin can grant the sample entities
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(TestCarrier.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Carrier.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Party.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Contact.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(CarrierGroup.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(CarrierTeam.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Document.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Email.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityFieldFactory.createResource(Comment.class)));
        // admin can revoke the sample entities
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Carrier.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Party.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Contact.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(CarrierGroup.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(CarrierTeam.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Document.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Email.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityFieldFactory.createResource(Comment.class)));

        entityManager.flush();
    }

    @Test
    public void test_user_context() {
        assertNotNull(userContext);
        assertNotNull(userContext.getUser());
        assertNotNull(userContext.getUser().getId());
    }

    public void setUserContext(User user) {
        userContext.setUser(user);
    }

    protected void testPermissionSize(User user, int expectedSize) {
        if (expectedSize != permissionManager.getPermissions(user).size()) {
            for (Permission p : permissionManager.getPermissions(user)) {
                System.out.println("Permission: " + p);
            }
        }
        assertEquals(expectedSize, permissionManager.getPermissions(user).size());
    }

    public Action getUpdateAction() {
        return actionFactory.createAction(ActionConstants.UPDATE);
    }

    public Action getCreateAction() {
        return actionFactory.createAction(ActionConstants.CREATE);
    }

    public Action getAddAction() {
        return actionFactory.createAction(ActionConstants.ADD);
    }

    public Action getRemoveAction() {
        return actionFactory.createAction(ActionConstants.REMOVE);
    }

    public Action getDeleteAction() {
        return actionFactory.createAction(ActionConstants.DELETE);
    }
    
    public Action getReadAction() {
        return actionFactory.createAction(ActionConstants.READ);
    }
    
    public Action getGrantAction() {
        return actionFactory.createAction(ActionConstants.GRANT);
    }
    
    public Action getRevokeAction() {
        return actionFactory.createAction(ActionConstants.REVOKE);
    }
}
