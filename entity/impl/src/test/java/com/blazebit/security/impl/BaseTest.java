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

import static org.junit.Assert.*;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import com.blazebit.security.data.PermissionManager;
import com.blazebit.security.entity.EntityDataResource;
import com.blazebit.security.entity.EntityFeatures;
import com.blazebit.security.entity.EntityResource;
import com.blazebit.security.entity.EntityResourceFactory;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.model.Action;
import com.blazebit.security.model.Company;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.User;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.model.sample.Carrier;
import com.blazebit.security.model.sample.CarrierContactEntry;
import com.blazebit.security.model.sample.CarrierGroup;
import com.blazebit.security.model.sample.CarrierTeam;
import com.blazebit.security.model.sample.Comment;
import com.blazebit.security.model.sample.Contact;
import com.blazebit.security.model.sample.Document;
import com.blazebit.security.model.sample.Email;
import com.blazebit.security.model.sample.Party;
import com.blazebit.security.model.sample.TestCarrier;
import com.blazebit.security.spi.ActionFactory;
import com.blazebit.security.spi.PermissionFactory;
import com.blazebit.security.spi.ResourceFactory;
import com.blazebit.security.test.AbstractContainerTest;

/**
 * 
 * @author Christian
 */
public abstract class BaseTest<T extends BaseTest<T>> extends AbstractContainerTest<T> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    protected DefaultUserContext userContext;
    @Inject
    protected PermissionFactory permissionFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected EntityResourceFactory entityResourceFactory;
    @Inject
    protected ResourceFactory resourceFactory;
    @Inject
    protected PermissionManager permissionManager;

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
    protected EntityResource userEntity;
    protected EntityResource groupEntity;
    protected EntityResource documentEntity;
    protected EntityResource documentEntityTitleField;
    protected EntityResource documentEntityContentField;
    protected EntityResource emailEntity;

    protected EntityDataResource user1Entity;
    protected EntityDataResource user2Entity;
    protected EntityDataResource document1Entity;
    protected EntityDataResource document2Entity;
    protected EntityDataResource document1EntityTitleField;
    protected EntityDataResource document1EntityContentField;

    protected final String Title_Field = "title";
    protected final String Content_Field = "content";
    protected final String Subject_Field = "subject";
    protected final String Body_Field = "body";

    protected void initData() {
        EntityFeatures.deactivateInterceptor();
        Company company=new Company();
        company.setUserLevelEnabled(true);
        company.setFieldLevelEnabled(true);
        company.setGroupHierarchyEnabled(true);
        company.setObjectLevelEnabled(true);
        persist(company);
        
        user1 = new User("User 1");
        user1.setCompany(company);
        persist(user1);

        user2 = new User("User 2");
        user2.setCompany(company);
        persist(user2);

        userGroupA = new UserGroup("Usergroup A");
        persist(userGroupA);

        userGroupB = new UserGroup("Usergroup B");
        persist(userGroupB);

        userGroupC = new UserGroup("Usergroup C");
        persist(userGroupC);
        
        userGroupD = new UserGroup("Usergroup D");
        persist(userGroupD);

        // create actions
        createAction = actionFactory.createAction(Action.CREATE);
        deleteAction = actionFactory.createAction(Action.DELETE);
        updateAction = actionFactory.createAction(Action.UPDATE);
        grantAction = actionFactory.createAction(Action.GRANT);
        revokeAction = actionFactory.createAction(Action.REVOKE);
        readAction = actionFactory.createAction(Action.READ);
        // create some resources
        userEntity = (EntityResource) entityResourceFactory.createResource(User.class);
        groupEntity = (EntityResource) entityResourceFactory.createResource(UserGroup.class);
        documentEntity = (EntityResource) entityResourceFactory.createResource(Document.class);

        documentEntityTitleField = (EntityResource) entityResourceFactory.createResource(Document.class, Title_Field);
        documentEntityContentField = (EntityResource) entityResourceFactory.createResource(Document.class, Content_Field);
        emailEntity = (EntityResource) entityResourceFactory.createResource(Email.class);
        user1Entity = (EntityDataResource) entityResourceFactory.createResource(user1.getClass(), user1.getId());
        user2Entity = (EntityDataResource) entityResourceFactory.createResource(user2.getClass(), user2.getId());
        document1Entity = (EntityDataResource) entityResourceFactory.createResource(Document.class, 1);
        document2Entity = (EntityDataResource) entityResourceFactory.createResource(Document.class, 2);
        document1EntityTitleField = (EntityDataResource) entityResourceFactory.createResource(Document.class, Title_Field, 1);
        document1EntityContentField = (EntityDataResource) entityResourceFactory.createResource(Document.class, Content_Field, 1);
        // create admin
        admin = new User("Admin");
        admin.setCompany(company);
        persist(admin);
        // add permissions to admin

        // admin can grant and revoke any action
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getAddAction())));
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getCreateAction())));
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getDeleteAction())));
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getGrantAction())));
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getReadAction())));
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getRemoveAction())));
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getRevokeAction())));
        permissionManager.save(permissionFactory.create(admin, grantAction, resourceFactory.createResource(getUpdateAction())));

        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getAddAction())));
        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getCreateAction())));
        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getDeleteAction())));
        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getGrantAction())));
        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getReadAction())));
        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getRemoveAction())));
        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getRevokeAction())));
        permissionManager.save(permissionFactory.create(admin, revokeAction, resourceFactory.createResource(getUpdateAction())));
        
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
        permissionManager.save(permissionFactory.create(admin, updateAction, entityResourceFactory.createResource(Company.class)));
        // admin can grant the sample entities
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(TestCarrier.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(Carrier.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(Party.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(Contact.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(CarrierGroup.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(CarrierTeam.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(CarrierContactEntry.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(Document.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(Email.class)));
        permissionManager.save(permissionFactory.create(admin, grantAction, entityResourceFactory.createResource(Comment.class)));
        // admin can revoke the sample entities
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(Carrier.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(Party.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(Contact.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(CarrierGroup.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(CarrierTeam.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(Document.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(Email.class)));
        permissionManager.save(permissionFactory.create(admin, revokeAction, entityResourceFactory.createResource(Comment.class)));
    }

    @Test
    public void test_user_context() {
        assertNotNull(userContext);
        assertNotNull(userContext.getUser());
        assertNotNull(userContext.getUser().getName());
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

    protected static void assertSetsEquals(Set<Permission> expected, Set<Permission> actual) {
        assertEquals("Size is not equal", expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
    }

    protected static void assertSetsNotEquals(Set<Permission> expected, Set<Permission> actual) {
        if (expected.containsAll(actual)) {
            assertFalse(actual.containsAll(expected));
        }
    }

    public Action getUpdateAction() {
        return actionFactory.createAction(Action.UPDATE);
    }

    public Action getCreateAction() {
        return actionFactory.createAction(Action.CREATE);
    }

    public Action getAddAction() {
        return actionFactory.createAction(Action.ADD);
    }

    public Action getRemoveAction() {
        return actionFactory.createAction(Action.REMOVE);
    }

    public Action getDeleteAction() {
        return actionFactory.createAction(Action.DELETE);
    }
    
    public Action getReadAction() {
        return actionFactory.createAction(Action.READ);
    }
    
    public Action getGrantAction() {
        return actionFactory.createAction(Action.GRANT);
    }
    
    public Action getRevokeAction() {
        return actionFactory.createAction(Action.REVOKE);
    }
}
