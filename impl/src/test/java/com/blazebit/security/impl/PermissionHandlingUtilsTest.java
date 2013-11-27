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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.impl.utils.PermissionHandlingUtils;

/**
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class PermissionHandlingUtilsTest extends BaseTest<PermissionHandlingUtilsTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionHandlingUtils permissionHandlingUtils;
    @Inject
    private PermissionFactory permissionFactory;

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

    // implies(Collection<Permission>, Permission)
    @Test
    public void test_implies_parent_implies_parent() {
        Permission givenPermission = permissionFactory.create(updateAction, documentEntity);
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(givenPermission);
        assertTrue(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_child_implies_child() {
        Permission givenPermission = permissionFactory.create(updateAction, documentEntityContentField);
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(givenPermission);
        assertTrue(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_parent_implies_child() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, documentEntity));

        Permission givenPermission = permissionFactory.create(updateAction, documentEntityContentField);
        assertTrue(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_child_does_not_imply_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Permission givenPermission = permissionFactory.create(updateAction, documentEntity);
        assertFalse(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_object_parent_implies_object_child() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1Entity));

        Permission givenPermission = permissionFactory.create(updateAction, document1EntityContentField);
        assertTrue(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_object_parent_implies_object_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1Entity));

        Permission givenPermission = permissionFactory.create(updateAction, document1Entity);
        assertTrue(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_object_child_does_not_imply_object_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));

        Permission givenPermission = permissionFactory.create(updateAction, document1Entity);
        assertFalse(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_parent_implies_object_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, documentEntity));

        Permission givenPermission = permissionFactory.create(updateAction, document1Entity);
        assertTrue(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    @Test
    public void test_implies_parent_implies_object_child() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, documentEntity));

        Permission givenPermission = permissionFactory.create(updateAction, document1EntityContentField);
        assertTrue(permissionHandlingUtils.implies(permissions, givenPermission));
    }

    // replaces(Collection<Permission>, Permission)
    @Test
    public void test_replaces_parent_doees_not_replace_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, documentEntity));

        Permission givenPermission = permissionFactory.create(updateAction, documentEntity);
        assertFalse(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_replaces_child_does_not_replaces_child() {
        Permission givenPermission = permissionFactory.create(updateAction, documentEntityContentField);
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(givenPermission);
        assertFalse(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_replaces_parent_replaces_child() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Permission givenPermission = permissionFactory.create(updateAction, documentEntity);
        assertTrue(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_replaces_child_does_not_replace_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, documentEntity));

        Permission givenPermission = permissionFactory.create(updateAction, documentEntityContentField);
        assertFalse(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_replaces_object_parent_replaces_object_child() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));

        Permission givenPermission = permissionFactory.create(updateAction, document1Entity);
        assertTrue(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_replaces_object_parent_does_not_replace_object_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1Entity));

        Permission givenPermission = permissionFactory.create(updateAction, document1Entity);
        assertFalse(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_replaces_object_child_does_not_replace_object_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1Entity));

        Permission givenPermission = permissionFactory.create(updateAction, document1EntityContentField);
        assertFalse(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_replaces_parent_replaces_object_parent() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1Entity));

        Permission givenPermission = permissionFactory.create(updateAction, documentEntity);
        assertTrue(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    @Test
    public void test_implies_parent_replaces_object_child() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));

        Permission givenPermission = permissionFactory.create(updateAction, documentEntity);
        assertTrue(permissionHandlingUtils.replaces(permissions, givenPermission));
    }

    // getNormalizedPermissions(Collection<Permission>)
    @Test
    public void test_getNormalizedPermissions() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntity));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    @Test
    public void test_getNormalizedPermissions1() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));
        redundantPermissions.add(permissionFactory.create(updateAction, emailEntity.getChild("subject")));

        permissions.add(permissionFactory.create(updateAction, emailEntity.getChild("subject")));
        permissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntity));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    @Test
    public void test_getNormalizedPermissions2() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        permissions.add(permissionFactory.create(updateAction, documentEntity));
        permissions.add(permissionFactory.create(updateAction, document1Entity));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));
    }

    // entity field permission and entity object field permission are implied by existing entity permission-> only entity
    // permission stays
    @Test
    public void test_getNormalizedPermissions3() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        permissions.add(permissionFactory.create(updateAction, documentEntity));
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // entity object fields are implied by existing entity object permission, entity field permission stays
    @Test
    public void test_getNormalizedPermissions4() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        redundantPermissions.add(permissionFactory.create(updateAction, document1Entity));
        redundantPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        permissions.add(permissionFactory.create(updateAction, document1Entity));
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // complete entity fields are merged into entity permission
    @Test
    public void test_getNormalizedPermissions5() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // incomplete entity fields stay as field permissions
    @Test
    public void test_getNormalizedPermissions6() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        redundantPermissions.addAll(permissions);

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // entity object permission and and entity field permissions are all implied by entity permission, keep only entity
    // permission
    @Test
    public void test_getNormalizedPermissions7() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        permissions.add(permissionFactory.create(updateAction, document1Entity));
        permissions.add(permissionFactory.create(updateAction, documentEntity));
        permissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // complete entity object fields imply entity object permission
    @Test
    public void test_getNormalizedPermissions8() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, document1Entity));

        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        permissions.add(permissionFactory.create(updateAction, document1EntityTitleField));
        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // entity object fields are implied by existing entity object permission
    @Test
    public void test_getNormalizedPermissions9() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, document1Entity));

        permissions.add(permissionFactory.create(updateAction, document1Entity));
        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        permissions.add(permissionFactory.create(updateAction, document1EntityTitleField));
        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // complete entity object fields and entity fields, entity fields imply entity object fields and entity fields will be
    // merged into entity permission
    @Test
    public void test_getNormalizedPermissions10() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        permissions.add(permissionFactory.create(updateAction, document1EntityTitleField));
        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // complete entity fields, missing entity id fields -> entity fields imply entity id fields, and entity fields merged into
    // entity permission
    @Test
    public void test_getNormalizedPermissions11() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        permissions.add(permissionFactory.create(updateAction, document1EntityTitleField));

        permissions.add(permissionFactory.create(updateAction, document1EntityContentField.getInstance("2")));
        permissions.add(permissionFactory.create(updateAction, document1EntityTitleField.getInstance("2")));
        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("size").getInstance("2")));

        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        permissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        permissions.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // entity field implies all entity object fields
    @Test
    public void test_getNormalizedPermissions12() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        redundantPermissions.add(permissionFactory.create(updateAction, document1Entity));

        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        permissions.add(permissionFactory.create(updateAction, document1EntityTitleField));
        permissions.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        permissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // entity object fields merged into entity id permission and entity field permission stays
    @Test
    public void test_getNormalizedPermissions13() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();
        redundantPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        permissions.add(permissionFactory.create(updateAction, document1EntityContentField));

        permissions.add(permissionFactory.create(updateAction, document1EntityContentField.getInstance("2")));

        permissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions));

    }

    // multiple entity permission
    @Test
    public void test_getNormalizedPermissions14() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        permissions.add(permissionFactory.create(userGroupA, updateAction, documentEntity));
        permissions.add(permissionFactory.create(admin, updateAction, documentEntity));

        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        assertTrue(redundantPermissions.size() == permissionHandlingUtils.getNormalizedPermissions(permissions).size());
        assertTrue(permissionHandlingUtils.containsAll(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions)));

    }
    
    @Test
    public void test_getNormalizedPermissions15() {
        Set<Permission> permissions = new HashSet<Permission>();
        Set<Permission> redundantPermissions = new HashSet<Permission>();

        permissions.add(permissionFactory.create(userGroupA, updateAction, documentEntity));
        permissions.add(permissionFactory.create(admin, updateAction, documentEntityContentField));
        permissions.add(permissionFactory.create(admin, updateAction, documentEntity));

        redundantPermissions.add(permissionFactory.create(updateAction, documentEntity));

        assertTrue(redundantPermissions.size() == permissionHandlingUtils.getNormalizedPermissions(permissions).size());
        assertTrue(permissionHandlingUtils.containsAll(redundantPermissions, permissionHandlingUtils.getNormalizedPermissions(permissions)));

    }

    // getGrantable(Collection<Permission>, Collection<Permission>) from selectedpermissions

    // given document update + selected is email update -> email update
    @Test
    public void test_getGranted1() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, emailEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, emailEntity));

        Set<Permission> notGranted = new HashSet<Permission>();

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(notGranted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(1));

    }

    // given document update -> document field cannot be granted
    @Test
    public void test_getGranted2() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> granted = new HashSet<Permission>();

        Set<Permission> notGranted = new HashSet<Permission>();
        notGranted.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(notGranted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(1));

    }

    // entity field permission given + grant entity permission -> granted entity permission
    @Test
    public void test_getGranted3() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(new HashSet<Permission>(), permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(1));

    }

    // Resources->Users
    @Test
    public void test_grant_resource_to_users1() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> toGrant = new HashSet<Permission>();
        toGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();

        Set<Permission> notGranted = new HashSet<Permission>();
        notGranted.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> replaced = new HashSet<Permission>();

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, toGrant).get(0));
        assertSetsEquals(notGranted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, toGrant).get(1));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, toGrant));

    }

    @Test
    public void test_grant_resource_to_users2() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> toGrant = new HashSet<Permission>();
        toGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> notGranted = new HashSet<Permission>();

        Set<Permission> replaced = new HashSet<Permission>();
        replaced.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, toGrant).get(0));
        assertSetsEquals(notGranted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, toGrant).get(1));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, toGrant));

    }

    // getGranted+getFinalGranted
    // current: few field permissions+ grant missing field permissions->entity permission
    @Test
    public void test_getGranted_and_finalGranted1() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(0));

        Set<Permission> finalGranted = new HashSet<Permission>();
        finalGranted.add(permissionFactory.create(updateAction, documentEntity));

        assertSetsEquals(finalGranted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, new HashSet<Permission>(), granted).get(1));

        Set<Permission> finalRevoked = new HashSet<Permission>();
        finalRevoked.add(permissionFactory.create(updateAction, documentEntityContentField));
        finalRevoked.add(permissionFactory.create(updateAction, documentEntityTitleField));

        assertSetsEquals(finalRevoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, new HashSet<Permission>(), granted).get(0));

    }

    // current: few object field permissions+missing object field permissions->entity permission
    @Test
    public void test_getGranted_and_finalGranted2() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, document1EntityTitleField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        selectedPermissions.add(permissionFactory.create(updateAction, document1EntityTitleField));
        selectedPermissions.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        selectedPermissions.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        granted.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(0));

        Set<Permission> finalGranted = new HashSet<Permission>();
        finalGranted.add(permissionFactory.create(updateAction, document1Entity));

        assertSetsEquals(finalGranted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, new HashSet<Permission>(), granted).get(1));

        Set<Permission> finalRevoked = new HashSet<Permission>();
        finalRevoked.add(permissionFactory.create(updateAction, document1EntityContentField));
        finalRevoked.add(permissionFactory.create(updateAction, document1EntityTitleField));

        assertSetsEquals(finalRevoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, new HashSet<Permission>(), granted).get(0));

    }

    // getGranted+getFinalGranted+check revoked too
    @Test
    public void test_getGranted_and_finalGranted3() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(readAction, document1EntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, document1EntityContentField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, document1EntityContentField));
        selectedPermissions.add(permissionFactory.create(updateAction, document1EntityTitleField));
        selectedPermissions.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        selectedPermissions.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        selectedPermissions.add(permissionFactory.create(readAction, document1EntityTitleField));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, document1EntityTitleField));
        granted.add(permissionFactory.create(updateAction, document1Entity.getChild("id")));
        granted.add(permissionFactory.create(updateAction, document1Entity.getChild("size")));

        granted.add(permissionFactory.create(readAction, document1EntityTitleField));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(readAction, document1EntityContentField));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        Set<Permission> finalGranted = new HashSet<Permission>();
        finalGranted.add(permissionFactory.create(updateAction, document1Entity));
        finalGranted.add(permissionFactory.create(readAction, document1EntityTitleField));

        assertSetsEquals(finalGranted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(1));

        Set<Permission> finalRevoked = new HashSet<Permission>();
        finalRevoked.add(permissionFactory.create(readAction, document1EntityContentField));
        finalRevoked.add(permissionFactory.create(updateAction, document1EntityContentField));

        assertSetsEquals(finalRevoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(0));

    }

    // getRevoked - revoke fields: id and size
    @Test
    public void test_getRevoked1() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityTitleField));
        granted.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(1));
        assertSetsEquals(revoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(0));

    }

    @Test
    public void test_getRevoked2() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityTitleField));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(1));
        assertSetsEquals(revoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(0));

    }

    @Test
    public void test_getRevoked3() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntityContentField));
        revoked.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(1));
        assertSetsEquals(revoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(0));

    }

    @Test
    public void test_getRevoked4() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, emailEntity));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, emailEntity));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(1));
        assertSetsEquals(revoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(0));

    }

    @Test
    public void test_getRevoked5() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(readAction, documentEntity));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(readAction, documentEntityTitleField));
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(readAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(readAction, documentEntityTitleField));
        granted.add(permissionFactory.create(updateAction, documentEntity));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));
        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(1));
        assertSetsEquals(revoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, revoked, granted).get(0));

    }

    // Combined granting, revoking and replacing - Scenarios

    // same action
    // field+entity
    @Test
    public void test_scenario1() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> revoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> replaced = new HashSet<Permission>();
        replaced.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, selectedPermissions));

    }

    // field1-field1+field2
    @Test
    public void test_scenario2() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> replaced = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, granted));

    }

    // field1+field2-field2
    @Test
    public void test_scenario3() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> granted = new HashSet<Permission>();

        Set<Permission> replaced = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, granted));

    }

    // entity-entity+field
    @Test
    public void test_scenario4() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> replaced = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, selectedPermissions));

    }

    // different actions
    @Test
    public void test_scenario5() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(createAction, documentEntity));
        currentPermissions.add(permissionFactory.create(readAction, documentEntity));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(deleteAction, documentEntity));
        selectedPermissions.add(permissionFactory.create(readAction, documentEntity));
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(deleteAction, documentEntity));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(createAction, documentEntity));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

    }

    // different actions
    @Test
    public void test_scenario6() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(createAction, documentEntity));
        currentPermissions.add(permissionFactory.create(readAction, documentEntity));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(createAction, documentEntity));
        selectedPermissions.add(permissionFactory.create(deleteAction, documentEntity));
        selectedPermissions.add(permissionFactory.create(readAction, documentEntity));
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(deleteAction, documentEntity));

        Set<Permission> revoked = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

    }

    // different entities
    @Test
    public void test_scenario7() {
        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(createAction, documentEntity));
        currentPermissions.add(permissionFactory.create(readAction, documentEntity));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        currentPermissions.add(permissionFactory.create(createAction, emailEntity));
        currentPermissions.add(permissionFactory.create(readAction, emailEntity));
        currentPermissions.add(permissionFactory.create(updateAction, emailEntity));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(deleteAction, documentEntity));

        selectedPermissions.add(permissionFactory.create(deleteAction, emailEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(deleteAction, documentEntity));
        granted.add(permissionFactory.create(deleteAction, emailEntity));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(createAction, documentEntity));
        revoked.add(permissionFactory.create(readAction, documentEntity));
        revoked.add(permissionFactory.create(updateAction, documentEntity));

        revoked.add(permissionFactory.create(createAction, emailEntity));
        revoked.add(permissionFactory.create(readAction, emailEntity));
        revoked.add(permissionFactory.create(updateAction, emailEntity));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromSelected(currentPermissions, selectedPermissions).get(0));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), selectedPermissions).get(0));

    }

    @Test
    public void test_scenario8() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> currentDataPermissions = new HashSet<Permission>();
        currentDataPermissions.add(permissionFactory.create(updateAction, document1Entity));

        Set<Permission> currentAllPermissions = new HashSet<Permission>();
        currentAllPermissions.addAll(currentDataPermissions);
        currentAllPermissions.addAll(currentPermissions);

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> replaced = new HashSet<Permission>();
        replaced.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> replacedAll = new HashSet<Permission>();
        replacedAll.add(permissionFactory.create(updateAction, document1Entity));
        replacedAll.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, selectedPermissions));

        assertSetsEquals(replacedAll, permissionHandlingUtils.getReplacedByGranting(currentAllPermissions, selectedPermissions));

    }

    @Test
    public void test_scenario9() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        currentPermissions.add(permissionFactory.create(readAction, documentEntityContentField));

        Set<Permission> selectedPermissions = new HashSet<Permission>();
        selectedPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        selectedPermissions.add(permissionFactory.create(readAction, documentEntityContentField));

        Set<Permission> replaced = new HashSet<Permission>();

        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, selectedPermissions));
    }

    // Scenario: test when groups are selected to be added. they come with a set of permissions to grant if
    // possible.

    @Test
    public void test_scenario_add_to_group_1() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, groupPermissionsToGrant).get(0));
        assertSetsEquals(new HashSet<Permission>(), permissionHandlingUtils.getGrantableFromSelected(currentPermissions, groupPermissionsToGrant).get(1));
    }

    @Test
    public void test_scenario_add_to_group_2() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> notGranted = new HashSet<Permission>();
        notGranted.add(permissionFactory.create(updateAction, documentEntityContentField));

        assertSetsEquals(new HashSet<Permission>(), permissionHandlingUtils.getGrantableFromSelected(currentPermissions, groupPermissionsToGrant).get(0));
        assertSetsEquals(notGranted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, groupPermissionsToGrant).get(1));
    }

    @Test
    public void test_scenario_add_to_group_3() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        Set<Permission> granted = new HashSet<Permission>();
        granted.addAll(groupPermissionsToGrant);

        Set<Permission> finalGranted = new HashSet<Permission>();
        finalGranted.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> finalRevoked = new HashSet<Permission>();
        finalRevoked.addAll(currentPermissions);

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, groupPermissionsToGrant).get(0));
        assertSetsEquals(finalRevoked, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, new HashSet<Permission>(), granted).get(0));
        assertSetsEquals(finalGranted, permissionHandlingUtils.getRevokedAndGrantedAfterMerge(currentPermissions, new HashSet<Permission>(), granted).get(1));

    }

    // Scenario: test when groups are unselected-> they come with a collection of permissions to be revoked
    // entity-field not possible, must be forced
    @Test
    public void test_scenario_remove_from_group_1() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> revoked = new HashSet<Permission>();

        Set<Permission> notRevoked = new HashSet<Permission>();
        notRevoked.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityTitleField));
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke).get(1));

    }

    @Test
    public void test_scenario_remove_from_group_2() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityTitleField));
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(granted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));

    }

    @Test
    public void test_scenario_remove_from_group_3() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(granted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));
    }

    @Test
    public void test_scenario_remove_from_group_4() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.addAll(currentPermissions);

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(granted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));
    }

    @Test
    public void test_eliminiate_conflicts_1() {

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> expected = new HashSet<Permission>();

        assertSetsEquals(expected, permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke));
    }

    @Test
    public void test_eliminiate_conflicts_2() {

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionFactory.create(updateAction, documentEntityTitleField));

        assertSetsEquals(expected, permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke));
    }

    @Test
    public void test_eliminiate_conflicts_3() {

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> expected = new HashSet<Permission>();

        assertSetsEquals(expected, permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke));
    }

    @Test
    public void test_eliminiate_conflicts_33() {

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> expected = new HashSet<Permission>();

        assertSetsEquals(expected, permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke));
    }

    @Test
    public void test_eliminiate_conflicts_4() {

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityTitleField));
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntityTitleField));
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntityContentField));
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));
        expected.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        assertSetsEquals(expected, permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke));
    }

    // test cases for Users->Groups. The scenario is that user has a set of current permissions, granted permissions and revoked
    // permission represent the current permissions of the selected added and removed groups.

    // add and remove groups
    @Test
    public void test_scenario_add_and_remove_from_group1() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntityTitleField));

        groupPermissionsToRevoke = permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke);

        Set<Permission> replaced = new HashSet<Permission>();
        replaced.addAll(currentPermissions);

        Set<Permission> revoked = new HashSet<Permission>();

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> additionalGranted = new HashSet<Permission>();

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, groupPermissionsToGrant).get(0));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, groupPermissionsToGrant));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(additionalGranted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));
    }

    @Test
    public void test_scenario_add_and_remove_from_group2() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        groupPermissionsToRevoke = permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke);

        Set<Permission> replaced = new HashSet<Permission>();

        Set<Permission> revoked = new HashSet<Permission>();

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();

        Set<Permission> additionalGranted = new HashSet<Permission>();

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(currentPermissions, groupPermissionsToGrant).get(0));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, groupPermissionsToGrant));

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(additionalGranted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));
    }

    @Test
    public void test_scenario_add_and_remove_from_group3() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        groupPermissionsToRevoke = permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke);

        Set<Permission> replaced = new HashSet<Permission>();

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> additionalGranted = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(additionalGranted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), groupPermissionsToGrant).get(0));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, groupPermissionsToGrant));

    }

    // user has document content field-> loses it because of removing from group with entity permission, but gets it again from
    // the added group-> it is revoked and granted at the same time! one can choose
    @Test
    public void test_scenario_add_and_remove_from_group4() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        groupPermissionsToRevoke = permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke);

        Set<Permission> replaced = new HashSet<Permission>();

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntityContentField));

        Set<Permission> additionalGranted = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(additionalGranted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), groupPermissionsToGrant).get(0));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, groupPermissionsToGrant));

    }

    @Test
    public void test_scenario_add_and_remove_from_group5() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityContentField));
        currentPermissions.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        groupPermissionsToRevoke = permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke);

        Set<Permission> replaced = new HashSet<Permission>();

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntityContentField));
        revoked.add(permissionFactory.create(updateAction, documentEntityTitleField));

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("id")));

        Set<Permission> additionalGranted = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(additionalGranted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), groupPermissionsToGrant).get(0));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, groupPermissionsToGrant));

    }

    @Test
    public void test_scenario_add_and_remove_from_group6() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        groupPermissionsToRevoke = permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke);

        Set<Permission> replaced = new HashSet<Permission>();

        Set<Permission> revoked = new HashSet<Permission>();
        revoked.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();
        granted.add(permissionFactory.create(updateAction, documentEntity.getChild("size")));

        Set<Permission> additionalGranted = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(additionalGranted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), groupPermissionsToGrant).get(0));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, groupPermissionsToGrant));

    }

    @Test
    public void test_scenario_add_and_remove_from_group7() {

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToGrant = new HashSet<Permission>();
        groupPermissionsToGrant.add(permissionFactory.create(updateAction, documentEntity));

        Set<Permission> groupPermissionsToRevoke = new HashSet<Permission>();
        groupPermissionsToRevoke.add(permissionFactory.create(updateAction, documentEntity));

        groupPermissionsToRevoke = permissionHandlingUtils.getRevokedByEliminatingConflicts(groupPermissionsToGrant, groupPermissionsToRevoke);

        Set<Permission> replaced = new HashSet<Permission>();

        Set<Permission> revoked = new HashSet<Permission>();

        Set<Permission> notRevoked = new HashSet<Permission>();

        Set<Permission> granted = new HashSet<Permission>();

        Set<Permission> additionalGranted = new HashSet<Permission>();

        assertSetsEquals(revoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(0));
        assertSetsEquals(notRevoked, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(1));
        assertSetsEquals(additionalGranted, permissionHandlingUtils.getRevokableFromRevoked(currentPermissions, groupPermissionsToRevoke, true).get(2));

        assertSetsEquals(granted, permissionHandlingUtils.getGrantableFromSelected(permissionHandlingUtils.removeAll(currentPermissions, revoked), groupPermissionsToGrant).get(0));
        assertSetsEquals(replaced, permissionHandlingUtils.getReplacedByGranting(currentPermissions, groupPermissionsToGrant));

    }

    private void assertSetsEquals(Set<Permission> expected, Set<Permission> actual) {
        assertTrue(expected.size() == actual.size());
        assertTrue(expected.containsAll(actual));
    }

}
