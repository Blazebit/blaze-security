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

import com.blazebit.security.service.RoleService;
import com.blazebit.security.SecurityActionException;
import com.blazebit.security.service.SecurityService;
import com.blazebit.security.impl.model.CarrierModule;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.utils.EntityUtils;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.service.RoleSecurityService;
import java.util.Collection;
import java.util.Iterator;
import javax.inject.Inject;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author cuszk
 */
public class SecurityServiceTest extends BaseTest {

    @Inject
    private SecurityService securityService;
    @Inject
    private RoleService roleService;
    @Inject
    private RoleSecurityService roleSecurityService;

    @Before
    public void init() throws Exception {
        super.initData();
    }

    @Test
    public void test_initial_data() {
        //injections
        assertNotNull(roleService);
        assertNotNull(securityService);
        //entityManager
        assertNotNull(entityManager);
        //entities
        assertNotNull(admin);
        assertNotNull(user1);
        assertNotNull(user2);
        assertNotNull(userGroupA);
        assertNotNull(userGroupB);
    }

    @Test
    public void test_admin_has_grant_and_revoke_action_for_users_and_groups() throws Exception {
        assertTrue(securityService.isGranted(admin, grantAction, userEntity));
        assertTrue(securityService.isGranted(admin, revokeAction, userEntity));
        assertTrue(securityService.isGranted(admin, grantAction, groupEntity));
        assertTrue(securityService.isGranted(admin, revokeAction, groupEntity));
    }
    //grant checks

