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
import com.blazebit.security.RoleSecurityService;
import com.blazebit.security.RoleService;
import com.blazebit.security.SecurityService;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.utils.EntityUtils;
import javax.inject.Inject;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author cuszk
 */
public class RoleServiceTest extends BaseTest {

    @Inject
    private SecurityService securityService;
    @Inject
    private RoleService roleService;
    @Inject
    RoleSecurityService roleSecurityService;
    @Inject
    private PermissionFactory permissionFactory;
    private UserGroup ug1;
    private UserGroup ug11;
    private UserGroup ug12;
    private UserGroup ug13;
    private UserGroup ug111;
    private UserGroup ug112;
    private UserGroup ug121;
    private UserGroup ug1111;
    private UserGroup ug1112;

    @Before
    public void init() throws Exception {
        utx.begin();
        super.initData();

        //admin has grant/revoke action to all user entity
        UserPermission grantPermission = permissionFactory.create(admin, grantAction, userEntity);
        entityManager.persist(grantPermission);
        admin.getPermissions().add(grantPermission);
        admin = (User) entityManager.merge(admin);

        UserPermission revokePermission = permissionFactory.create(admin, revokeAction, userEntity);
        entityManager.persist(revokePermission);
        admin.getPermissions().add(revokePermission);
        admin = (User) entityManager.merge(admin);


        UserPermission grantPermissionGroup = permissionFactory.create(admin, grantAction, groupEntity);
        entityManager.persist(grantPermissionGroup);
        admin.getPermissions().add(grantPermissionGroup);
        admin = (User) entityManager.merge(admin);

        UserPermission revokePermissionGroup = permissionFactory.create(admin, revokeAction, groupEntity);
        entityManager.persist(revokePermissionGroup);

        admin.getPermissions().add(revokePermissionGroup);
        admin = (User) entityManager.merge(admin);

        UserPermission grantToGrant = permissionFactory.create(admin, grantAction, EntityUtils.getEntityFieldFor(grantAction));
        entityManager.persist(grantToGrant);

        admin.getPermissions().add(grantToGrant);
        admin = (User) entityManager.merge(admin);

        entityManager.flush();
        //entityManager.getTransaction().commit();
        utx.commit();

        utx.begin();
        ug1 = new UserGroup("ug1");
        entityManager.persist(ug1);


        ug11 = new UserGroup("ug11");
        ug11.setParent(ug1);
        ug1.getUserGroups().add(ug11);
        entityManager.persist(ug11);


        ug12 = new UserGroup("ug12");
        ug12.setParent(ug1);
        ug1.getUserGroups().add(ug12);
        entityManager.persist(ug12);


        ug13 = new UserGroup("ug13");
        ug13.setParent(ug1);
        ug1.getUserGroups().add(ug13);
        entityManager.persist(ug13);


        ug111 = new UserGroup("ug111");
        ug111.setParent(ug11);
        ug11.getUserGroups().add(ug111);
        entityManager.persist(ug111);

        ug112 = new UserGroup("ug112");
        ug112.setParent(ug11);
        ug11.getUserGroups().add(ug112);
        entityManager.persist(ug112);

        ug121 = new UserGroup("ug121");
        ug121.setParent(ug12);
        ug12.getUserGroups().add(ug121);
        entityManager.persist(ug121);

        ug1111 = new UserGroup("ug1111");
        ug1111.setParent(ug111);
        ug111.getUserGroups().add(ug1111);
        entityManager.persist(ug1111);

        ug1112 = new UserGroup("ug1112");
        ug1112.setParent(ug111);
        ug111.getUserGroups().add(ug1112);
        entityManager.persist(ug1112);

        utx.commit();

    }

    private void addUserToGroup(User user, UserGroup userGroup) throws Exception {
        utx.begin();
        user.getUserGroups().add(userGroup);
        entityManager.merge(user);
        utx.commit();
    }

