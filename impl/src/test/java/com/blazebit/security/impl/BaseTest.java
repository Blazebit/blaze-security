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

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityFieldFactory;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.context.UserContextMock;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
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

/**
 * 
 * @author Christian
 */
@RunWith(TestRunner.class)
public abstract class BaseTest<T extends BaseTest<T>> implements Serializable {

    @Inject
    protected Instance<T> self;
    @PersistenceContext
    protected EntityManager entityManager;
    @Inject
    protected PermissionFactory permissionFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected EntityFieldFactory entityFieldFactory;
    @Inject
    private PermissionManager permissionManager;
    @Inject
    private UserContextMock userContext;

    // users
    protected User admin;
    protected User user1;
    protected User user2;
    // groups
    protected UserGroup userGroupAB;
    protected UserGroup userGroupA;
    protected UserGroup userGroupB;
    protected UserGroup userGroupC;
    // actions
    protected EntityAction createAction;
    protected EntityAction deleteAction;
    protected EntityAction updateAction;
    protected EntityAction grantAction;
    protected EntityAction revokeAction;
    protected EntityAction readAction;
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
        user1 = new User("User 1");
        entityManager.persist(user1);

        user2 = new User("User 2");
        entityManager.persist(user2);

        userGroupAB = new UserGroup("Usergroup AB");
        entityManager.persist(userGroupAB);

        userGroupA = new UserGroup("Usergroup A");
        entityManager.persist(userGroupA);

        userGroupB = new UserGroup("Usergroup B");
        entityManager.persist(userGroupB);

        userGroupC = new UserGroup("Usergroup C");
        entityManager.persist(userGroupC);
        // create actions
        createAction = actionFactory.createAction(ActionConstants.CREATE);
        deleteAction = actionFactory.createAction(ActionConstants.DELETE);
        updateAction = actionFactory.createAction(ActionConstants.UPDATE);
        grantAction = actionFactory.createAction(ActionConstants.GRANT);
        revokeAction = actionFactory.createAction(ActionConstants.REVOKE);
        readAction = actionFactory.createAction(ActionConstants.READ);
        // create entities
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

        admin = new User("Admin");
        entityManager.persist(admin);

        entityManager.persist(permissionFactory.create(admin, grantAction, userEntity));
        entityManager.persist(permissionFactory.create(admin, revokeAction, userEntity));
        entityManager.persist(permissionFactory.create(admin, updateAction, userEntity));

        entityManager.persist(permissionFactory.create(admin, grantAction, groupEntity));
        entityManager.persist(permissionFactory.create(admin, revokeAction, groupEntity));
        entityManager.persist(permissionFactory.create(admin, updateAction, groupEntity));
        // allow admin to create permissions, then activate interceptor <--- not needed now because only update interceptor is
        // activated

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
        assertEquals(expectedSize, permissionManager.getAllPermissions(user).size());
    }
}