    @Test
    public void test_admin_grants_action() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
    }

    @Test(expected = com.blazebit.security.SecurityException.class)
    public void test_admin2_has_no_grant_action() throws Exception {
        utx.begin();
        User admin2 = new User();
        admin2.setUsername("admin2");
        entityManager.persist(admin2);
        utx.commit();
        securityService.grant(admin2, user1, accessAction, documentEntity);
    }

    @Test
    public void test_admin_grants_different_entities() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user1, accessAction, emailEntity);

        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));

        assertTrue(securityService.isGranted(user1, accessAction, emailEntity));
    }

    @Test
    public void test_admin_grants_different_actions() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user1, readAction, documentEntity);

        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));

        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));

    }

    @Test
    public void test_admin_grants_A_f_to_A_different_actions() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
    }

    @Test
    public void test_admin_grants_A_f_and_A_g() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntityTitleField);
        securityService.grant(admin, user1, accessAction, documentEntityContentField);
        testPermissionSize(user1, 2);
    }

    @Test
    public void test_admin_grants_A_f_to_A_different_subjects() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user2, accessAction, documentEntityTitleField);
    }
    //grant checks with error

    @Test(expected = SecurityActionException.class)
    public void test_admin_grants_A_to_A() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user1, accessAction, documentEntity);
    }

    @Test(expected = SecurityActionException.class)
    public void test_admin_grants_A_f_to_A() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user1, accessAction, documentEntityTitleField);
    }

    @Test(expected = SecurityActionException.class)
    public void test_admin_grants_A_i_to_A() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user1, accessAction, document1Entity);
    }

    @Test(expected = SecurityActionException.class)
    public void test_admin_grants_A_f_i_to_A() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
    }

    //grant checks when existing permissions need to be removed
    @Test
    public void test_admin_grants_A_to_A_f() throws Exception {
        //user gets access to 1 field of document entity
        securityService.grant(admin, user1, accessAction, documentEntityTitleField);
        //user gets access to document entity
        securityService.grant(admin, user1, accessAction, documentEntity);
        //user remains with only document entity permission
        testPermissionSize(user1, 1);
        //user can access document entity
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        //user can access any field of document entity
        assertTrue(securityService.isGranted(user1, accessAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, documentEntityContentField));
    }

    @Test
    public void test_admin_grants_A_to_A_f_and_A_g() throws Exception {
        //user gets access to 2 fields of document entity
        securityService.grant(admin, user1, accessAction, documentEntityTitleField);
        securityService.grant(admin, user1, accessAction, documentEntityContentField);
        testPermissionSize(user1, 2);
        //user gets access to document entity
        securityService.grant(admin, user1, accessAction, documentEntity);
        //user remains with only document entity permission
        testPermissionSize(user1, 1);
        //user can access document entity
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        //user can access any field of document entity
        assertTrue(securityService.isGranted(user1, accessAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, documentEntityContentField));
    }

    @Test
    public void test_admin_grants_A_i_to_A_f_i() throws Exception {
        //user gets access to 1 field of document object with id 1
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        //user gets access to document object with id 1
        securityService.grant(admin, user1, accessAction, document1Entity);
        //user remains with only document object permission
        testPermissionSize(user1, 1);
        //user can access any field of document object
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_A_i_to_A_f_i_and_A_g_i() throws Exception {
        //user gets access to 2 fields of document object with id 1
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        securityService.grant(admin, user1, accessAction, document1EntityContentField);
        testPermissionSize(user1, 2);
        //user gets access to document object with id 1
        securityService.grant(admin, user1, accessAction, document1Entity);
        //user remains with only document object permission
        testPermissionSize(user1, 1);
        //user can access any field of document object
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_A_to_A_i() throws Exception {
        securityService.grant(admin, user1, accessAction, document1Entity);
        securityService.grant(admin, user1, accessAction, documentEntity);

        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_i_and_A_j() throws Exception {
        securityService.grant(admin, user1, accessAction, document1Entity);
        securityService.grant(admin, user1, accessAction, document2Entity);
        testPermissionSize(user1, 2);
        securityService.grant(admin, user1, accessAction, documentEntity);
        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_f_i() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        securityService.grant(admin, user1, accessAction, documentEntity);

        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_f_i_and_A_g_i() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        securityService.grant(admin, user1, accessAction, document1EntityContentField);
        securityService.grant(admin, user1, accessAction, documentEntity);
        testPermissionSize(user1, 1);
    }

    //revoke checks
    @Test
    public void test_admin_revokes_action() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        securityService.revoke(admin, user1, accessAction, documentEntity);
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_f() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntityTitleField);
        securityService.revoke(admin, user1, accessAction, documentEntity);
        assertFalse(securityService.isGranted(user1, accessAction, documentEntityTitleField));
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_f_and_A_g() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntityTitleField);
        securityService.grant(admin, user1, accessAction, documentEntityContentField);
        testPermissionSize(user1, 2);
        securityService.revoke(admin, user1, accessAction, documentEntity);
        assertFalse(securityService.isGranted(user1, accessAction, documentEntityTitleField));
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
        testPermissionSize(user1, 0);
    }

    @Test
    public void test_admin_revoke_A_from_A_i() throws Exception {
        securityService.grant(admin, user1, accessAction, document1Entity);
        securityService.revoke(admin, user1, accessAction, documentEntity);
        assertFalse(securityService.isGranted(user1, accessAction, document1Entity));
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_i_and_A_j() throws Exception {
        securityService.grant(admin, user1, accessAction, document2Entity);
        securityService.grant(admin, user1, accessAction, document1Entity);
        testPermissionSize(user1, 2);
        securityService.revoke(admin, user1, accessAction, documentEntity);
        assertFalse(securityService.isGranted(user1, accessAction, document2Entity));
        assertFalse(securityService.isGranted(user1, accessAction, document1Entity));
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
        testPermissionSize(user1, 0);

    }

    @Test
    public void test_admin_revoke_A_i_from_A_f_i() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        securityService.revoke(admin, user1, accessAction, document1Entity);
        assertFalse(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        assertFalse(securityService.isGranted(user1, accessAction, document1Entity));
    }

    @Test
    public void test_admin_revoke_A_i_from_A_f_i_and_A_g_i() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        securityService.grant(admin, user1, accessAction, document1EntityContentField);
        testPermissionSize(user1, 2);
        securityService.revoke(admin, user1, accessAction, document1Entity);
        testPermissionSize(user1, 0);
    }

    @Test(expected = SecurityActionException.class)
    public void test_admin_revoke_A_f_from_A() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.revoke(admin, user1, accessAction, documentEntityTitleField);
    }

    @Test(expected = SecurityActionException.class)
    public void test_admin_revoke_A_i_from_A() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        securityService.revoke(admin, user1, accessAction, document1Entity);
    }

    @Test(expected = SecurityActionException.class)
    public void test_admin_revoke_A_f_i_from_A_i() throws Exception {
        securityService.grant(admin, user1, accessAction, document1Entity);
        securityService.revoke(admin, user1, accessAction, document1EntityTitleField);
    }

    //access checks
    @Test
    public void test_admin_grants_access_to_object_user_accesses_other_object() throws Exception {
        securityService.grant(admin, user1, accessAction, document1Entity);
        assertFalse(securityService.isGranted(user1, accessAction, document2Entity));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_other_entity() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        assertFalse(securityService.isGranted(user1, accessAction, emailEntity));
    }

    @Test
    public void test_admin_grants_access_to_object_user_accesses_entity() throws Exception {
        securityService.grant(admin, user1, accessAction, document1Entity);
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_other_field() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        assertFalse(securityService.isGranted(user1, accessAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_field() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_object() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        assertFalse(securityService.isGranted(user1, accessAction, document1Entity));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_entity() throws Exception {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        assertFalse(securityService.isGranted(user1, accessAction, documentEntity));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_object_field() throws Exception {
        securityService.grant(admin, user1, accessAction, documentEntity);
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityContentField));
    }

    //admin cannot grant to users
    @Test(expected = SecurityException.class)
    public void test_admin_with_limited_permissions_grants_action_to_user() throws Exception {
        createAdminWithRevokePermissions();
        securityService.grant(admin, user1, accessAction, documentEntity);
    }

    //admin cannot revoke
    @Test(expected = SecurityException.class)
    public void test_admin_with_limited_permissions_grants_action_to_user2() throws Exception {
        createAdminWithGrantPermissions();
        securityService.grant(admin, user1, accessAction, documentEntity);
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        securityService.revoke(admin, user1, accessAction, documentEntity);
    }

    //admin has only grant and revoke permissions for users-> not allowed to grant to usergroups
    @Test(expected = SecurityException.class)
    public void test_admin_with_limited_permissions_grants_action_to_user4() throws Exception {
        createAdminWithPermissionsForUsers();
        roleSecurityService.grant(admin, userGroupA, readAction, emailEntity);
    }

    //admin has only grant and revoke permissions for user groups -> not allowed to grant to users
    @Test(expected = SecurityException.class)
    public void test_admin_with_limited_permissions_grants_action_to_user5() throws Exception {
        createAdminWithPermissionsForUserGroups();
        securityService.grant(admin, user1, readAction, emailEntity);
    }

    //module permissions
    @Test
    public void test_grant_permission_to_CarrierModule() {
        Collection<com.blazebit.security.Resource> result = securityService.getAllResources(CarrierModule.getInstance());
        for (com.blazebit.security.Resource resource : result) {
            securityService.grant(admin, user1, accessAction, resource);
        }

        Iterator it = result.iterator();
        while (it.hasNext()) {
            assertTrue(securityService.isGranted(user1, accessAction, (com.blazebit.security.Resource) it.next()));
        }
    }

    @Test
    public void test_current_case() throws Exception {
        utx.begin();
        UserGroup webUser = new UserGroup("webuser");
        entityManager.persist(webUser);
        UserGroup superuserC = new UserGroup("Superuser(C)");
        entityManager.persist(superuserC);
        UserGroup superuserS = new UserGroup("Superuser(S)");
        entityManager.persist(superuserS);
        utx.commit();


    }

    @Test
    public void test_current_FUR_1_groups() throws Exception {
        utx.begin();
        UserGroup g1 = new UserGroup("Nord-Kunden");
        entityManager.persist(g1);
        UserGroup g2 = new UserGroup("Süd-Kunden");
        entityManager.persist(g2);
        UserGroup g3 = new UserGroup("Ost-Kunden");
        entityManager.persist(g3);


        EntityField ostRegion = new EntityField("Ost-Region");
        EntityField nordRegion = new EntityField("Nord-Region");
        EntityField suedRegion = new EntityField("Sued-Region");

        roleSecurityService.grant(admin, g1, readAction, nordRegion);
        roleSecurityService.grant(admin, g2, readAction, suedRegion);
        roleSecurityService.grant(admin, g3, readAction, ostRegion);

        User superUser = new User("superuser");
        entityManager.persist(superUser);

        utx.commit();

        roleService.addSubjectToRole(admin, superUser, g1, true);
        assertTrue(securityService.isGranted(superUser, readAction, nordRegion));
        assertFalse(securityService.isGranted(superUser, readAction, suedRegion));

    }
}
