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

import static org.junit.Assert.assertNotNull;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

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
public class EntitySaveTest extends BaseTest<EntitySaveTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionService securityService;
    private Carrier carrier;

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
        setUserContext(admin);
        ChangeInterceptor.activatePersist();
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
    public void remove(Object object) {
        entityManager.remove(object);
    }

    @Test
    public void test_create_entity_with_primitive_field() {
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        self.get().persist(carrier);
        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_one_to_one_field() {
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);
        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_one_field_not_permitted() {
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

    }

    @Test
    public void test_create_entity_with_one_to_many_field() {
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        Contact contact = new Contact();
        contact.setContactField("contact_field");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);
        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_many_to_many_field() {
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        CarrierGroup group = new CarrierGroup();
        group.setName("group");
        self.get().persist(group);
        carrier.getGroups().add(group);
        self.get().persist(carrier);
        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_many_field_not_permitted() {
        securityService.grant(admin, user1, actionFactory.createAction(ActionConstants.CREATE), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        CarrierGroup group = new CarrierGroup();
        group.setName("group");
        self.get().persist(group);
        carrier.getGroups().add(group);
        self.get().persist(carrier);
        assertNotNull(carrier.getId());
    }
}
