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
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.CarrierTeam;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.impl.model.sample.TestCarrier;

/**
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class EntitySaveTest extends BaseTest<EntitySaveTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionService permissionService;
    private TestCarrier carrier;

    @Before
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        super.initData();
        carrier = new TestCarrier();
        carrier.setField("field1");
        setUserContext(admin);
        ChangeInterceptor.activate();
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
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        setUserContext(user1);
        self.get().persist(carrier);
        assertNotNull(carrier.getId());
    }

    // One2One
    @Test
    public void test_create_entity_with_one_to_one_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class));
        permissionService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "party"));
        setUserContext(user1);
        
        Party party = new Party();
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);
        
        assertNotNull(carrier.getId());
    }

    // One2One+Cascade.ALL
    @Test
    public void test_create_entity_with_one_to_one_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class));
        permissionService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "partyWithCascade"));
        setUserContext(user1);

        Party party = new Party();
        carrier.setPartyWithCascade(party);
        self.get().persist(carrier);
        
        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_one_field_not_permitted_field_update_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class));
        setUserContext(user1);
        
        Party party = new Party();
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);
        
        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_one_field_not_permitted_related_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        setUserContext(user1);
        
        Party party = new Party();
        self.get().persist(party);
        carrier.setParty(party);
        
        self.get().persist(carrier);
    }

    // One2One+Cascade.ALL
    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_one_field_with_cascade_not_permitted_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "partyWithCascade"));
        setUserContext(user1);

        Party party = new Party();
        carrier.setPartyWithCascade(party);
        self.get().persist(carrier);
    }

    // One2Many
    @Test
    public void test_create_entity_with_one_to_many_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "contacts"));
        setUserContext(user1);

        Contact contact = new Contact();
        contact.setContactField("contact_field");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_one_to_many_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Document.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "documents"));
        setUserContext(user1);

        Document document = new Document();
        carrier.getDocuments().add(document);
        self.get().persist(carrier);
        
        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_many_field_not_permitted_add_field_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        setUserContext(user1);

        Contact contact = new Contact();
        contact.setContactField("contact_field");
        self.get().persist(contact);
        carrier.getContacts().add(contact);

        self.get().persist(carrier);
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_many_field_with_cascade_not_permitted_add_field_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Document.class));
        setUserContext(user1);

        Document document = new Document();
        carrier.getDocuments().add(document);
        self.get().persist(carrier);
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_many_field_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "contacts"));
        setUserContext(user1);
        
        Contact contact = new Contact();
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_one_to_many_field_with_cascade_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "documents"));
        setUserContext(user1);

        Document document = new Document();
        carrier.getDocuments().add(document);
        self.get().persist(carrier);
    }

    @Test
    public void test_create_entity_with_one_to_many_field_more_entries() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "contacts"));
        setUserContext(user1);
        
        Contact contact = new Contact();
        self.get().persist(contact);
        
        Contact contact1 = new Contact();
        self.get().persist(contact1);
        
        carrier.getContacts().add(contact);
        carrier.getContacts().add(contact1);
        carrier.getContacts().remove(contact);
        
        Contact contact2 = new Contact();
        self.get().persist(contact2);
        
        carrier.getContacts().add(contact2);
        self.get().persist(carrier);
        
        assertNotNull(carrier.getId());
    }

    // Many2Many
    @Test
    public void test_create_entity_with_many_to_many_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierGroup.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "groups"));
        setUserContext(user1);
        
        CarrierGroup group = new CarrierGroup();
        self.get().persist(group);
        carrier.getGroups().add(group);
        self.get().persist(carrier);
        
        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_many_to_many_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierTeam.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "teams"));
        setUserContext(user1);

        CarrierTeam team = new CarrierTeam();
        carrier.getTeams().add(team);
        self.get().persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_many_field_not_permitted_update_field_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierGroup.class));
        setUserContext(user1);
        
        CarrierGroup group = new CarrierGroup();
        self.get().persist(group);
        carrier.getGroups().add(group);
        self.get().persist(carrier);
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_many_field_with_cascade_not_permitted_update_field_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierTeam.class));
        setUserContext(user1);
        
        CarrierTeam team = new CarrierTeam();
        carrier.getTeams().add(team);
        self.get().persist(carrier);
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_many_field_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "groups"));
        setUserContext(user1);
        
        CarrierGroup group = new CarrierGroup();
        self.get().persist(group);
        carrier.getGroups().add(group);
        self.get().persist(carrier);
    }
    
    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_many_field_with_cascade_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "teams"));
        setUserContext(user1);
        
        CarrierTeam team = new CarrierTeam();
        carrier.getTeams().add(team);
        self.get().persist(carrier);
    }

    // Many2One
    @Test
    public void test_create_entity_with_many_to_one_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Email.class));
        permissionService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "email"));
        setUserContext(user1);
        
        Email email = new Email();
        self.get().persist(email);
        carrier.setEmail(email);
        self.get().persist(carrier);
        
        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_many_to_one_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Email.class));
        permissionService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "emailWithCascade"));
        setUserContext(user1);

        Email email = new Email();
        carrier.setEmailWithCascade(email);
        self.get().persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_one_field_not_permitted_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "email"));
        setUserContext(user1);

        Email email = new Email();
        self.get().persist(email);
        carrier.setEmail(email);
        self.get().persist(carrier);
    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_one_field_with_cascade_not_permitted_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "emailWithCascade"));
        setUserContext(user1);

        Email email = new Email();
        carrier.setEmailWithCascade(email);
        self.get().persist(carrier);

    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_one_field_not_permitted_field_update_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Email.class));
        setUserContext(user1);
        
        Email email = new Email();
        self.get().persist(email);
        carrier.setEmail(email);
        self.get().persist(carrier);

    }

    @Test(expected = PermissionException.class)
    public void test_create_entity_with_many_to_one_field_with_cascade_not_permitted_field_update_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Email.class));
        setUserContext(user1);

        Email email = new Email();
        carrier.setEmailWithCascade(email);
        self.get().persist(carrier);

    }

}