    //check if user can be added to group
    @Test
    public void test_canUserBeAddedToGroup() throws Exception {
        addUserToGroup(user1, ug1111);
        //user can be readded to the same group
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug1111));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug121));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug12));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug13));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug112));
    }

    @Test
    public void test_canUserBeAddedToGroup1() throws Exception {
        addUserToGroup(user1, ug1112);
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug1));
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug11));
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug111));

    }

    @Test
    public void test_canUserBeAddedToGroup2() throws Exception {
        addUserToGroup(user1, ug12);
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug1));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug13));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug11));
    }

    //add user to groups
    @Test
    public void test_addSubjectToRole1() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(userGroupA, readAction, documentEntity));
        entityManager.persist(permissionFactory.create(userGroupA, accessAction, emailEntity));
        utx.commit();
        roleService.addSubjectToRole(admin, user1, userGroupA, true);
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, emailEntity));

    }

    @Test
    public void test_addSubjectToRole2() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, userEntity));

        entityManager.persist(permissionFactory.create(ug11, accessAction, document1Entity));
        entityManager.persist(permissionFactory.create(ug12, accessAction, document2Entity));
        entityManager.persist(permissionFactory.create(ug13, accessAction, emailEntity));

        entityManager.persist(permissionFactory.create(ug111, accessAction, document1EntityTitleField));
        entityManager.persist(permissionFactory.create(ug112, accessAction, document1EntityContentField));
        entityManager.flush();
        utx.commit();

        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, accessAction, userEntity));
        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        testPermissionSize(user1, 2);

    }

    @Test
    public void test_addSubjectToRole3() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, documentEntity));

        entityManager.persist(permissionFactory.create(ug11, accessAction, document1Entity));
        entityManager.persist(permissionFactory.create(ug12, accessAction, document2Entity));
        entityManager.persist(permissionFactory.create(ug13, accessAction, emailEntity));

        entityManager.persist(permissionFactory.create(ug111, accessAction, document1EntityTitleField));
        entityManager.persist(permissionFactory.create(ug112, accessAction, document1EntityContentField));
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        testPermissionSize(user1, 1);

    }

    @Test
    public void test_addSubjectToRole4() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, document1Entity));
        entityManager.persist(permissionFactory.create(ug1, accessAction, document2Entity));

        entityManager.persist(permissionFactory.create(ug11, accessAction, document1EntityTitleField));
        entityManager.persist(permissionFactory.create(ug11, accessAction, document1EntityContentField));

        entityManager.persist(permissionFactory.create(ug111, accessAction, document2Entity));
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document2Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityContentField));
        testPermissionSize(user1, 2);

    }

    @Test
    public void test_addSubjectToRole5() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, document1Entity));


        entityManager.persist(permissionFactory.create(ug11, accessAction, documentEntityTitleField));
        entityManager.persist(permissionFactory.create(ug11, accessAction, document1EntityContentField));

        entityManager.persist(permissionFactory.create(ug111, accessAction, documentEntity));
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document2Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityContentField));
        testPermissionSize(user1, 1);

    }

    @Test
    public void test_addSubjectToRole6() throws Exception {
        utx.begin();
        //entityManager.persist(permissionFactory.create(ug1, accessAction, document1Entity));

        entityManager.persist(permissionFactory.create(ug11, readAction, documentEntity));
        entityManager.persist(permissionFactory.create(ug11, accessAction, documentEntity));

        entityManager.persist(permissionFactory.create(ug111, accessAction, emailEntity));
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, emailEntity));
        testPermissionSize(user1, 3);

    }

    @Test
    public void test_addSubjectToRole7() throws Exception {
        utx.begin();
        //entityManager.persist(permissionFactory.create(ug1, accessAction, document1Entity));

        entityManager.persist(permissionFactory.create(ug11, readAction, documentEntity));
        entityManager.persist(permissionFactory.create(ug11, accessAction, documentEntity));

        entityManager.persist(permissionFactory.create(ug111, accessAction, emailEntity));
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug111, false);
        testPermissionSize(user1, 0);

    }

    @Test
    public void test_addSubjectToRole8() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, document1Entity));

        entityManager.persist(permissionFactory.create(ug11, readAction, emailEntity));
        entityManager.persist(permissionFactory.create(ug11, accessAction, emailEntity));
        entityManager.persist(permissionFactory.create(ug11, readAction, documentEntity));

        entityManager.persist(permissionFactory.create(ug111, accessAction, document1EntityTitleField));
        utx.commit();
        securityService.grant(admin, user1, readAction, emailEntity);
        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));

        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityContentField));

        assertTrue(securityService.isGranted(user1, readAction, documentEntityContentField));

        assertTrue(securityService.isGranted(user1, readAction, emailEntity));
        assertTrue(securityService.isGranted(user1, accessAction, emailEntity));

        testPermissionSize(user1, 4);

    }

    //permission is added to root group. subject on the lowest level group gets permission too
    @Test
    public void test_grant_to_permission_role_and_it_is_propagated_to_users() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, document2Entity));

        entityManager.persist(permissionFactory.create(ug11, accessAction, document1EntityContentField));

        entityManager.persist(permissionFactory.create(ug111, accessAction, document1EntityTitleField));
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug1111, true);
        roleSecurityService.grant(admin, ug1, accessAction, emailEntity);
        assertTrue(securityService.isGranted(user1, accessAction, emailEntity));
    }

    //permission is added to root group. all added users will have the permission
    @Test
    public void test_grant_to_permission_role_and_it_is_propagated_to_users1() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, document2Entity));

        entityManager.persist(permissionFactory.create(ug11, accessAction, document1EntityContentField));

        entityManager.persist(permissionFactory.create(ug111, accessAction, document1EntityTitleField));
        User user3 = new User("user3");
        entityManager.persist(user3);
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug1111, true);
        roleService.addSubjectToRole(admin, user2, ug121, true);
        roleService.addSubjectToRole(admin, user3, ug13, true);

        roleSecurityService.grant(admin, ug1, accessAction, emailEntity);
        assertTrue(securityService.isGranted(user1, accessAction, emailEntity));
        assertTrue(securityService.isGranted(user2, accessAction, emailEntity));
        assertTrue(securityService.isGranted(user3, accessAction, emailEntity));
    }

    //permission is added to group on the second level. users in second level, 3rd and 4th level get the permission too 
    @Test
    public void test_grant_to_permission_role_and_it_is_propagated_to_users2() throws Exception {
        utx.begin();
        entityManager.persist(permissionFactory.create(ug1, accessAction, document2Entity));

        entityManager.persist(permissionFactory.create(ug11, accessAction, document1EntityContentField));

        entityManager.persist(permissionFactory.create(ug111, accessAction, document1EntityTitleField));
        User user3 = new User("user3");
        entityManager.persist(user3);
        utx.commit();
        roleService.addSubjectToRole(admin, user1, ug1111, true);
        roleService.addSubjectToRole(admin, user2, ug111, true);
        roleService.addSubjectToRole(admin, user3, ug11, true);

        roleSecurityService.grant(admin, ug11, accessAction, emailEntity);
        assertTrue(securityService.isGranted(user1, accessAction, emailEntity));
        assertTrue(securityService.isGranted(user2, accessAction, emailEntity));
        assertTrue(securityService.isGranted(user3, accessAction, emailEntity));
    }
}
