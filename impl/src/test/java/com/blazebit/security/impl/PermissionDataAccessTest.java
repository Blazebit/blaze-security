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

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.SecurityService;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityConstants;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserPermission;
import com.blazebit.security.impl.utils.EntityUtils;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test if grant and revoke are allowed. Test which permissions can be "merged"
 * when granting or revoking.
 *
 * @author cuszk
 */
public class PermissionDataAccessTest extends BaseTest {

    @PersistenceContext
    private EntityManager entityManager;
    @Resource
    private UserTransaction utx;
    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private PermissionDataAccess permissionDataAccess;
    private EntityField emailEntityWithSubject;
    private EntityObjectField document2EntityTitleField;
    private EntityObjectField email1EntityTitleField;
    private EntityObjectField email1Entity;

    @Before
    public void init() throws Exception {
        utx.begin();
        super.initData();
        email1Entity = EntityUtils.getEntityObjectFieldFor(EntityConstants.EMAIL, "", "1");
        emailEntityWithSubject = EntityUtils.getEntityFieldFor(EntityConstants.EMAIL, "subject");
        document2EntityTitleField = EntityUtils.getEntityObjectFieldFor(EntityConstants.DOCUMENT, "title", "2");
        email1EntityTitleField = EntityUtils.getEntityObjectFieldFor(EntityConstants.EMAIL, "title", "1");

        UserPermission grantPermission = permissionFactory.create(admin, grantAction, userEntity);
        entityManager.persist(grantPermission);
        admin.getPermissions().add(grantPermission);
        admin = (User) entityManager.merge(admin);

        UserPermission revokePermission = permissionFactory.create(admin, revokeAction, userEntity);
        entityManager.persist(revokePermission);
        admin.getPermissions().add(revokePermission);
        admin = (User) entityManager.merge(admin);

        utx.commit();
    }

    /**
     * creates a permission
     *
     * @param subject
     * @param action
     * @param resource
     * @throws Exception
     */
    private void createPermission(Subject subject, Action action, com.blazebit.security.Resource resource) throws Exception {
        utx.begin();
        Permission permission = permissionFactory.create(subject, action, resource);
        entityManager.persist(permission);
        utx.commit();
    }

