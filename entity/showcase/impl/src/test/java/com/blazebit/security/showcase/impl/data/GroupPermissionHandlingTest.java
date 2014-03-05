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
package com.blazebit.security.showcase.impl.data;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;

import com.blazebit.security.data.PermissionHandling;
import com.blazebit.security.entity.EntityResourceFactory;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.UserGroup;
import com.blazebit.security.model.sample.Carrier;
import com.blazebit.security.model.sample.CarrierGroup;
import com.blazebit.security.model.sample.Comment;
import com.blazebit.security.model.sample.Document;
import com.blazebit.security.showcase.data.GroupPermissionDataAccess;
import com.blazebit.security.showcase.data.UserGroupDataAccess;
import com.blazebit.security.spi.PermissionFactory;
import com.blazebit.security.test.BeforeDatabaseAware;
import com.blazebit.security.test.DatabaseAware;

/**
 * 
 * @author cuszk
 */
@DatabaseAware
public class GroupPermissionHandlingTest extends BaseTest<GroupPermissionHandlingTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionHandling permissionHandlingUtils;
    @Inject
    private GroupPermissionDataAccess groupPermissionHandling;
    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private UserGroupDataAccess userGroupDataAcces;
    @Inject
    private EntityResourceFactory entityResourceFactory;

    UserGroup userGroupE;

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
        merge(userGroupC);

        userGroupE = new UserGroup("UserGroup E");
        persist(userGroupE);

        setUserContext(admin);
    }

    // add remove user from groups
    @Test
    public void test_add_remove_from_groups1() {
        // user is in A and B
        user1.getUserGroups().add(userGroupA);
        userGroupA.getUsers().add(user1);
        merge(userGroupA);
        user1.getUserGroups().add(userGroupB);
        userGroupB.getUsers().add(user1);
        merge(userGroupB);
        merge(user1);

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
        merge(userGroupA);
        user1.getUserGroups().add(userGroupB);
        userGroupB.getUsers().add(user1);
        merge(userGroupB);
        merge(user1);

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
        merge(userGroupA);
        user1.getUserGroups().add(userGroupC);
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        merge(user1);

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

        persist(permissionFactory.create(userGroupA, updateAction, entityResourceFactory.createResource(Carrier.class)));
        persist(permissionFactory.create(userGroupB, updateAction, entityResourceFactory.createResource(CarrierGroup.class)));
        persist(permissionFactory.create(userGroupC, updateAction, entityResourceFactory.createResource(Document.class)));
        persist(permissionFactory.create(userGroupD, updateAction, entityResourceFactory.createResource(Comment.class)));

        user1.getUserGroups().add(userGroupA);
        userGroupA.getUsers().add(user1);
        merge(userGroupA);
        user1.getUserGroups().add(userGroupC);
        userGroupC.getUsers().add(user1);
        merge(userGroupC);
        merge(user1);

        persist(permissionFactory.create(user1, updateAction, entityResourceFactory.createResource(Carrier.class)));
        persist(permissionFactory.create(user1, updateAction, entityResourceFactory.createResource(Document.class)));

        Set<Permission> currentPermissions = new HashSet<Permission>();
        currentPermissions.add(permissionFactory.create(user1, updateAction,
                                                        entityResourceFactory.createResource(Carrier.class)));
        currentPermissions.add(permissionFactory.create(user1, updateAction,
                                                        entityResourceFactory.createResource(Document.class)));

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
        expectedGranted.add(permissionFactory.create(user1, updateAction,
                                                     entityResourceFactory.createResource(CarrierGroup.class)));

        Set<Permission> expectedNotGranted = new HashSet<Permission>();
        expectedNotGranted.add(permissionFactory.create(user1, updateAction,
                                                        entityResourceFactory.createResource(Carrier.class)));

        Set<Permission> expectedRevoked = new HashSet<Permission>();
        expectedRevoked
            .add(permissionFactory.create(user1, updateAction, entityResourceFactory.createResource(Document.class)));

        assertSetsContains(expectedGranted, actualGranted);
        assertSetsContains(expectedNotGranted, notGranted);
        assertSetsContains(expectedRevoked, revoked);

    }

    // field permissions in different groups-> when adding to both -> merge into entity permission
    @Test
    public void test_selectedGroups_grant_revoke_permissions2() {

        persist(permissionFactory.create(userGroupA, updateAction, documentEntityContentField));
        persist(permissionFactory.create(userGroupA, updateAction, documentEntity.withField("size")));
        persist(permissionFactory.create(userGroupB, updateAction, documentEntityTitleField));
        persist(permissionFactory.create(userGroupB, updateAction, documentEntity.withField("id")));

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

        persist(permissionFactory.create(userGroupA, updateAction, documentEntityContentField));
        persist(permissionFactory.create(userGroupA, updateAction, documentEntity.withField("size")));
        persist(permissionFactory.create(userGroupB, updateAction, documentEntityTitleField));
        persist(permissionFactory.create(userGroupB, updateAction, documentEntity.withField("id")));

        // user1 has already a field permission
        persist(permissionFactory.create(user1, updateAction, documentEntityContentField));

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
        // Set<Permission> notGranted = grant.get(1);
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
