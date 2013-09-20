/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl;

import com.blazebit.security.PermissionFactory;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.context.UserContextMock;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityConstants;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserDataPermission;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserGroupDataPermission;
import com.blazebit.security.impl.model.UserGroupPermission;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.impl.utils.EntityUtils;
import java.io.Serializable;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    protected UserContextMock userContext;
    //users
    protected User admin;
    protected User user1;
    protected User user2;
    //groups
    protected UserGroup userGroupAB;
    protected UserGroup userGroupA;
    protected UserGroup userGroupB;
    protected UserGroup userGroupC;
    //actions
    protected EntityAction createAction;
    protected EntityAction deleteAction;
    protected EntityAction updateAction;
    protected EntityAction grantAction;
    protected EntityAction revokeAction;
    protected EntityAction readAction;
    //entityfields and entityObjectFields
    protected EntityField userEntity;
    protected EntityField groupEntity;
    protected EntityField documentEntity;
    protected EntityField documentEntityTitleField;
    protected EntityField documentEntityContentField;
    protected EntityField emailEntity;
    protected EntityObjectField user1Entity;
    protected EntityObjectField user2Entity;
    protected EntityObjectField document1Entity;
    protected EntityObjectField document1EntityTitleField;
    protected EntityObjectField document1EntityContentField;
    protected EntityObjectField document2Entity;

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
        //create actions
        createAction = ActionUtils.getAction(ActionUtils.ActionConstants.CREATE);
        deleteAction = ActionUtils.getAction(ActionUtils.ActionConstants.DELETE);
        updateAction = ActionUtils.getAction(ActionUtils.ActionConstants.UPDATE);
        grantAction = ActionUtils.getAction(ActionUtils.ActionConstants.GRANT);
        revokeAction = ActionUtils.getAction(ActionUtils.ActionConstants.REVOKE);
        readAction = ActionUtils.getAction(ActionUtils.ActionConstants.READ);
        //create entities
        userEntity = EntityUtils.getEntityFieldFor(User.class, "");
        groupEntity = EntityUtils.getEntityFieldFor(UserGroup.class, "");
        documentEntity = EntityUtils.getEntityFieldFor(EntityConstants.DOCUMENT, "");
        documentEntityTitleField = EntityUtils.getEntityFieldFor(EntityConstants.DOCUMENT, "Title");
        documentEntityContentField = EntityUtils.getEntityFieldFor(EntityConstants.DOCUMENT, "Content");
        emailEntity = EntityUtils.getEntityFieldFor(EntityConstants.EMAIL, "");
        user1Entity = EntityUtils.getEntityObjectFieldFor(user1.getClass(), "", user1.getEntityId());
        user2Entity = EntityUtils.getEntityObjectFieldFor(user2.getClass(), "", user2.getEntityId());
        document1Entity = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "", "1");
        document2Entity = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "", "2");
        document1EntityTitleField = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "Title", "1");
        document1EntityContentField = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "Content", "1");

        admin = new User("Admin");
        entityManager.persist(admin);

        permissionFactory.create(admin, grantAction, userEntity);
        permissionFactory.create(admin, revokeAction, userEntity);
        permissionFactory.create(admin, updateAction, userEntity);

        permissionFactory.create(admin, grantAction, groupEntity);
        permissionFactory.create(admin, revokeAction, groupEntity);
        permissionFactory.create(admin, updateAction, groupEntity);
        //allow admin to create permissions, then activate interceptor <--- not needed now because only update interceptor is activated
        
        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(UserPermission.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(UserDataPermission.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(UserGroupPermission.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(UserGroupDataPermission.class, EntityField.EMPTY_FIELD));
        
        permissionFactory.create(admin, deleteAction, EntityUtils.getEntityFieldFor(UserPermission.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, deleteAction, EntityUtils.getEntityFieldFor(UserDataPermission.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, deleteAction, EntityUtils.getEntityFieldFor(UserGroupPermission.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, deleteAction, EntityUtils.getEntityFieldFor(UserGroupDataPermission.class, EntityField.EMPTY_FIELD));
        
        entityManager.flush();
    }

    @Test
    public void test_user_context() {
        assertNotNull(userContext);
        assertNotNull(userContext.getUser());
        assertNotNull(userContext.getUser().getId());
    }

    protected UserContext getUserContext() {
        return userContext;
    }

    public void setUserContext(User user) {
        userContext.setUser(user);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected void createAdminWithPermissionsForUsers() {
        entityManager.persist(admin);
        permissionFactory.create(admin, grantAction, userEntity);
        permissionFactory.create(admin, revokeAction, userEntity);
        entityManager.flush();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected void createAdminWithPermissionsForUserGroups() {
        admin = new User("Admin");
        entityManager.persist(admin);
        permissionFactory.create(admin, grantAction, groupEntity);
        permissionFactory.create(admin, revokeAction, groupEntity);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected void createAdminWithPermissionsForUsersAndGroups() {
        admin = new User("Admin");
        entityManager.persist(admin);


        permissionFactory.create(admin, grantAction, userEntity);
        permissionFactory.create(admin, revokeAction, userEntity);
        permissionFactory.create(admin, updateAction, userEntity);

        permissionFactory.create(admin, grantAction, groupEntity);
        permissionFactory.create(admin, revokeAction, groupEntity);
        permissionFactory.create(admin, updateAction, groupEntity);


        entityManager.flush();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected void createAdminWithGrantPermissions() {
        admin = new User("Admin");
        entityManager.persist(admin);
        permissionFactory.create(admin, grantAction, userEntity);
        permissionFactory.create(admin, grantAction, groupEntity);
        entityManager.flush();
    }

    protected void createAdminWithRevokePermissions() {
        admin = new User("Admin");
        entityManager.persist(admin);
        permissionFactory.create(admin, revokeAction, userEntity);
        permissionFactory.create(admin, revokeAction, groupEntity);
        entityManager.flush();
    }

    protected void testPermissionSize(User user, int expectedSize) {
        User reloadedUser = (User) entityManager.find(User.class, user.getId());
        assertEquals(expectedSize, reloadedUser.getAllPermissions().size());
    }
}