    //REVOKE
    //isRevokable
    //existing permission for entity, revoke permission for entity
    @Test
    public void test_isRevokable_A_minus_A() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //existing permission for entity with field, revoke permission for entity with field
    @Test
    public void test_isRevokable_Af_minus_Af() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntityTitleField));
    }

    //existing permission for entity with id, revoke permission for entity with id
    @Test
    public void test_isRevokable_Ai_minus_Ai() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, document1Entity));
    }

    //existing permission for entity with field and id, revoke permission for entity with field and id
    @Test
    public void test_isRevokable_Afi_minus_Afi() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, document1EntityTitleField));
    }

    //existing permission for entity B revoke permission for entity A -> not possible
    @Test
    public void test_isRevokable_B_minus_A() throws Exception {
        createPermission(user1, accessAction, emailEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //existing permission for entity with field, revoke permission for entity -> possible. will remove entity with field
    @Test
    public void test_isRevokable_Af_minus_A() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //existing permission for entity, revoke permission for entity with field -> not possible. Should be A-A+(A_with_field which is not "f")
    @Test
    public void test_isRevokable_A_minus_Af() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, documentEntityTitleField));
    }

    //existing permission for entity with id, revoke permission for entity -> possible. will remove entity with id
    @Test
    public void test_isRevokable_Ai_minus_A() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //existing permission for entity, revoke permission for entity with id -> not possible. Should be A-A+(A with ids which is not "i")
    @Test
    public void test_isRevokable_A_minus_Ai() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, document1Entity));
    }

    //existing permission for entity with field and id, revoke permission for entity -> possib;e. will remove entity with field and id
    @Test
    public void test_isRevokable_Afi_minus_A() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //existing p. for entity, revoke p. for entity with field and id -> not possible. Should be A-A+(A with fields and ids that are not "f" and "i")
    @Test
    public void test_isRevokable_A_minus_Afi() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, document1EntityTitleField));
    }

    //existing p. for entity with field and id, revoke p. for entity with id -> possible. remove permission for entitiy with field and id.
    @Test
    public void test_isRevokable_Afi_minus_Ai() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, document1Entity));
    }

    //existing p for entity with id, revoke p. for entity with field and id -> not possible.
    @Test
    public void test_isRevokable_Ai_minus_Afi() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, document1EntityTitleField));
    }

    //existing p. for entity with field, remove entity with id -> not possible
    @Test
    public void test_isRevokable_Af_minus_Ai() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, document1Entity));
    }

    //existing permission for entity with field, remove entity with field and id-> not possible
    @Test
    public void test_isRevokable_Af_minus_Afi() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, document1EntityTitleField));
    }

    //existing permission with id, remove entity with field-> not possible. Ai has access to all fields, cannot remove permission to one field
    @Test
    public void test_isRevokable_Ai_minus_Af() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, documentEntityTitleField));
    }

    //existing permission with field and id , revoke entity with field -> possible. will remove entity with field and id
    @Test
    public void test_isRevokable_Afi_minus_Af() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntityTitleField));
    }

    //different entity, one with field-> not possible
    @Test
    public void test_isRevokable_A_minus_B_f() throws Exception {
        createPermission(user1, accessAction, emailEntityWithSubject);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //different entity, one with id -> not possible
    @Test
    public void test_isRevokable_A_minus_B_i() throws Exception {
        createPermission(user1, accessAction, user1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //same entity, different action -> not possible
    @Test
    public void test_isRevokable_A_a_minus_A_b() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //same entity, same id, different action -> not possible
    @Test
    public void test_isRevokable_A_a_i_minus_A_b_i() throws Exception {
        createPermission(user1, readAction, document1Entity);
        assertFalse(permissionDataAccess.isRevokable(user1, accessAction, document1Entity));
    }

    //more permissions for entity fields, remove entity -> possible, will remove all entities with fields
    @Test
    public void test_isRevokable_A_f_and_A_g_minus_A() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        createPermission(user1, accessAction, document1EntityContentField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //more permissions for entity ids, remove entity -> possible, will remove all entities with ids
    @Test
    public void test_isRevokable_A_i_and_A_j_minus_A() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        createPermission(user1, accessAction, document2Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //more permissions for entity fields, and entity ids remove entity -> possible, will remove all entities with fields and ids
    @Test
    public void test_isRevokable_A_f_and_A_g_and_A_i_and_A_j_minus_A() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        createPermission(user1, accessAction, document1EntityContentField);
        createPermission(user1, accessAction, document1Entity);
        createPermission(user1, accessAction, document2Entity);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //existing permissions for different fields and ids, remove permission for entity -> will remove all entities with fields and ids
    @Test
    public void test_isRevokable_A_f_i_and_A_g_j_minus_A() throws Exception {
        createPermission(user1, accessAction, document1EntityContentField);
        createPermission(user1, accessAction, document2EntityTitleField);
        assertTrue(permissionDataAccess.isRevokable(user1, accessAction, documentEntity));
    }

    //getRevokablePermissionsWhenRevoking(Subject,...
    @Test
    public void test_getRevokablePermissions_A_i_and_A_j_minus_A() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        createPermission(user1, accessAction, document2Entity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1Entity));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document2Entity));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenRevoking(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }

    @Test
    public void test_getRevokablePermissions_A_f_i_and_A_g_j_minus_A() throws Exception {
        createPermission(user1, accessAction, document1EntityContentField);
        createPermission(user1, accessAction, document2EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1EntityContentField));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document2EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenRevoking(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }

    @Test
    public void test_getRevokablePermissions_A_f_i_and_A_g_j_and_B_i_and_A_a_minus_A() throws Exception {
        createPermission(user1, accessAction, document1EntityContentField);
        createPermission(user1, accessAction, document2EntityTitleField);
        createPermission(user1, accessAction, email1Entity);
        createPermission(user1, readAction, documentEntity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1EntityContentField));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document2EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenRevoking(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }
    //same for roles
    //getRevokablePermissionsWhenRevoking(Role,....

    //GRANT
    //already existing same permission
    @Test
    public void test_isGrantable_A_plus_A() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, documentEntity));

    }

    //already existing same permission
    @Test
    public void test_isGrantable_A_i_plus_A_i() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, document1Entity));
    }

    //already existing same permission
    @Test
    public void test_isGrantable_A_f_plus_A_f() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, documentEntityTitleField));
    }

    //already existing same permission
    @Test
    public void test_isGrantable_Af_i_plus_Afi() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, document1EntityTitleField));
    }

    //add permission for entity when existing permission for entity with field(s) -> merge into permission for entity
    @Test
    public void test_isGrantable_Af_plus_A() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, documentEntity));
    }

    //add permission for entity with field when existing permission for entity -> doesnt make sense. permission already exists for all entities for all fields
    @Test
    public void test_isGrantable_A_plus_Af() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, documentEntityTitleField));
    }

    //add permission for entity when existing permission for entity with id -> merge into permission for entity (for all ids, for all fields)
    @Test
    public void test_isGrantable_Ai_plus_A() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, documentEntity));
    }

    //add permission for entity with id when existing permission for entity -> permission already exists for all entities with all ids for all fields
    @Test
    public void test_isGrantable_A_plus_Ai() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, document1Entity));
    }

    //add permission for entity when existing permission for field and id -> merge into permission for entity
    @Test
    public void test_isGrantable_Afi_plus_A() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, documentEntity));
    }

    //add permission for entity with field and id when existing permission for entity -> permission already exists for all entities with all ids for all fields
    @Test
    public void test_isGrantable_A_plus_Afi() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, document1EntityTitleField));
    }

    //add permission for entity with id when existing permission for entity with field and id -> merge into permission for entity with id for all fields
    @Test
    public void test_isGrantable_Afi_plus_Ai() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, document1Entity));
    }

    //add permission for entity with id and field when existing permission for entity with id -> permission already exists for all entities with id for all fields
    @Test
    public void test_isGrantable_Ai_plus_Afi() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, document1EntityTitleField));
    }

    //add permission for entity with field when exists permission for id -> they dont exclude each other
    @Test
    public void test_isGrantable_Ai_plus_Af() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, documentEntityTitleField));
    }

    //add permission for entity with id when exists permission for field -> they dont exclude each other
    @Test
    public void test_isGrantable_Af_plus_Ai() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, document1Entity));
    }

    //add permission for entity with field and id when permission for field already exists -> doesnt make sense. permission already exists for all entities with the given field
    @Test
    public void test_isGrantable_Af_plus_Afi() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertFalse(permissionDataAccess.isGrantable(user1, accessAction, document1EntityTitleField));
    }

    //add permission with field to permission for entity with field and id -> will be merged into permission for all entities and the given field
    @Test
    public void test_isGrantable_Afi_plus_Af() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, documentEntityTitleField));
    }

    //different entities
    @Test
    public void test_isGrantable_A_plus_B() throws Exception {
        createPermission(user1, accessAction, documentEntity);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, emailEntity));
    }
    //different ids

    @Test
    public void test_isGrantable_A_i_plus_A_j() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, document2Entity));
    }

    //different fields
    @Test
    public void test_isGrantable_A_f_plus_A_g() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, documentEntityContentField));
    }

    //different actions
    @Test
    public void test_isGrantable_A_a_plus_A_b() throws Exception {
        createPermission(user1, readAction, documentEntity);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, documentEntityContentField));
    }

    //same entities, same ids, different fields
    @Test
    public void test_isGrantable_A_f_i_plus_A_g_i() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, document1EntityContentField));
    }

    //same entity, same field, different ids
    @Test
    public void test_isGrantable_A_f_i_plus_A_f_j() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, document2EntityTitleField));
    }

    //same field, same id, different entity
    @Test
    public void test_isGrantable_A_f_i_plus_B_f_i() throws Exception {
        createPermission(user1, readAction, document1EntityTitleField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, email1EntityTitleField));
    }

    //same entity, different field, different ids
    @Test
    public void test_isGrantable_A_f_i_plus_A_g_j() throws Exception {
        createPermission(user1, readAction, document1EntityContentField);
        assertTrue(permissionDataAccess.isGrantable(user1, accessAction, document2EntityTitleField));
    }

    //getRevokablePermissionsWhenGranting(Subject,....
    //add permission for entity when existing permission for entity with field(s) -> merge into permission for entity
    @Test
    public void test_getRevokablePermissionsWhenGranting_Af_plus_A() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, documentEntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }

    //add permission for entity with id when existing permission for entity with field and id -> merge into permission for entity with id for all fields
    @Test
    public void test_getRevokablePermissionsWhenGranting_Afi_plus_Ai() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, document1Entity);
        assertEquals(expected, actual);
    }

    //add permission for entity when existing permission for entity with id -> merge into permission for entity (for all ids, for all fields)
    @Test
    public void test_getRevokablePermissionsWhenGranting_A_plus_Ai() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1Entity));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }

    //add permission for entity when existing permission for entity with field and id -> merge into permission for entity
    @Test
    public void test_getRevokablePermissionsWhenGranting_Afi_plus_A() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1EntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }

    //grant combinations
    //permissions exists for entity and different fields, granting permission for entity, wil remove the existing permissions for fields
    @Test
    public void test_getRevokablePermissionsWhenGranting_Af_and_A_g_plus_A() throws Exception {
        createPermission(user1, accessAction, documentEntityTitleField);
        createPermission(user1, accessAction, documentEntityContentField);
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, documentEntity);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, documentEntityTitleField));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, documentEntityContentField));
        assertEquals(expected, actual);
    }

    //permissions exists for entity and different fields with ids, granting permission for entity, wil remove the existing permissions for fields
    //different entity with the same id and field stays
    @Test
    public void test_getRevokablePermissionsWhenGranting_Afi_and_Agi_and_Bfi_plus_Ai() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        createPermission(user1, accessAction, document1EntityContentField);
        createPermission(user1, accessAction, email1EntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1EntityTitleField));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1EntityContentField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, document1Entity);
        assertEquals(expected, actual);
    }

    //permissions for different ids and field exist, granting permission for the whole entity will remove existing permissions for field and ids
    @Test
    public void test_getRevokablePermissionsWhenGranting_Ai_and_Aj_and_Af_plus_A() throws Exception {
        createPermission(user1, accessAction, document1Entity);
        createPermission(user1, accessAction, document2Entity);
        createPermission(user1, accessAction, documentEntityTitleField);
        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1Entity));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document2Entity));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, documentEntityTitleField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }

    //add permission for entity when existing permission for entity with field and id -> merge into permission for entity
    @Test
    public void test_getRevokablePermissionsWhenGranting_Agi_and_Af_plus_A() throws Exception {
        createPermission(user1, accessAction, document1EntityTitleField);
        createPermission(user1, accessAction, documentEntityContentField);

        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionDataAccess.findPermission(user1, accessAction, document1EntityTitleField));
        expected.add(permissionDataAccess.findPermission(user1, accessAction, documentEntityContentField));
        Set<Permission> actual = permissionDataAccess.getRevokablePermissionsWhenGranting(user1, accessAction, documentEntity);
        assertEquals(expected, actual);
    }
    @Inject
    SecurityService securityService;
    //findPermission

    @Test
    public void test_findPermission() {
        securityService.grant(admin, user1, accessAction, document1EntityTitleField);
        securityService.grant(admin, user1, accessAction, document1Entity);
        assertNull(permissionDataAccess.findPermission(user1, accessAction, documentEntity));
        securityService.grant(admin, user1, accessAction, documentEntity);
        
        assertTrue(securityService.isGranted(user1, accessAction, documentEntity));
        assertTrue(securityService.isGranted(user1, accessAction, document1Entity));
        assertTrue(securityService.isGranted(user1, accessAction, document1EntityTitleField));
    }
}
