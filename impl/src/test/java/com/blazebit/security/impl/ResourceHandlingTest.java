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
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.service.PermissionHandlingImpl;
import com.blazebit.security.impl.service.resource.EntityFieldResourceHandling;
import com.blazebit.security.impl.service.resource.EntityFieldResourceHandling.PermissionFamily;

/**
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class ResourceHandlingTest extends BaseTest<ResourceHandlingTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionFactory permissionFactory;
    @Inject
    private EntityFieldResourceHandling resourceHandling;

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

    // these are helpers to create maps
    // groupPermissionsByResourceName(Collection<Permission>)
    // groupEntityPermissionsByField(Collection<Permission>)
    // groupResourcePermissionsByAction(Collection<Permission>)

    // findPermission(Collection<Permission>, Permission)

    // getSeparatedPermissionsByResource(Collection<Permission>) -> separates UserPermissions from UserDataPermissions

    // getSeparatedParentAndChildEntityPermissions(Collection<Permission>)
    // getParentPermissions(Collection<Permission>)
    // getChildPermissions(Action, Resource)

    // separate a collection of permissions with the same resource name into parent and child permissions
    @Test
    public void test_getSeparatedParentAndChildEntityPermissions() {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class)));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "id")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "title")));

        PermissionFamily actual = resourceHandling.getSeparatedParentAndChildEntityPermissions(permissions);
        PermissionFamily expected = resourceHandling.new PermissionFamily();
        expected.parent = permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class));
        expected.children = new HashSet<Permission>();
        expected.children.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "id")));
        expected.children.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "title")));

        assertTrue(actual.equals(expected));
    }

    @Test
    public void test_getSeparatedParentAndChildEntityPermissions1() {
        Set<Permission> permissions = new HashSet<Permission>();

        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "id")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "title")));

        PermissionFamily actual = resourceHandling.getSeparatedParentAndChildEntityPermissions(permissions);
        PermissionFamily expected = resourceHandling.new PermissionFamily();
        expected.children = new HashSet<Permission>();
        expected.children.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "id")));
        expected.children.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "title")));

        assertTrue(actual.equals(expected));
    }

    // child permissions of a parent permission
    @Test
    public void test_getChildPermissions() {

        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "id")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "content")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "title")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "size")));

        assertSetsEquals(permissions,
                         resourceHandling.getChildPermissions(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE),
                                                                                       entityFieldFactory.createResource(Document.class))));

    }

    @Test
    public void test_getChildPermissions1() {

        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "id")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "title")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "size")));

        assertSetsNotEquals(permissions,
                            resourceHandling.getChildPermissions(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE),
                                                                                          entityFieldFactory.createResource(Document.class))));

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getChildPermissions2() {
        resourceHandling.getChildPermissions(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE),
                                                                      entityFieldFactory.createResource(Document.class, "title")));
    }

    // parent permissions
    @Test
    public void test_getParentPermissions() {

        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "id")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "title")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class, "size")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.READ), entityFieldFactory.createResource(Document.class, "size")));
        permissions.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Email.class, "subject")));

        Set<Permission> expected = new HashSet<Permission>();
        expected.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Document.class)));
        expected.add(permissionFactory.create(actionFactory.createAction(ActionConstants.READ), entityFieldFactory.createResource(Document.class)));
        expected.add(permissionFactory.create(actionFactory.createAction(ActionConstants.UPDATE), entityFieldFactory.createResource(Email.class)));

        assertSetsNotEquals(permissions, resourceHandling.getParentPermissions(permissions));

    }

    private void assertSetsEquals(Set<Permission> expected, Set<Permission> actual) {
        assertTrue(expected.size() == actual.size());
        assertTrue(expected.containsAll(actual));
    }

    private void assertSetsNotEquals(Set<Permission> expected, Set<Permission> actual) {
        assertFalse(expected.size() == actual.size());
        assertFalse(expected.containsAll(actual));
    }

}
