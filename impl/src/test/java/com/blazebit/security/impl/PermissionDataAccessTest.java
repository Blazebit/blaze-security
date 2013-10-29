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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;

/**
 * Test if grant and revoke are allowed. Test which permissions can be "merged" when granting or revoking.
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class PermissionDataAccessTest extends BaseTest<PermissionDataAccessTest> {

    @Inject
    private PermissionDataAccess permissionDataAccess;
    private EntityField emailEntityWithSubject;
    private EntityObjectField document2EntityTitleField;
    private EntityObjectField email1EntityTitleField;
    private EntityObjectField email1Entity;

    @Before
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() throws Exception {
        super.initData();
        email1Entity = (EntityObjectField) entityFieldFactory.createResource(Email.class, EntityField.EMPTY_FIELD, 1);
        emailEntityWithSubject = (EntityField) entityFieldFactory.createResource(Email.class, Subject_Field);
        document2EntityTitleField = (EntityObjectField) entityFieldFactory.createResource(Document.class, Title_Field, 2);
        email1EntityTitleField = (EntityObjectField) entityFieldFactory.createResource(Email.class, Subject_Field, 1);
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

    /**
     * creates a permission
     * 
     * @param subject
     * @param action
     * @param resource
     * @throws Exception
     */
    private Permission createPermission(Subject subject, Action action, com.blazebit.security.Resource resource) {
        Permission permission = permissionFactory.create(subject, action, resource);
        self.get().persist(permission);
        return permission;
    }

    /**
     * creates a permission
     * 
     * @param subject
     * @param action
     * @param resource
     * @throws Exception
     */
    private Permission createPermission(Role role, Action action, com.blazebit.security.Resource resource) {
        Permission permission = permissionFactory.create(role, action, resource);
        self.get().persist(permission);
        return permission;
    }

    // REVOKE
    // isRevokable
    // existing permission for entity, revoke permission for entity
    @Test
    public void test_isRevokable_A_minus_A() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // existing permission for entity with field, revoke permission for entity with field
    @Test
    public void test_isRevokable_Af_minus_Af() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntityTitleField));
    }

    // existing permission for entity with id, revoke permission for entity with id
    @Test
    public void test_isRevokable_Ai_minus_Ai() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, document1Entity));
    }

    // existing permission for entity with field and id, revoke permission for entity with field and id
    @Test
    public void test_isRevokable_Afi_minus_Afi() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, document1EntityTitleField));
    }

    // existing permission for entity B revoke permission for entity A -> not possible
    @Test
    public void test_isRevokable_B_minus_A() throws Exception {
        createPermission(user1, readAction, emailEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // existing permission for entity with field, revoke permission for entity -> possible. will remove entity with field
    @Test
    public void test_isRevokable_Af_minus_A() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // existing permission for entity, revoke permission for entity with field -> not possible. Should be A-A+(A_with_field
    // which is not "f")
    @Test
    public void test_isRevokable_A_minus_Af() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, documentEntityTitleField));
    }

    // existing permission for entity with id, revoke permission for entity -> possible. will remove entity with id
    @Test
    public void test_isRevokable_Ai_minus_A() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // existing permission for entity, revoke permission for entity with id -> not possible. Should be A-A+(A with ids which is
    // not "i")
    @Test
    public void test_isRevokable_A_minus_Ai() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, document1Entity));
    }

    // existing permission for entity with field and id, revoke permission for entity -> possib;e. will remove entity with field
    // and id
    @Test
    public void test_isRevokable_Afi_minus_A() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // existing p. for entity, revoke p. for entity with field and id -> not possible. Should be A-A+(A with fields and ids that
    // are not "f" and "i")
    @Test
    public void test_isRevokable_A_minus_Afi() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, document1EntityTitleField));
    }

    // existing p. for entity with field and id, revoke p. for entity with id -> possible. remove permission for entitiy with
    // field and id.
    @Test
    public void test_isRevokable_Afi_minus_Ai() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, document1Entity));
    }

    // existing p for entity with id, revoke p. for entity with field and id -> not possible.
    @Test
    public void test_isRevokable_Ai_minus_Afi() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, document1EntityTitleField));
    }

    // existing p. for entity with field, remove entity with id -> not possible
    @Test
    public void test_isRevokable_Af_minus_Ai() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, document1Entity));
    }

    // existing permission for entity with field, remove entity with field and id-> not possible
    @Test
    public void test_isRevokable_Af_minus_Afi() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, document1EntityTitleField));
    }

    // existing permission with id, remove entity with field-> not possible. Ai has access to all fields, cannot remove
    // permission to one field
    @Test
    public void test_isRevokable_Ai_minus_Af() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, documentEntityTitleField));
    }

    // existing permission with field and id , revoke entity with field -> possible. will remove entity with field and id
    @Test
    public void test_isRevokable_Afi_minus_Af() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntityTitleField));
    }

    // different entity, one with field-> not possible
    @Test
    public void test_isRevokable_A_minus_B_f() throws Exception {
        createPermission(user1, readAction, emailEntityWithSubject);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // different entity, one with id -> not possible
    @Test
    public void test_isRevokable_A_minus_B_i() throws Exception {
        createPermission(user1, readAction, user1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // same entity, different action -> not possible
    @Test
    public void test_isRevokable_A_a_minus_A_b() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, deleteAction, documentEntity));
    }

    // same entity, same id, different action -> not possible
    @Test
    public void test_isRevokable_A_a_i_minus_A_b_i() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, deleteAction, document1Entity));
    }

    // more permissions for entity fields, remove entity -> possible, will remove all entities with fields
    @Test
    public void test_isRevokable_A_f_and_A_g_minus_A() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        createPermission(user1, readAction, document1EntityContentField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // more permissions for entity ids, remove entity -> possible, will remove all entities with ids
    @Test
    public void test_isRevokable_A_i_and_A_j_minus_A() throws Exception {
        createPermission(user1, readAction, document1Entity);
        createPermission(user1, readAction, document2Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // more permissions for entity fields, and entity ids remove entity -> possible, will remove all entities with fields and
    // ids
    @Test
    public void test_isRevokable_A_f_and_A_g_and_A_i_and_A_j_minus_A() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        createPermission(user1, readAction, document1EntityContentField);
        createPermission(user1, readAction, document1Entity);
        createPermission(user1, readAction, document2Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // existing permissions for different fields and ids, remove permission for entity -> will remove all entities with fields
    // and ids
    @Test
    public void test_isRevokable_A_f_i_and_A_g_j_minus_A() throws Exception {
        createPermission(user1, readAction, document1EntityContentField);
        createPermission(user1, readAction, document2EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, readAction, documentEntity));
    }

    // getRevokablePermissionsWhenRevoking(Subject,...
    @Test
    public void test_getRevokablePermissions_A_i_and_A_j_minus_A() throws Exception {
        createPermission(user1, readAction, document1Entity);
        createPermission(user1, readAction, document2Entity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1Entity));
        expected.add(permissionDataAccess.findPermission(user1, readAction, document2Entity));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenRevoking(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    @Test
    public void test_getRevokablePermissions_A_f_i_and_A_g_j_minus_A() throws Exception {
        createPermission(user1, readAction, document1EntityContentField);
        createPermission(user1, readAction, document2EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1EntityContentField));
        expected.add(permissionDataAccess.findPermission(user1, readAction, document2EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenRevoking(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    @Test
    public void test_getRevokablePermissions_A_f_i_and_A_g_j_and_B_i_and_A_a_minus_A() throws Exception {
        createPermission(user1, readAction, document1EntityContentField);
        createPermission(user1, readAction, document2EntityTitleField);
        createPermission(user1, readAction, email1Entity);
        createPermission(user1, deleteAction, documentEntity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1EntityContentField));
        expected.add(permissionDataAccess.findPermission(user1, readAction, document2EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenRevoking(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    // same for roles
    // getRevokablePermissionsWhenRevoking(Role,....

    // GRANT
    // already existing same permission
    @Test
    public void test_isGrantable_A_plus_A() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, documentEntity));

    }

    // already existing same permission
    @Test
    public void test_isGrantable_A_i_plus_A_i() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, document1Entity));
    }

    // already existing same permission
    @Test
    public void test_isGrantable_A_f_plus_A_f() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, documentEntityTitleField));
    }

    // already existing same permission
    @Test
    public void test_isGrantable_Af_i_plus_Afi() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, document1EntityTitleField));
    }

    // add permission for entity when existing permission for entity with field(s) -> merge into permission for entity
    @Test
    public void test_isGrantable_Af_plus_A() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, documentEntity));
    }

    // add permission for entity with field when existing permission for entity -> doesnt make sense. permission already exists
    // for all entities for all fields
    @Test
    public void test_isGrantable_A_plus_Af() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, documentEntityTitleField));
    }

    // add permission for entity when existing permission for entity with id -> merge into permission for entity (for all ids,
    // for all fields)
    @Test
    public void test_isGrantable_Ai_plus_A() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, documentEntity));
    }

    // add permission for entity with id when existing permission for entity -> permission already exists for all entities with
    // all ids for all fields
    @Test
    public void test_isGrantable_A_plus_Ai() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, document1Entity));
    }

    // add permission for entity when existing permission for field and id -> merge into permission for entity
    @Test
    public void test_isGrantable_Afi_plus_A() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, documentEntity));
    }

    // add permission for entity with field and id when existing permission for entity -> permission already exists for all
    // entities with all ids for all fields
    @Test
    public void test_isGrantable_A_plus_Afi() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, document1EntityTitleField));
    }

    // add permission for entity with id when existing permission for entity with field and id -> merge into permission for
    // entity with id for all fields
    @Test
    public void test_isGrantable_Afi_plus_Ai() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, document1Entity));
    }

    // add permission for entity with id and field when existing permission for entity with id -> permission already exists for
    // all entities with id for all fields
    @Test
    public void test_isGrantable_Ai_plus_Afi() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, document1EntityTitleField));
    }

    // add permission for entity with field when exists permission for id -> they dont exclude each other
    @Test
    public void test_isGrantable_Ai_plus_Af() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, documentEntityTitleField));
    }

    // add permission for entity with id when exists permission for field -> they dont exclude each other
    @Test
    public void test_isGrantable_Af_plus_Ai() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, document1Entity));
    }

    // add permission for entity with field and id when permission for field already exists -> doesnt make sense. permission
    // already exists for all entities with the given field
    @Test
    public void test_isGrantable_Af_plus_Afi() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isGrantable(user1, readAction, document1EntityTitleField));
    }

    // add permission with field to permission for entity with field and id -> will be merged into permission for all entities
    // and the given field
    @Test
    public void test_isGrantable_Afi_plus_Af() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, documentEntityTitleField));
    }

    // different entities
    @Test
    public void test_isGrantable_A_plus_B() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, emailEntity));
    }

    // different ids

    @Test
    public void test_isGrantable_A_i_plus_A_j() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, document2Entity));
    }

    // different fields
    @Test
    public void test_isGrantable_A_f_plus_A_g() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, documentEntityContentField));
    }

    // different actions
    @Test
    public void test_isGrantable_A_a_plus_A_b() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertTrue(permissionDataAccess.isGrantable(user1, deleteAction, documentEntity));
    }

    // same entities, same ids, different fields
    @Test
    public void test_isGrantable_A_f_i_plus_A_g_i() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, document1EntityContentField));
    }

    // same entity, same field, different ids
    @Test
    public void test_isGrantable_A_f_i_plus_A_f_j() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, document2EntityTitleField));
    }

    // same field, same id, different entity
    @Test
    public void test_isGrantable_A_f_i_plus_B_f_i() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, email1EntityTitleField));
    }

    // same entity, different field, different ids
    @Test
    public void test_isGrantable_A_f_i_plus_A_g_j() throws Exception {
        createPermission(user1, readAction, document1EntityContentField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, document2EntityTitleField));
    }

    // getRevokablePermissionsWhenGranting(Subject,....
    // add permission for entity when existing permission for entity with field(s) -> merge into permission for entity
    @Test
    public void test_getRevokablePermissionsWhenGranting_Af_plus_A() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, documentEntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    // add permission for entity with id when existing permission for entity with field and id -> merge into permission for
    // entity with id for all fields
    @Test
    public void test_getRevokablePermissionsWhenGranting_Afi_plus_Ai() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, document1Entity);
        assertEquals(expected, actual);
    }

    // add permission for entity when existing permission for entity with id -> merge into permission for entity (for all ids,
    // for all fields)
    @Test
    public void test_getRevokablePermissionsWhenGranting_A_plus_Ai() throws Exception {
        createPermission(user1, readAction, document1Entity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1Entity));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    // add permission for entity when existing permission for entity with field and id -> merge into permission for entity
    @Test
    public void test_getRevokablePermissionsWhenGranting_Afi_plus_A() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    // grant combinations
    // permissions exists for entity and different fields, granting permission for entity, wil remove the existing permissions
    // for fields
    @Test
    public void test_getRevokablePermissionsWhenGranting_Af_and_A_g_plus_A() throws Exception {
        createPermission(user1, readAction, documentEntityTitleField);
        createPermission(user1, readAction, documentEntityContentField);
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, documentEntity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, documentEntityTitleField));
        expected.add(permissionDataAccess.findPermission(user1, readAction, documentEntityContentField));
        assertEquals(expected, actual);
    }

    // permissions exists for entity and different fields with ids, granting permission for entity, wil remove the existing
    // permissions for fields
    // different entity with the same id and field stays
    @Test
    public void test_getRevokablePermissionsWhenGranting_Afi_and_Agi_and_Bfi_plus_Ai() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        createPermission(user1, readAction, document1EntityContentField);
        createPermission(user1, readAction, email1EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1EntityTitleField));
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1EntityContentField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, document1Entity);
        assertEquals(expected, actual);
    }

    // permissions for different ids and field exist, granting permission for the whole entity will remove existing permissions
    // for field and ids
    @Test
    public void test_getRevokablePermissionsWhenGranting_Ai_and_Aj_and_Af_plus_A() throws Exception {
        createPermission(user1, readAction, document1Entity);
        createPermission(user1, readAction, document2Entity);
        createPermission(user1, readAction, documentEntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1Entity));
        expected.add(permissionDataAccess.findPermission(user1, readAction, document2Entity));
        expected.add(permissionDataAccess.findPermission(user1, readAction, documentEntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    // add permission for entity when existing permission for entity with field and id -> merge into permission for entity
    @Test
    public void test_getRevokablePermissionsWhenGranting_Agi_and_Af_plus_A() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        createPermission(user1, readAction, documentEntityContentField);

        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, readAction, document1EntityTitleField));
        expected.add(permissionDataAccess.findPermission(user1, readAction, documentEntityContentField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, readAction, documentEntity);
        assertEquals(expected, actual);
    }

    @Inject
    PermissionService securityService;

    // findPermission
    @Test
    public void test_findPermission() {
        securityService.grant(admin, user1, readAction, document1EntityTitleField);
        securityService.grant(admin, user1, readAction, document1Entity);
        assertNull(permissionDataAccess.findPermission(user1, readAction, documentEntity));
        securityService.grant(admin, user1, readAction, documentEntity);

        assertTrue(securityService.isGranted(user1, readAction, documentEntity));
        assertTrue(securityService.isGranted(user1, readAction, document1Entity));
        assertTrue(securityService.isGranted(user1, readAction, document1EntityTitleField));
    }

    // entity object field 1 + entity object field 2 + entity object= entity object
    @Test
    public void test_isGrantable_Afi_Afj_plus_A() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        createPermission(user1, readAction, document1EntityContentField);
        assertTrue(permissionDataAccess.isGrantable(user1, readAction, documentEntity));
    }

    @Inject
    private PermissionManager permissionManager;

    @Test
    public void test_add_user_to_groups_1() {
        Set<Permission> expectedToBeGranted = new HashSet<Permission>();
        Set<Permission> actualToBeGranted = new HashSet<Permission>();

        UserGroup userGroup1 = new UserGroup("Usergroup 1");
        self.get().persist(userGroup1);
        expectedToBeGranted.add(createPermission(userGroup1, readAction, documentEntity));

        UserGroup userGroup2 = new UserGroup("Usergroup 2");
        userGroup2.setParent(userGroup1);
        self.get().persist(userGroup2);
        expectedToBeGranted.add(createPermission(userGroup2, readAction, entityFieldFactory.createResource(Comment.class)));

        UserGroup userGroup3 = new UserGroup("Usergroup 3");
        userGroup3.setParent(userGroup2);
        self.get().persist(userGroup3);

        expectedToBeGranted.add(createPermission(userGroup3, readAction, emailEntity));

        User user = new User("user");
        self.get().persist(user);

        Permission ret = createPermission(user, readAction, entityFieldFactory.createResource(Comment.class, "text"));
        Set<Permission> expectedToBeRevoked = new HashSet<Permission>();
        expectedToBeRevoked.add(ret);
        Set<Permission> actualToBeRevoked = new HashSet<Permission>();

        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroup3);
        for (UserGroup userGroup : selectedGroups) {
            // add user to userGroup 3
            UserGroup parent = userGroup;
            while (parent != null) {
                for (Permission p : permissionManager.getAllPermissions(parent)) {
                    if (permissionDataAccess.isGrantable(user, p.getAction(), p.getResource())) {
                        actualToBeGranted.add(p);
                    }
                    actualToBeRevoked.addAll(permissionDataAccess.getRevokablePermissionsWhenGranting(user, p.getAction(), p.getResource()));
                }
                parent = parent.getParent();
            }
        }
        assertEquals(expectedToBeRevoked, actualToBeRevoked);
        assertEquals(expectedToBeGranted, actualToBeGranted);
    }

 
}
