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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.model.EntityField;
import com.blazebit.security.test.BeforeDatabaseAware;
import com.blazebit.security.test.DatabaseAware;

/**
 * 
 * @author cuszk
 */
@DatabaseAware
public class EntityFieldTest extends BaseTest<EntityFieldTest> {

    private static final long serialVersionUID = 1L;

    @BeforeDatabaseAware
    public void init() {
        super.initData();
        setUserContext(admin);
    }

    // parent-child
    @Test
    public void test_parent_relation_of_entityField() {
        assertEquals(documentEntityTitleField.getParent(), documentEntity);
        assertEquals(documentEntityContentField.getParent(), documentEntity);
    }

    @Test
    public void test_parent_relation_of_entityField_false() {
        assertNotEquals(documentEntityTitleField.getParent(), emailEntity);
        assertNotEquals(documentEntityTitleField.getParent(), document1EntityContentField);
    }

    @Test
    public void test_child_relation_of_entityField() {
        assertEquals(documentEntity.withField("title"), documentEntityTitleField);
        assertEquals(documentEntity.withField("content"), documentEntityContentField);
    }

    @Test
    public void test_child_relation_of_entityField_false() {
        assertNotEquals(documentEntity.withField("title"), documentEntityContentField);
        assertNotEquals(documentEntity.withField("content"), documentEntity);
    }

    @Test
    public void test_parent_relation_of_entityObjectField() {
        assertEquals(document1EntityTitleField.getParent(), document1Entity);
    }

    @Test
    public void test_child_relation_of_entityObjectField() {
        assertEquals(document1Entity.withField("title"), document1EntityTitleField);
    }

    // implies
    @Test
    public void test_entity_implies_entity() {
        assertTrue(documentEntity.implies(documentEntity));
    }

    @Test
    public void test_entity_field_implies_entity_field() {
        assertTrue(documentEntityTitleField.implies(documentEntityTitleField));
    }

    @Test
    public void test_entity_object_implies_entity_object() {
        assertTrue(document1Entity.implies(document1Entity));
    }

    @Test
    public void test_entity_object_field_implies_entity_object_field() {
        assertTrue(document1EntityTitleField.implies(document1EntityTitleField));
    }

    @Test
    public void test_entity_implies_field() {
        assertTrue(documentEntity.implies(documentEntityTitleField));
    }

    @Test
    public void test_entity_implies_object() {
        assertTrue(documentEntity.implies(document1Entity));
    }

    @Test
    public void test_entity_implies_object_field() {
        assertTrue(documentEntity.implies(document1EntityTitleField));
    }

    @Test
    public void test_entity_field_implies_object_field() {
        assertTrue(documentEntityTitleField.implies(document1EntityTitleField));
    }

    @Test
    public void test_entity_object_implies_object_field() {
        assertTrue(document1Entity.implies(document1EntityTitleField));
    }

    // isEmptyField
    @Test
    public void test_isEmtptyField_entity() {
        assertEquals(EntityField.EMPTY_FIELD, documentEntity.getField());
    }

    @Test
    public void test_isEmtptyField_entityObject() {
        assertEquals(EntityField.EMPTY_FIELD, document1Entity.getField());
    }

    @Test
    public void test_isEmtptyField_entity_field_false() {
        assertNotEquals(EntityField.EMPTY_FIELD, documentEntityTitleField.getField());
    }

    @Test
    public void test_isEmtptyField_entityObject_field_false() {
        assertNotEquals(EntityField.EMPTY_FIELD, document1EntityTitleField.getField());
    }

    // isReplaceableBy
    @Test
    public void test_entityField_replaceableBy_entity() {
        assertTrue(documentEntityTitleField.isReplaceableBy(documentEntity));
    }

    @Test
    public void test_entityObject_replaceableBy_entity() {
        assertTrue(document1Entity.isReplaceableBy(documentEntity));
    }

    @Test
    public void test_entityObjectField_replaceableBy_entity() {
        assertTrue(document1EntityTitleField.isReplaceableBy(documentEntity));
    }

    @Test
    public void test_entityObjectField_replaceableBy_entityObject() {
        assertTrue(document1EntityTitleField.isReplaceableBy(document1Entity));
    }

