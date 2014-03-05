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

import com.blazebit.security.entity.EntityResourceMetamodel;
import com.blazebit.security.exception.PermissionActionException;
import com.blazebit.security.exception.PermissionException;
import com.blazebit.security.model.User;
import com.blazebit.security.service.PermissionService;
import com.blazebit.security.test.BeforeDatabaseAware;
import com.blazebit.security.test.DatabaseAware;

/**
 * 
 * @author cuszk
 */
@DatabaseAware
public class PermissionServiceTest extends BaseTest<PermissionServiceTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionService permissionService;
    
    @Inject
    private EntityResourceMetamodel metamodel;


    @BeforeDatabaseAware
    public void init() {
        super.initData();

        userGroupA.getUserGroups().add(userGroupB);
        userGroupB.setParent(userGroupA);
        merge(userGroupB);

        userGroupB.getUserGroups().add(userGroupC);
        userGroupC.setParent(userGroupB);
        merge(userGroupC);

        userGroupC.getUserGroups().add(userGroupD);
        userGroupD.setParent(userGroupC);
        merge(userGroupD);

        // D->C->B->A
        setUserContext(admin);
    }

    @Test
    public void test_initial_data() {
        // injections
        assertNotNull(permissionService);
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
        assertTrue(permissionService.isGranted(admin, grantAction, userEntity));
        assertTrue(permissionService.isGranted(admin, revokeAction, userEntity));
        assertTrue(permissionService.isGranted(admin, grantAction, groupEntity));
        assertTrue(permissionService.isGranted(admin, revokeAction, groupEntity));
    }

    // grant checks

    @Test
    public void test_admin_grants_action() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test(expected = PermissionException.class)
    public void test_admin2_has_no_grant_action() {
        User admin2 = new User();
        admin2.setUsername("admin2");
        persist(admin2);
        permissionService.grant(admin2, user1, readAction, documentEntity);
    }

    @Test
    public void test_admin_grants_different_entities() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.grant(admin, user1, readAction, emailEntity);

        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
        assertTrue(permissionService.isGranted(user1, readAction, document1Entity));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityTitleField));

        assertTrue(permissionService.isGranted(user1, readAction, emailEntity));
    }

    @Test
    public void test_admin_grants_different_actions() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.grant(admin, user1, createAction, documentEntity);

        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
        assertTrue(permissionService.isGranted(user1, readAction, document1Entity));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityTitleField));

        assertTrue(permissionService.isGranted(user1, createAction, documentEntity));
        assertTrue(permissionService.isGranted(user1, createAction, document1Entity));
        assertTrue(permissionService.isGranted(user1, createAction, documentEntityTitleField));
        assertTrue(permissionService.isGranted(user1, createAction, document1EntityTitleField));

    }

    @Test
    public void test_admin_grants_A_f_to_A_different_actions() {
        permissionService.grant(admin, user1, createAction, documentEntity);
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
        testPermissionSize(user1, 2);
    }

    @Test
    public void test_admin_grants_A_f_and_A_g() {
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
        permissionService.grant(admin, user1, readAction, documentEntityContentField);
        testPermissionSize(user1, 2);
    }

    @Test
    public void test_admin_grants_A_f_to_A_different_subjects() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.grant(admin, user2, readAction, documentEntityTitleField);
        testPermissionSize(user1, 1);
        testPermissionSize(user1, 1);
    }

    // grant checks with error

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_to_A() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.grant(admin, user1, readAction, documentEntity);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_f_to_A() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_i_to_A() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.grant(admin, user1, readAction, document1Entity);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_grants_A_f_i_to_A() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
    }

    // grant checks when existing permissions need to be removed
    @Test
    public void test_admin_grants_A_to_A_f() {
        // user gets access to 1 field of document entity
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
        // user gets access to document entity
        permissionService.grant(admin, user1, readAction, documentEntity);
        // user remains with only document entity permission
        testPermissionSize(user1, 1);
        // user can access document entity
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
        // user can access any field of document entity
        assertTrue(permissionService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntityContentField));
    }

    @Test
    public void test_admin_grants_A_to_A_f_and_A_g() {
        // user gets access to 2 fields of document entity
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
        permissionService.grant(admin, user1, readAction, documentEntityContentField);
        testPermissionSize(user1, 2);
        // user gets access to document entity
        permissionService.grant(admin, user1, readAction, documentEntity);
        // user remains with only document entity permission
        testPermissionSize(user1, 1);
        // user can access document entity
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
        // user can access any field of document entity
        assertTrue(permissionService.isGranted(user1, readAction, documentEntityTitleField));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntityContentField));
    }

    @Test
    public void test_admin_grants_A_i_to_A_f_i() {
        // user gets access to 1 field of document object with id 1
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        // user gets access to document object with id 1
        permissionService.grant(admin, user1, readAction, document1Entity);
        // user remains with only document object permission
        testPermissionSize(user1, 1);
        // user can access any field of document object
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_A_i_to_A_f_i_and_A_g_i() {
        // user gets access to 2 fields of document object with id 1
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        permissionService.grant(admin, user1, readAction, document1EntityContentField);
        testPermissionSize(user1, 2);
        // user gets access to document object with id 1
        permissionService.grant(admin, user1, readAction, document1Entity);
        // user remains with only document object permission
        testPermissionSize(user1, 1);
        // user can access any field of document object
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_A_to_A_i() {
        permissionService.grant(admin, user1, readAction, document1Entity);
        permissionService.grant(admin, user1, readAction, documentEntity);

        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_i_and_A_j() {
        permissionService.grant(admin, user1, readAction, document1Entity);
        permissionService.grant(admin, user1, readAction, document2Entity);
        testPermissionSize(user1, 2);
        permissionService.grant(admin, user1, readAction, documentEntity);
        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_f_i() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        permissionService.grant(admin, user1, readAction, documentEntity);

        testPermissionSize(user1, 1);
    }

    @Test
    public void test_admin_grants_A_to_A_f_i_and_A_g_i() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        permissionService.grant(admin, user1, readAction, document1EntityContentField);
        permissionService.grant(admin, user1, readAction, documentEntity);
        testPermissionSize(user1, 1);
    }

    // revoke checks
    @Test
    public void test_admin_revokes_action() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
        permissionService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_f() {
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
        permissionService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntityTitleField));
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_f_and_A_g() {
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
        permissionService.grant(admin, user1, readAction, documentEntityContentField);
        testPermissionSize(user1, 2);
        permissionService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntityTitleField));
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
        testPermissionSize(user1, 0);
    }

    @Test
    public void test_admin_revoke_A_from_A_i() {
        permissionService.grant(admin, user1, readAction, document1Entity);
        permissionService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(permissionService.isGranted(user1, readAction, document1Entity));
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_revoke_A_from_A_i_and_A_j() {
        permissionService.grant(admin, user1, readAction, document2Entity);
        permissionService.grant(admin, user1, readAction, document1Entity);
        testPermissionSize(user1, 2);
        permissionService.revoke(admin, user1, readAction, documentEntity);
        assertFalse(permissionService.isGranted(user1, readAction, document2Entity));
        assertFalse(permissionService.isGranted(user1, readAction, document1Entity));
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
        testPermissionSize(user1, 0);

    }

    @Test
    public void test_admin_revoke_A_i_from_A_f_i() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        permissionService.revoke(admin, user1, readAction, document1Entity);
        assertFalse(permissionService.isGranted(user1, readAction, document1EntityTitleField));
        assertFalse(permissionService.isGranted(user1, readAction, document1Entity));
    }

    @Test
    public void test_admin_revoke_A_i_from_A_f_i_and_A_g_i() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        permissionService.grant(admin, user1, readAction, document1EntityContentField);
        testPermissionSize(user1, 2);
        permissionService.revoke(admin, user1, readAction, document1Entity);
        testPermissionSize(user1, 0);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_f_from_A() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.revoke(admin, user1, readAction, documentEntityTitleField);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_i_from_A() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.revoke(admin, user1, readAction, document1Entity);
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_f_i_from_A_i() {
        permissionService.grant(admin, user1, readAction, document1Entity);
        permissionService.revoke(admin, user1, readAction, document1EntityTitleField);
    }

    // forced revoke checks
    @Test
    public void test_admin_revoke_A_f_from_A_forced() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.revoke(admin, user1, readAction, documentEntityTitleField, true);
        try {
            testPermissionSize(user1, metamodel.getFields(documentEntity.getEntity()).size() - 1);
        } catch (ClassNotFoundException e) {
        }
    }

    @Test
    public void test_admin_revoke_A_f_i_from_A_i_forced() {
        permissionService.grant(admin, user1, readAction, document1Entity);
        permissionService.revoke(admin, user1, readAction, document1EntityTitleField, true);
        try {
            testPermissionSize(user1, metamodel.getFields(document1Entity.getEntity()).size() - 1);
        } catch (ClassNotFoundException e) {
        }
    }

    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_i_from_A_forced() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        permissionService.revoke(admin, user1, readAction, document1Entity, true);
    }
    
    //TODO should this work? Ai - Af = Ai(abcde<f>g)
    @Test(expected = PermissionActionException.class)
    public void test_admin_revoke_A_f_from_A_i_forced() {
        permissionService.grant(admin, user1, readAction, documentEntityTitleField);
        permissionService.revoke(admin, user1, readAction, document1Entity, true);
    }

    // access checks
    @Test
    public void test_admin_grants_access_to_object_user_accesses_other_object() {
        permissionService.grant(admin, user1, readAction, document1Entity);
        assertFalse(permissionService.isGranted(user1, readAction, document2Entity));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_other_entity() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        assertFalse(permissionService.isGranted(user1, readAction, emailEntity));
    }

    @Test
    public void test_admin_grants_access_to_object_user_accesses_entity() {
        permissionService.grant(admin, user1, readAction, document1Entity);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_other_field() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        assertFalse(permissionService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_field() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityTitleField));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_object() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        assertFalse(permissionService.isGranted(user1, readAction, document1Entity));
    }

    @Test
    public void test_admin_grants_access_to_object_field_user_accesses_entity() {
        permissionService.grant(admin, user1, readAction, document1EntityTitleField);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_object_field() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityTitleField));
        assertTrue(permissionService.isGranted(user1, readAction, document1EntityContentField));
    }

    @Test
    public void test_admin_grants_access_to_entity_user_accesses_entity_field() {
        permissionService.grant(admin, user1, readAction, documentEntity);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntityTitleField));
    }

    // grant to role -> logic in grating and revoking for roles works the same way as for subjects
    // BUT difference is the propagation option to users----> tests for propagation
    // grant permission to A-> propagated to A users
    @Test
    public void test_grant_permission_to_role_propagate_to_users1() {
        userGroupA.getUsers().add(user1);
        merge(userGroupA);
        user1.getUserGroups().add(userGroupA);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity, true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_grant_permission_to_role_propagate_to_users2() {
        userGroupA.getUsers().add(user1);
        merge(userGroupA);
        user1.getUserGroups().add(userGroupA);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntityContentField, true);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
    }

    // garnt to A, propagate to users of child groups of A
    @Test
    public void test_grant_permission_to_role_propagate_to_users3() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity, true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    // grant to A, propagate to Users, it completes it for user into entity permission
    @Test
    public void test_grant_permission_to_role_propagate_to_users4() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, user1, readAction, documentEntity.withField("content"));
        permissionService.grant(admin, user1, readAction, documentEntity.withField("title"));
        permissionService.grant(admin, user1, readAction, documentEntity.withField("size"));

        permissionService.grant(admin, userGroupA, readAction, documentEntity.withField("id"), true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    // user already has the same permission
    @Test
    public void test_grant_permission_to_role_propagate_to_users5() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, user1, readAction, documentEntity);

        permissionService.grant(admin, userGroupA, readAction, documentEntity, true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    // user already has overrideing permission
    @Test
    public void test_grant_permission_to_role_propagate_to_users6() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, user1, readAction, documentEntity);

        permissionService.grant(admin, userGroupA, readAction, documentEntity.withField("content"), true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    // group permission will override user permission
    @Test
    public void test_grant_permission_to_role_propagate_to_users7() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, user1, readAction, documentEntity.withField("content"));

        permissionService.grant(admin, userGroupA, readAction, documentEntity, true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_grant_permission_to_role_propagate_to_users8() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        userGroupB.getUsers().add(user2);
        merge(userGroupB);
        user2.getUserGroups().add(userGroupB);
        merge(user2);

        permissionService.grant(admin, user1, readAction, documentEntity.withField("content"));
        permissionService.grant(admin, user2, readAction, documentEntity);

        permissionService.grant(admin, userGroupA, readAction, documentEntity, true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
        assertTrue(permissionService.isGranted(user2, readAction, documentEntity));
    }

    // by revoking permission from group take the same permission away from users
    @Test
    public void test_revoke_permission_to_role_propagate_to_users1() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity);
        permissionService.grant(admin, user1, readAction, documentEntity);

        permissionService.revoke(admin, userGroupA, readAction, documentEntity, true);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_revoke_permission_to_role_propagate_to_users2() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity);
        permissionService.grant(admin, user1, readAction, documentEntity.withField("content"));

        permissionService.revoke(admin, userGroupA, readAction, documentEntity, true);
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity.withField("content")));
    }

    @Test
    public void test_revoke_permission_to_role_propagate_to_users3() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity.withField("content"));
        permissionService.grant(admin, user1, readAction, documentEntity);

        permissionService.revoke(admin, userGroupA, readAction, documentEntity.withField("content"), true, true);
        
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity));
        assertFalse(permissionService.isGranted(user1, readAction, documentEntity.withField("content")));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity.withField("title")));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity.withField("size")));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity.withField("id")));
    }

    @Test
    public void test_revoke_permission_to_role_propagate_to_users4() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity);
        permissionService.grant(admin, userGroupA, readAction, emailEntity);

        permissionService.grant(admin, user1, readAction, documentEntity);

        permissionService.revoke(admin, userGroupA, readAction, emailEntity, true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity));
    }

    @Test
    public void test_revoke_permission_to_role_propagate_to_users5() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity.withField("content"));

        permissionService.grant(admin, user1, readAction, documentEntity.withField("title"));

        permissionService.revoke(admin, userGroupA, readAction, documentEntity.withField("content"), true);
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity.withField("title")));
    }

    @Test
    public void test_revoke_permission_to_role_propagate_to_users6() {
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        user1.getUserGroups().add(userGroupC);
        merge(user1);

        permissionService.grant(admin, userGroupA, readAction, documentEntity.withField("content"));
        permissionService.grant(admin, userGroupA, readAction, documentEntity.withField("title"));
        permissionService.grant(admin, userGroupA, readAction, documentEntity.withField("size"));

        assertTrue(permissionService.isGranted(userGroupA, readAction, documentEntity.withField("content")));
        assertTrue(permissionService.isGranted(userGroupA, readAction, documentEntity.withField("title")));
        assertTrue(permissionService.isGranted(userGroupA, readAction, documentEntity.withField("size")));

        permissionService.grant(admin, user1, readAction, documentEntity.withField("title"));
        permissionService.grant(admin, user1, readAction, documentEntity.withField("id"));

        permissionService.revoke(admin, userGroupA, readAction, documentEntity.withField("content"), true);
        permissionService.revoke(admin, userGroupA, readAction, documentEntity.withField("title"), true);

        assertTrue(permissionService.isGranted(userGroupA, readAction, documentEntity.withField("size")));
        assertTrue(permissionService.isGranted(user1, readAction, documentEntity.withField("id")));

    }

}
