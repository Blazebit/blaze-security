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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.PermissionActionException;
import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.model.User;

/**
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class SecurityServiceTest extends BaseTest<SecurityServiceTest> {

    @Inject
    private PermissionService securityService;

    @Before
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        super.initData();
        setUserContext(admin);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persist(Object object) {
        entityManager.persist(object);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Object merge(Object object) {
        object = entityManager.merge(object);
        entityManager.flush();
        return object;
    }

    @Test
    public void test_initial_data() {
        // injections
        assertNotNull(securityService);
        // entityManager
        assertNotNull(entityManager);
        // entities
        assertNotNull(admin);
        assertNotNull(user1);
        assertNotNull(user2);
        assertNotNull(userGroupA);
        assertNotNull(userGroupB);
    }

    @Test
    public void test_admin_has_grant_and_revoke_action_for_users_and_groups() {
        assertTrue(securityService.isGranted(admin, grantAction, userEntity));
        assertTrue(securityService.isGranted(admin, revokeAction, userEntity));
        assertTrue(securityService.isGranted(admin, grantAction, groupEntity));
        assertTrue(securityService.isGranted(admin, revokeAction, groupEntity));
    }

    // grant checks

    @Test
    public void test_admin_grants_action() {
        securityService.grant(admin, user1, readAction, documentEntity);
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
    }

    @Test(expected = PermissionException.class)
    public void test_admin2_has_no_grant_action() {
        User admin2 = new User();
        admin2.setUsername("admin2");
        self.get().persist(admin2);
        securityService.grant(admin2, user1, readAction, documentEntity);
    }

    @Test
    public void test_admin_grants_different_entities() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.grant(admin, user1, readAction, emailEntity);

        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));

        assertTrue(securityService.isGranted(user1, readAction, emailEntity));
    }

    @Test
    public void test_admin_grants_different_actions() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.grant(admin, user1, createAction, documentEntity);

        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));

        assertTrue(securityService.isGranted(user1, createAction, documentEntity));
        assertTrue(securityService.isGranted(user1, createAction, document1Entity));
        assertTrue(securityService.isGranted(user1, createAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, createAction, document1EntityTitleField));

    }

    @Test
    public void test_admin_grants_A_f_to_A_different_actions() {
        securityService.grant(admin, user1, createAction, documentEntity);
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
    }

    @Test
    public void test_admin_grants_A_f_and_A_g() {
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
        securityService.grant(admin, user1, readAction, documentEntityContentField);
        testPermissionSize(user1, 2);
    }

    @Test
    public void test_admin_grants_A_f_to_A_different_subjects() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.grant(admin, user2, readAction, documentEntityTitleField);
    }

    // grant checks with error

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_to_A() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.grant(admin, user1, readAction, documentEntity);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_f_to_A() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_i_to_A() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.grant(admin, user1, readAction, document1Entity);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_f_i_to_A() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
    }

    // grant checks when existing permissions need to be removed
    @Test
    public void test_admin_grants_A_to_A_f() {
        // user gets access to 1 field of document entity
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
        // user gets access to document entity
        securityService.grant(admin, user1, readAction, documentEntity);
        // user remains with only document entity permission
        testPermissionSize(user1, 1);
        // user can access document entity
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        // user can access any field of document entity
        assertTrue(securityService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, documentEntityContentField));
    }

    @Test
    public void test_admin_grants_A_to_A_f_and_A_g() {
        // user gets access to 2 fields of document entity
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
        securityService.grant(admin, user1, readAction, documentEntityContentField);
        testPermissionSize(user1, 2);
        // user gets access to document entity
        securityService.grant(admin, user1, readAction, documentEntity);
        // user remains with only document entity permission
        testPermissionSize(user1, 1);
        // user can access document entity
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        // user can access any field of document entity
        assertTrue(securityService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, documentEntityContentField));
    }

    @Test
    public void test_admin_grants_A_i_to_A_f_i() {
        // user gets access to 1 field of document object with id 1
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        // user gets access to document object with id 1
        securityService.grant(admin, user1, readAction, document1Entity);
        // user remains with only document object permission
        testPermissionSize(user1, 1);
        // user can access any field of document object
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_A_i_to_A_f_i_and_A_g_i() {
        // user gets access to 2 fields of document object with id 1
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        securityService.grant(admin, user1, readAction, document1EntityContentField);
        testPermissionSize(user1, 2);
        // user gets access to document object with id 1
        securityService.grant(admin, user1, readAction, document1Entity);
        // user remains with only document object permission
        testPermissionSize(user1, 1);
        // user can access any field of document object
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_A_to_A_i() {
        securityService.grant(admin, user1, readAction, document1Entity);
        securityService.grant(admin, user1, readAction, documentEntity);

        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_i_and_A_j() {
        securityService.grant(admin, user1, readAction, document1Entity);
        securityService.grant(admin, user1, readAction, document2Entity);
        testPermissionSize(user1, 2);
        securityService.grant(admin, user1, readAction, documentEntity);
        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_f_i() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        securityService.grant(admin, user1, readAction, documentEntity);

        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_f_i_and_A_g_i() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        securityService.grant(admin, user1, readAction, document1EntityContentField);
        securityService.grant(admin, user1, readAction, documentEntity);
        testPermissionSize(user1, 1);
    }

    // revoke checks
    @Test
    public void test_admin_revokes_action() {
        securityService.grant(admin, user1, readAction, documentEntity);
        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        securityService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_f() {
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
        securityService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(securityService.isGranted(user1, readAction, documentEntityTitleField));
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_f_and_A_g() {
        securityService.grant(admin, user1, readAction, documentEntityTitleField);
        securityService.grant(admin, user1, readAction, documentEntityContentField);
        testPermissionSize(user1, 2);
        securityService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(securityService.isGranted(user1, readAction, documentEntityTitleField));
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
        testPermissionSize(user1, 0);
    }

    @Test
    public void test_admin_revoke_A_from_A_i() {
        securityService.grant(admin, user1, readAction, document1Entity);
        securityService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(securityService.isGranted(user1, readAction, document1Entity));
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_i_and_A_j() {
        securityService.grant(admin, user1, readAction, document2Entity);
        securityService.grant(admin, user1, readAction, document1Entity);
        testPermissionSize(user1, 2);
        securityService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(securityService.isGranted(user1, readAction, document2Entity));
        assertFalse(securityService.isGranted(user1, readAction, document1Entity));
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
        testPermissionSize(user1, 0);

    }

    @Test
    public void test_admin_revoke_A_i_from_A_f_i() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        securityService.revoke(admin, user1, readAction, document1Entity);
        assertFalse(securityService.isGranted(user1, readAction, document1EntityTitleField));
        assertFalse(securityService.isGranted(user1, readAction, document1Entity));
    }

    @Test
    public void test_admin_revoke_A_i_from_A_f_i_and_A_g_i() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        securityService.grant(admin, user1, readAction, document1EntityContentField);
        testPermissionSize(user1, 2);
        securityService.revoke(admin, user1, readAction, document1Entity);
        testPermissionSize(user1, 0);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_f_from_A() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.revoke(admin, user1, readAction, documentEntityTitleField);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_i_from_A() {
        securityService.grant(admin, user1, readAction, documentEntity);
        securityService.revoke(admin, user1, readAction, document1Entity);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_f_i_from_A_i() {
        securityService.grant(admin, user1, readAction, document1Entity);
        securityService.revoke(admin, user1, readAction, document1EntityTitleField);
    }

    // access checks
    @Test
    public void test_admin_grants_access_to_object_user_accesses_other_object() {
        securityService.grant(admin, user1, readAction, document1Entity);
        assertFalse(securityService.isGranted(user1, readAction, document2Entity));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_other_entity() {
        securityService.grant(admin, user1, readAction, documentEntity);
        assertFalse(securityService.isGranted(user1, readAction, emailEntity));
    }

    @Test
    public void test_admin_grants_access_to_object_user_accesses_entity() {
        securityService.grant(admin, user1, readAction, document1Entity);
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_other_field() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        assertFalse(securityService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_field() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_object() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        assertFalse(securityService.isGranted(user1, readAction, document1Entity));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_entity() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        assertFalse(securityService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_object_field() {
        securityService.grant(admin, user1, readAction, documentEntity);
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_entity_field() {
        securityService.grant(admin, user1, readAction, documentEntity);
        assertTrue(securityService.isGranted(user1, readAction, documentEntityTitleField));
    }

}
