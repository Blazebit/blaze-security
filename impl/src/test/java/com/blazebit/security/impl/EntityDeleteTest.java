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

import static org.junit.Assert.assertNull;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.Action;
import com.blazebit.security.IdHolder;
import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionService;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;

/**
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class EntityDeleteTest extends BaseTest<EntityDeleteTest> {

    @Inject
    private PermissionService securityService;
    private Carrier carrier;
    private Carrier carrierWithParty;
    private Carrier carrierWithContacts;
    private Carrier carrierWithGroups;

    @Before
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        super.initData();

        carrier = new Carrier();
        carrier.setField1("field1");
        carrier.setField2("field2");
        carrier.setField3("field3");
        carrier.setField4("field4");
        carrier.setField5(null);
        entityManager.persist(carrier);

        Party party = new Party();
        entityManager.persist(party);
        carrierWithParty = new Carrier();
        carrierWithParty.setParty(party);
        entityManager.persist(carrierWithParty);

        Contact contact = new Contact();
        entityManager.persist(contact);
        carrierWithContacts = new Carrier();
        carrierWithContacts.getContacts().add(contact);
        entityManager.persist(carrierWithContacts);

        CarrierGroup group = new CarrierGroup();
        entityManager.persist(group);
        carrierWithGroups = new Carrier();
        carrierWithGroups.getGroups().add(group);
        entityManager.persist(carrierWithGroups);

        entityManager.flush();
        setUserContext(user1);
        ChangeInterceptor.activateDelete();
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

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(IdHolder object) {
        object = entityManager.find(object.getClass(), object.getId());
        entityManager.remove(object);
    }

    @Test
    public void test_delete_entity_with_primitive_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        self.get().remove(carrier);
        assertNull(entityManager.find(Carrier.class, carrier.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_one_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD));
        self.get().remove(carrierWithParty);
        assertNull(entityManager.find(Carrier.class, carrierWithParty.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_many_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));
        self.get().remove(carrierWithContacts);
        assertNull(entityManager.find(Carrier.class, carrierWithContacts.getId()));
    }

    @Test
    public void test_delete_entity_with_many_to_many_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        self.get().remove(carrierWithGroups);
        assertNull(entityManager.find(Carrier.class, carrierWithGroups.getId()));
    }

    @Test
    public void test_delete_entity_with_primitive_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, carrier.getId()));
        self.get().remove(carrier);
        assertNull(entityManager.find(Carrier.class, carrier.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_one_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, carrierWithParty.getId()));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD, carrierWithParty.getParty().getId()));
        self.get().remove(carrierWithParty);
        assertNull(entityManager.find(Carrier.class, carrierWithParty.getId()));
    }

    @Test(expected = PermissionException.class)
    public void test_delete_entity_with_one_to_one_field_with_missing_party_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, carrierWithParty.getId()));
        self.get().remove(carrierWithParty);
        assertNull(entityManager.find(Carrier.class, carrierWithParty.getId()));
    }

    @Test(expected = PermissionException.class)
    public void test_delete_entity_with_one_to_one_field_with_wrong_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, -1));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD, carrierWithParty.getParty().getId()));
        self.get().remove(carrierWithParty);
        assertNull(entityManager.find(Carrier.class, carrierWithParty.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_many_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, carrierWithContacts.getId()));
        securityService.grant(admin, user1, getDeleteAction(),
                              entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD, carrierWithContacts.getContacts().iterator().next().getId()));
        self.get().remove(carrierWithContacts);
        assertNull(entityManager.find(Carrier.class, carrierWithContacts.getId()));
    }

    @Test
    public void test_delete_entity_with_many_to_many_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(),
                              entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD, carrierWithGroups.getGroups().iterator().next().getId()));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, carrierWithGroups.getId()));
        self.get().remove(carrierWithGroups);
        assertNull(entityManager.find(Carrier.class, carrierWithGroups.getId()));
    }

    private Action getDeleteAction() {
        return actionFactory.createAction(ActionConstants.DELETE);
    }
}