    @Test
    public void test_entityObjectField_replaceableBy_entityField() {
        assertTrue(document1EntityTitleField.isReplaceableBy(documentEntityTitleField));
    }

    // isApplicableAction
    @Test
    public void test_applicable_entity_action() {
        assertTrue(documentEntity.isApplicable(getCreateAction()));
        assertTrue(documentEntity.isApplicable(getUpdateAction()));
        assertTrue(documentEntity.isApplicable(getDeleteAction()));

        assertTrue(documentEntity.isApplicable(getReadAction()));
        assertTrue(documentEntity.isApplicable(getGrantAction()));
        assertTrue(documentEntity.isApplicable(getRevokeAction()));

        assertFalse(documentEntity.isApplicable(getAddAction()));
        assertFalse(documentEntity.isApplicable(getRemoveAction()));
    }

    @Test
    public void test_applicable_entityField_action() {
        assertTrue(documentEntityTitleField.isApplicable(getCreateAction()));
        assertTrue(documentEntityTitleField.isApplicable(getUpdateAction()));
        assertFalse(documentEntityTitleField.isApplicable(getDeleteAction()));

        assertTrue(documentEntityTitleField.isApplicable(getReadAction()));
        assertTrue(documentEntityTitleField.isApplicable(getGrantAction()));
        assertTrue(documentEntityTitleField.isApplicable(getRevokeAction()));

        assertTrue(documentEntityTitleField.isApplicable(getAddAction()));
        assertTrue(documentEntityTitleField.isApplicable(getRemoveAction()));
    }

    @Test
    public void test_applicable_entityObject_action() {
        assertFalse(document1Entity.isApplicable(getCreateAction()));
        assertTrue(document1Entity.isApplicable(getUpdateAction()));
        assertTrue(document1Entity.isApplicable(getDeleteAction()));

        assertTrue(document1Entity.isApplicable(getReadAction()));
        assertTrue(document1Entity.isApplicable(getGrantAction()));
        assertTrue(document1Entity.isApplicable(getRevokeAction()));

        assertFalse(document1Entity.isApplicable(getAddAction()));
        assertFalse(document1Entity.isApplicable(getRemoveAction()));
    }

    @Test
    public void test_applicable_entityObjectField_action() {
        assertFalse(document1EntityTitleField.isApplicable(getCreateAction()));
        assertTrue(document1EntityTitleField.isApplicable(getUpdateAction()));
        assertTrue(document1EntityTitleField.isApplicable(getDeleteAction()));

        assertTrue(document1EntityTitleField.isApplicable(getReadAction()));
        assertTrue(document1EntityTitleField.isApplicable(getGrantAction()));
        assertTrue(document1EntityTitleField.isApplicable(getRevokeAction()));

        assertTrue(document1EntityTitleField.isApplicable(getAddAction()));
        assertTrue(document1EntityTitleField.isApplicable(getRemoveAction()));
    }

    // connected Resources
    @Test
    public void test_connectedResources_entity() {
        assertEquals(1, documentEntity.connectedResources().size());
        assertTrue(documentEntity.connectedResources().contains(documentEntity));
    }
    
    @Test
    public void test_connectedResources_entityField() {
        assertEquals(2, documentEntityTitleField.connectedResources().size());
        assertTrue(documentEntityTitleField.connectedResources().contains(documentEntityTitleField));
        assertTrue(documentEntityTitleField.connectedResources().contains(documentEntity));
    }
    
    @Test
    public void test_connectedResources_entityObject() {
        assertEquals(2, document1Entity.connectedResources().size());
        assertTrue(document1Entity.connectedResources().contains(document1Entity));
        assertTrue(document1Entity.connectedResources().contains(documentEntity));
    }
    
    @Test
    public void test_connectedResources_entityObjectField() {
        assertEquals(4, document1EntityTitleField.connectedResources().size());
        assertTrue(document1EntityTitleField.connectedResources().contains(document1EntityTitleField));
        assertTrue(document1EntityTitleField.connectedResources().contains(documentEntityTitleField));
        assertTrue(document1EntityTitleField.connectedResources().contains(documentEntity));
        assertTrue(document1EntityTitleField.connectedResources().contains(documentEntityTitleField));
    }
}
