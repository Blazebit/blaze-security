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

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityConstants;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.impl.utils.EntityUtils;
import java.io.Serializable;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import static org.junit.Assert.assertEquals;
import org.junit.runner.RunWith;

/**
 *
 * @author Christian
 */
@RunWith(TestRunner.class)
public abstract class BaseTest implements Serializable {

    @PersistenceContext
    protected EntityManager entityManager;
    @Resource
    protected UserTransaction utx;
    @Inject
    protected PermissionFactory permissionFactory;
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
    protected EntityAction grantAction;
    protected EntityAction revokeAction;
    protected EntityAction accessAction;
    protected EntityAction readAction;
    protected EntityAction writeAction;
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

    protected void initData() throws Exception {
        utx.begin();

        //create individual entity object



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
        grantAction = ActionUtils.getGrantAction();
        revokeAction = ActionUtils.getRevokeAction();
        accessAction = ActionUtils.getAccessAction();
        readAction = ActionUtils.getReadAction();
        writeAction = ActionUtils.getWriteAction();
        //create entities
        userEntity = EntityUtils.getEntityFieldFor(User.class);
        groupEntity = EntityUtils.getEntityFieldFor(UserGroup.class);
        documentEntity = EntityUtils.getEntityFieldFor(EntityConstants.DOCUMENT);
        documentEntityTitleField = EntityUtils.getEntityFieldFor(EntityConstants.DOCUMENT, "Title");
        documentEntityContentField = EntityUtils.getEntityFieldFor(EntityConstants.DOCUMENT, "Content");
        emailEntity = EntityUtils.getEntityFieldFor(EntityConstants.EMAIL);
        user1Entity = EntityUtils.getEntityObjectFieldFor(user1);
        user2Entity = EntityUtils.getEntityObjectFieldFor(user2);
        document1Entity = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "1");
        document2Entity = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "2");
        document1EntityTitleField = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "Title", "1");
        document1EntityContentField = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "Content", "1");
        entityManager.flush();
        utx.commit();

        createAdminWithPermissionsForUsersAndGroups();
    }

    protected void testPermissionSize(User user, int expectedSize) throws Exception {
        utx.begin();
        user1 = entityManager.find(User.class, user.getId());
        assertEquals(expectedSize, user1.getAllPermissions().size());
        utx.commit();
    }

    protected void createAdminWithPermissionsForUsers() throws Exception {
        utx.begin();
         if (admin != null) {
            admin = entityManager.find(User.class, admin.getId());
            for (Permission p : admin.getAllPermissions()) {
                entityManager.remove(p);
            }
            entityManager.remove(admin);
            entityManager.flush();
        }
        admin = new User("Admin");
        entityManager.persist(admin);
        entityManager.persist(permissionFactory.create(admin, grantAction, userEntity));
        entityManager.persist(permissionFactory.create(admin, revokeAction, userEntity));
        utx.commit();
    }

    protected void createAdminWithPermissionsForUserGroups() throws Exception {
        utx.begin();
        if (admin != null) {
            admin = entityManager.find(User.class, admin.getId());
            for (Permission p : admin.getAllPermissions()) {
                entityManager.remove(p);
            }
            entityManager.remove(admin);
            entityManager.flush();
        }
        admin = new User("Admin");
        entityManager.persist(admin);
        entityManager.persist(permissionFactory.create(admin, grantAction, groupEntity));
        entityManager.persist(permissionFactory.create(admin, revokeAction, groupEntity));
        utx.commit();
    }

    protected void createAdminWithPermissionsForUsersAndGroups() throws Exception {
        utx.begin();
         if (admin != null) {
            admin = entityManager.find(User.class, admin.getId());
            for (Permission p : admin.getAllPermissions()) {
                entityManager.remove(p);
            }
            entityManager.remove(admin);
            entityManager.flush();
        }
        admin = new User("Admin");
        entityManager.persist(admin);
        entityManager.persist(permissionFactory.create(admin, grantAction, userEntity));
        entityManager.persist(permissionFactory.create(admin, revokeAction, userEntity));
        entityManager.persist(permissionFactory.create(admin, grantAction, groupEntity));
        entityManager.persist(permissionFactory.create(admin, revokeAction, groupEntity));
        utx.commit();
    }

    protected void createAdminWithGrantPermissions() throws Exception {
        utx.begin();
        if (admin != null) {
            admin = entityManager.find(User.class, admin.getId());
            for (Permission p : admin.getAllPermissions()) {
                entityManager.remove(p);
            }
            entityManager.remove(admin);
            entityManager.flush();
        }
        admin = new User("Admin");
        entityManager.persist(admin);
        entityManager.persist(permissionFactory.create(admin, grantAction, userEntity));
        entityManager.persist(permissionFactory.create(admin, grantAction, groupEntity));
        utx.commit();

    }

    protected void createAdminWithRevokePermissions() throws Exception {
        utx.begin();
        if (admin != null) {
            admin = entityManager.find(User.class, admin.getId());
            for (Permission p : admin.getAllPermissions()) {
                entityManager.remove(p);
            }
            entityManager.remove(admin);
            entityManager.flush();
        }
        admin = new User("Admin");
        entityManager.persist(admin);
        entityManager.persist(permissionFactory.create(admin, revokeAction, userEntity));
        entityManager.persist(permissionFactory.create(admin, revokeAction, groupEntity));
        utx.commit();

    }
}
