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

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.service.PermissionHandlingImpl;
import com.blazebit.security.impl.service.resource.GroupPermissionHandling;
import com.blazebit.security.impl.service.resource.UserGroupDataAccess;

/**
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class GroupPermissionHandlingUtilsTest extends BaseTest<GroupPermissionHandlingUtilsTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionHandlingImpl permissionHandlingUtils;
    @Inject
    private GroupPermissionHandling groupPermissionHandling;
    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private UserGroupDataAccess userGroupDataAcces;

    UserGroup userGroupE;

    @Before
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        super.initData();

        userGroupA.getUserGroups().add(userGroupB);
        userGroupB.setParent(userGroupA);
        self.get().merge(userGroupB);

        userGroupB.getUserGroups().add(userGroupC);
        userGroupC.setParent(userGroupB);
        self.get().merge(userGroupC);

        userGroupC.getUserGroups().add(userGroupD);
        userGroupD.setParent(userGroupC);
        self.get().merge(userGroupC);

        userGroupE = new UserGroup("UserGroup E");
        self.get().persist(userGroupE);

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

    // add remove user from groups
    @Test
    public void test_add_remove_from_groups1() {
        // user is in A and B
        user1.getUserGroups().add(userGroupA);
        userGroupA.getUsers().add(user1);
        self.get().merge(userGroupA);
        user1.getUserGroups().add(userGroupB);
        userGroupB.getUsers().add(user1);
        self.get().merge(userGroupB);
        self.get().merge(user1);

        // add to D, remove from B, keep A
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroupA);
        selectedGroups.add(userGroupD);

        Set<UserGroup> expectedAdded = new HashSet<UserGroup>();
        expectedAdded.add(userGroupD);

        Set<UserGroup> expectedRemoved = new HashSet<UserGroup>();
        expectedRemoved.add(userGroupB);

        Set<UserGroup> added = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(0);
        Set<UserGroup> removed = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(1);

        assertSetsEquals(expectedAdded, added);
        assertSetsEquals(expectedRemoved, removed);
    }

    @Test
    public void test_add_remove_from_groups2() {
        user1.getUserGroups().add(userGroupA);
        userGroupA.getUsers().add(user1);
        self.get().merge(userGroupA);
        user1.getUserGroups().add(userGroupB);
        userGroupB.getUsers().add(user1);
        self.get().merge(userGroupB);
        self.get().merge(user1);

        // remove from A, B add to C
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroupC);

        Set<UserGroup> expectedAdded = new HashSet<UserGroup>();
        expectedAdded.add(userGroupC);

        Set<UserGroup> expectedRemoved = new HashSet<UserGroup>();
        expectedRemoved.add(userGroupB);
        expectedRemoved.add(userGroupA);

        Set<UserGroup> added = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(0);
        Set<UserGroup> removed = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(1);

        assertSetsEquals(expectedAdded, added);
        assertSetsEquals(expectedRemoved, removed);
    }

    @Test
    public void test_add_remove_from_groups3() {

        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroupD);

        Set<UserGroup> expectedAdded = new HashSet<UserGroup>();
        expectedAdded.add(userGroupD);

        Set<UserGroup> expectedRemoved = new HashSet<UserGroup>();

        Set<UserGroup> added = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(0);
        Set<UserGroup> removed = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(1);

        assertSetsEquals(expectedAdded, added);
        assertSetsEquals(expectedRemoved, removed);
    }

    @Test
    public void test_add_remove_from_groups4() {
        user1.getUserGroups().add(userGroupA);
        userGroupA.getUsers().add(user1);
        self.get().merge(userGroupA);
        user1.getUserGroups().add(userGroupC);
        userGroupC.getUsers().add(user1);
        self.get().merge(userGroupC);
        self.get().merge(user1);

        // remove from A, B add to C
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroupA);
        selectedGroups.add(userGroupC);

        Set<UserGroup> expectedAdded = new HashSet<UserGroup>();

        Set<UserGroup> expectedRemoved = new HashSet<UserGroup>();

        Set<UserGroup> added = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(0);
        Set<UserGroup> removed = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(1);

        assertSetsEquals(expectedAdded, added);
        assertSetsEquals(expectedRemoved, removed);
    }

    // check permissions to grant and revoke when adding/removing groups
    @Test
    public void test_selectedGroups_grant_revoke_permissions1() {

        self.get().persist(permissionFactory.create(userGroupA, updateAction, entityFieldFactory.createResource(Carrier.class)));
        self.get().persist(permissionFactory.create(userGroupB, updateAction, entityFieldFactory.createResource(CarrierGroup.class)));
        self.get().persist(permissionFactory.create(userGroupC, updateAction, entityFieldFactory.createResource(Document.class)));
        self.get().persist(permissionFactory.create(userGroupD, updateAction, entityFieldFactory.createResource(Comment.class)));

        user1.getUserGroups().add(userGroupA);
        userGroupA.getUsers().add(user1);
        self.get().merge(userGroupA);
        user1.getUserGroups().add(userGroupC);
        userGroupC.getUsers().add(user1);
        self.get().merge(userGroupC);
        self.get().merge(user1);

        self.get().persist(permissionFactory.create(user1, updateAction, entityFieldFactory.createResource(Carrier.class)));
        self.get().persist(permissionFactory.create(user1, updateAction, entityFieldFactory.createResource(Document.class)));

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(user1, updateAction, entityFieldFactory.createResource(Carrier.class)));
        currentPermissions.add(permissionFactory.create(user1, updateAction, entityFieldFactory.createResource(Document.class)));

        // remove from C, add to B, keep A
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroupA);
        selectedGroups.add(userGroupB);

        Set<UserGroup> added = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(0);
        Set<UserGroup> removed = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(1);

        Set<Permission> granted = groupPermissionHandling.getGroupPermissions(added);
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantable(currentPermissions, granted);
        Set<Permission> actualGranted = grant.get(0);
        Set<Permission> notGranted = grant.get(1);
        Set<Permission> revoked = groupPermissionHandling.getGroupPermissions(removed);
        revoked = permissionHandlingUtils.eliminateRevokeConflicts(granted, revoked);

        Set<Permission> expectedGranted = new HashSet<Permission>();
        expectedGranted.add(permissionFactory.create(user1, updateAction, entityFieldFactory.createResource(CarrierGroup.class)));

        Set<Permission> expectedNotGranted = new HashSet<Permission>();
        expectedNotGranted.add(permissionFactory.create(user1, updateAction, entityFieldFactory.createResource(Carrier.class)));

        Set<Permission> expectedRevoked = new HashSet<Permission>();
        expectedRevoked.add(permissionFactory.create(user1, updateAction, entityFieldFactory.createResource(Document.class)));

        assertSetsContains(expectedGranted, actualGranted);
        assertSetsContains(expectedNotGranted, notGranted);
        assertSetsContains(expectedRevoked, revoked);

    }

    // field permissions in different groups-> when adding to both -> merge into entity permission
    @Test
    public void test_selectedGroups_grant_revoke_permissions2() {

        self.get().persist(permissionFactory.create(userGroupA, updateAction, documentEntityContentField));
        self.get().persist(permissionFactory.create(userGroupA, updateAction, documentEntity.getChild("size")));
        self.get().persist(permissionFactory.create(userGroupB, updateAction, documentEntityTitleField));
        self.get().persist(permissionFactory.create(userGroupB, updateAction, documentEntity.getChild("id")));

        Set<Permission> currentPermissions = new HashSet<Permission>();

        // add to A+B
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroupA);
        selectedGroups.add(userGroupB);

        Set<UserGroup> added = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(0);
        Set<UserGroup> removed = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(1);

        Set<Permission> granted = groupPermissionHandling.getGroupPermissions(added);
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantable(currentPermissions, granted);
        Set<Permission> actualGranted = grant.get(0);

        Set<Permission> revoked = groupPermissionHandling.getGroupPermissions(removed);
        revoked = permissionHandlingUtils.eliminateRevokeConflicts(granted, revoked);

        Set<Permission> expectedGranted = new HashSet<Permission>();
        expectedGranted.add(permissionFactory.create(user1, updateAction, documentEntity));

        Set<Permission> expectedRevoked = new HashSet<Permission>();

        assertSetsContains(expectedGranted, actualGranted);
        assertSetsContains(expectedRevoked, revoked);

    }

    // field permissions in different groups-> user has already one field-> will be replaced
    @Test
    public void test_selectedGroups_grant_revoke_permissions3() {

        self.get().persist(permissionFactory.create(userGroupA, updateAction, documentEntityContentField));
        self.get().persist(permissionFactory.create(userGroupA, updateAction, documentEntity.getChild("size")));
        self.get().persist(permissionFactory.create(userGroupB, updateAction, documentEntityTitleField));
        self.get().persist(permissionFactory.create(userGroupB, updateAction, documentEntity.getChild("id")));

        // user1 has already a field permission
        self.get().persist(permissionFactory.create(user1, updateAction, documentEntityContentField));

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(user1, updateAction, documentEntityContentField));

        // remove from C, add to A
        Set<UserGroup> selectedGroups = new HashSet<UserGroup>();
        selectedGroups.add(userGroupA);
        selectedGroups.add(userGroupB);

        Set<UserGroup> added = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(0);
        Set<UserGroup> removed = userGroupDataAcces.getAddedAndRemovedUserGroups(user1, selectedGroups).get(1);

        Set<Permission> granted = groupPermissionHandling.getGroupPermissions(added);
        List<Set<Permission>> grant = permissionHandlingUtils.getGrantable(currentPermissions, granted);
        Set<Permission> actualGranted = grant.get(0);
        Set<Permission> notGranted = grant.get(1);
        Set<Permission> revoked = groupPermissionHandling.getGroupPermissions(removed);
        revoked = permissionHandlingUtils.eliminateRevokeConflicts(granted, revoked);

        Set<Permission> expectedGranted = new HashSet<Permission>();
        expectedGranted.add(permissionFactory.create(user1, updateAction, documentEntity));

        Set<Permission> expectedRevoked = new HashSet<Permission>();

        assertSetsContains(expectedGranted, actualGranted);
        assertSetsContains(expectedRevoked, revoked);

    }

    private void assertSetsEquals(Set<?> expected, Set<?> actual) {
        assertTrue(expected.size() == actual.size());
        assertTrue(expected.containsAll(actual));
    }

    private void assertSetsContains(Set<Permission> expected, Set<Permission> actual) {
        assertTrue(expected.size() == actual.size());
        assertTrue(permissionHandlingUtils.containsAll(expected, actual));
    }

}
