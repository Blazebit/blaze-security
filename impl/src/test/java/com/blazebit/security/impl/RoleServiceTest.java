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

import com.blazebit.security.PermissionActionException;
import com.blazebit.security.RoleSecurityService;
import com.blazebit.security.RoleService;
import com.blazebit.security.SecurityService;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class RoleServiceTest extends BaseTest<RoleServiceTest> {

    @Inject
    private SecurityService securityService;
    @Inject
    private RoleService roleService;
    @Inject
    private RoleSecurityService roleSecurityService;
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
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        super.initData();

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
        
        setUserContext(admin);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persist(Object object) {
        entityManager.persist(object);
        entityManager.flush();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Object merge(Object object) {
        object = entityManager.merge(object);
        entityManager.flush();
        return object;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addUserToGroup(User user, UserGroup userGroup) {
        userGroup.getUsers().add(user);
        self.get().merge(userGroup);
    }

    //check if user can be added to group
    //user can be added to groups which are not only the same groups that the user already got permissions from
    @Test
    public void test_canUserBeAddedToGroup() {
        self.get().addUserToGroup(user1, ug1111);
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug121));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug12));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug13));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug112));
    }

    //user already added to lowest level group. cannot be added to any of the parent's of the lowest group
    @Test
    public void test_canUserBeAddedToGroup1() {
        self.get().addUserToGroup(user1, ug1112);
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug1));
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug11));
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug111));
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug1112));

    }

    //user added to second level groups -> user cannot be added to the root, but it can be added to any children of the groups he already belongs to
    @Test
    public void test_canUserBeAddedToGroup2() {
        self.get().addUserToGroup(user1, ug11);
        self.get().addUserToGroup(user1, ug12);
        self.get().addUserToGroup(user1, ug13);
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug1));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug111));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug112));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug121));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug1111));
    }

    //user added to second level group -> user can be added to other second level groups, but not the root
    @Test
    public void test_canUserBeAddedToGroup3() {
        self.get().addUserToGroup(user1, ug12);
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug1));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug13));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug11));
    }

    //user can be added to other root group
    @Test
    public void test_canUserBeAddedToGroup4() {
        UserGroup ug2 = new UserGroup("ug2");
        self.get().persist(ug2);

        self.get().addUserToGroup(user1, ug11);
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug11));
        assertFalse(roleService.canSubjectBeAddedToRole(user1, ug1));
        assertTrue(roleService.canSubjectBeAddedToRole(user1, ug2));
    }

    //add user to groups
    //user gets the permissions from the group which he is added to
    @Test
    public void test_addSubjectToRole1() {

        permissionFactory.create(userGroupA, createAction, documentEntity);
        permissionFactory.create(userGroupA, readAction, emailEntity);

        roleService.addSubjectToRole(admin, user1, userGroupA, true);
        assertTrue(securityService.isGranted(user1, createAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, emailEntity));

    }

    //user is added to lowest level group, gets all the permissions "merged" from parent groups
    @Test
    public void test_addSubjectToRole2() {
        permissionFactory.create(ug1, readAction, userEntity);

        permissionFactory.create(ug11, readAction, document1Entity);
        permissionFactory.create(ug12, readAction, document2Entity);
        permissionFactory.create(ug13, readAction, emailEntity);

        permissionFactory.create(ug111, readAction, document1EntityTitleField);
        permissionFactory.create(ug112, readAction, document1EntityContentField);

        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, readAction, userEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
        testPermissionSize(user1, 2);

    }

    //user is added to lowest level group, gets all the permissions "merged" from parent groups
    @Test
    public void test_addSubjectToRole3() {

        permissionFactory.create(ug1, readAction, documentEntity);

        permissionFactory.create(ug11, readAction, document1Entity);
        permissionFactory.create(ug12, readAction, document2Entity);
        permissionFactory.create(ug13, readAction, emailEntity);

        permissionFactory.create(ug111, readAction, document1EntityTitleField);
        permissionFactory.create(ug112, readAction, document1EntityContentField);

        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
        testPermissionSize(user1, 1);

    }

    //user is added to lowest level group, gets all the permissions "merged" from parent groups
    @Test
    public void test_addSubjectToRole4() {
        permissionFactory.create(ug1, readAction, document1Entity);
        permissionFactory.create(ug1, readAction, document2Entity);

        permissionFactory.create(ug11, readAction, document1EntityTitleField);
        permissionFactory.create(ug11, readAction, document1EntityContentField);

        permissionFactory.create(ug111, readAction, document2Entity);

        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, document2Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityContentField));
        testPermissionSize(user1, 2);

    }

    //user is added to lowest level group, gets all the permissions "merged" from parent groups
    @Test
    public void test_addSubjectToRole5() {
        permissionFactory.create(ug1, readAction, document1Entity);
        permissionFactory.create(ug11, readAction, documentEntityTitleField);
        permissionFactory.create(ug11, readAction, document1EntityContentField);

        permissionFactory.create(ug111, readAction, documentEntity);

        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, document2Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityContentField));
        testPermissionSize(user1, 1);

    }

    @Test
    public void test_addSubjectToRole6() {
        //permissionFactory.create(ug1, readAction, document1Entity));

        permissionFactory.create(ug11, createAction, documentEntity);
        permissionFactory.create(ug11, readAction, documentEntity);

        permissionFactory.create(ug111, readAction, emailEntity);

        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, createAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, emailEntity));
        testPermissionSize(user1, 3);

    }

    @Test
    public void test_addSubjectToRole7() {
        //permissionFactory.create(ug1, readAction, document1Entity));

        permissionFactory.create(ug11, createAction, documentEntity);
        permissionFactory.create(ug11, readAction, documentEntity);

        permissionFactory.create(ug111, readAction, emailEntity);

        roleService.addSubjectToRole(admin, user1, ug111, false);
        testPermissionSize(user1, 0);

    }

    @Test
    public void test_addSubjectToRole8() {
        permissionFactory.create(ug1, readAction, document1Entity);

        permissionFactory.create(ug11, createAction, emailEntity);
        permissionFactory.create(ug11, readAction, emailEntity);
        permissionFactory.create(ug11, createAction, documentEntity);

        permissionFactory.create(ug111, readAction, document1EntityTitleField);

        securityService.grant(admin, user1, createAction, emailEntity);
        roleService.addSubjectToRole(admin, user1, ug111, true);
        assertTrue(securityService.isGranted(user1, createAction, documentEntity));

        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityContentField));

        assertTrue(securityService.isGranted(user1, createAction, documentEntityContentField));

        assertTrue(securityService.isGranted(user1, createAction, emailEntity));
        assertTrue(securityService.isGranted(user1, readAction, emailEntity));

        testPermissionSize(user1, 4);

    }

    //permission is added to root group. subject on the lowest level group gets permission too
    @Test
    public void test_grant_to_permission_role_and_it_is_propagated_to_users() {
        permissionFactory.create(ug1, readAction, document2Entity);

        permissionFactory.create(ug11, readAction, document1EntityContentField);

        permissionFactory.create(ug111, readAction, document1EntityTitleField);

        roleService.addSubjectToRole(admin, user1, ug1111, true);
        roleSecurityService.grant(admin, ug1, readAction, emailEntity);
        assertTrue(securityService.isGranted(user1, readAction, emailEntity));
    }

    //permission is added to root group. all added users will have the permission
    @Test
    public void test_grant_to_permission_role_and_it_is_propagated_to_users1() {
        permissionFactory.create(ug1, readAction, document2Entity);

        permissionFactory.create(ug11, readAction, document1EntityContentField);

        permissionFactory.create(ug111, readAction, document1EntityTitleField);
        User user3 = new User("user3");
        self.get().persist(user3);

        roleService.addSubjectToRole(admin, user1, ug1111, true);
        roleService.addSubjectToRole(admin, user2, ug121, true);
        roleService.addSubjectToRole(admin, user3, ug13, true);

        roleSecurityService.grant(admin, ug1, readAction, emailEntity);
        assertTrue(securityService.isGranted(user1, readAction, emailEntity));
        assertTrue(securityService.isGranted(user2, readAction, emailEntity));
        assertTrue(securityService.isGranted(user3, readAction, emailEntity));
    }

    //permission is added to group on the second level. users in second level, 3rd and 4th level get the permission too 
    @Test
    public void test_grant_to_permission_role_and_it_is_propagated_to_users2() {

        permissionFactory.create(ug1, readAction, document2Entity);

        permissionFactory.create(ug11, readAction, document1EntityContentField);

        permissionFactory.create(ug111, readAction, document1EntityTitleField);
        User user3 = new User("user3");
        self.get().persist(user3);

        roleService.addSubjectToRole(admin, user1, ug1111, true);
        roleService.addSubjectToRole(admin, user2, ug111, true);
        roleService.addSubjectToRole(admin, user3, ug11, true);

        roleSecurityService.grant(admin, ug11, readAction, emailEntity);
        assertTrue(securityService.isGranted(user1, readAction, emailEntity));
        assertTrue(securityService.isGranted(user2, readAction, emailEntity));
        assertTrue(securityService.isGranted(user3, readAction, emailEntity));
    }

    //remove subject from role
    //ug1111->ug111->ug11->ug1
    @Test
    public void test_removeSubjectFromRole() {

        permissionFactory.create(ug1, readAction, document2Entity);
        permissionFactory.create(ug1, readAction, document1Entity);
        permissionFactory.create(ug11, readAction, document1EntityContentField);
        permissionFactory.create(ug111, readAction, document1EntityTitleField);

        roleService.addSubjectToRole(admin, user1, ug1111, true);

        assertTrue(securityService.isGranted(user1, readAction, document2Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));

        roleService.removeSubjectFromRole(admin, user1, ug1111, true);

        assertFalse(securityService.isGranted(user1, readAction, document2Entity));
        assertFalse(securityService.isGranted(user1, readAction, document1Entity));
    }

    @Test(expected = PermissionActionException.class)
    public void test_removeSubjectFromRole_not_possible() {
        roleService.addSubjectToRole(admin, user1, ug1111, true);
        roleService.removeSubjectFromRole(admin, user1, ug111, true);
    }

    @Test
    public void test_removeSubjectFromRole2() {

        permissionFactory.create(ug1, readAction, documentEntity);
        
        permissionFactory.create(ug11, readAction, document1Entity);
        
        permissionFactory.create(ug111, readAction, document1EntityTitleField);
        
        permissionFactory.create(ug1111, readAction, document2Entity);

        roleService.addSubjectToRole(admin, user1, ug1111, true);

        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document2Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));

        roleService.removeSubjectFromRole(admin, user1, ug1111, true);

        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
        assertFalse(securityService.isGranted(user1, readAction, document2Entity));
        assertFalse(securityService.isGranted(user1, readAction, document1Entity));
        assertFalse(securityService.isGranted(user1, readAction, document1EntityTitleField));
        assertFalse(securityService.isGranted(user1, readAction, document1EntityContentField));
    }
}
