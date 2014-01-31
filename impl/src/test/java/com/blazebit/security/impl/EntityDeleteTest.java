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
import static org.junit.Assert.assertNull;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.exception.PermissionActionException;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.CarrierTeam;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Document;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.impl.model.sample.TestCarrier;
import com.blazebit.security.model.IdHolder;
import com.blazebit.security.service.PermissionService;

/**
 * 
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class EntityDeleteTest extends BaseTest<EntityDeleteTest> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionService securityService;
    private TestCarrier carrier;
    // one2one
    private TestCarrier carrierWithParty;
    // +cascade
    private TestCarrier carrierWithPartyCascade;
    // many2one
    private TestCarrier carrierWithEmail;
    // +cascade
    private TestCarrier carrierWithEmailCascade;
    // one2many
    private TestCarrier carrierWithContacts;
    // +cascade
    private TestCarrier carrierWithDocuments;
    // many2many
    private TestCarrier carrierWithGroups;
    // +cascade
    private TestCarrier carrierWithTeams;

    @Before
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        super.initData();

        carrier = new TestCarrier();
        carrier.setField("field1");
        entityManager.persist(carrier);
        // one2one
        Party party = new Party();
        entityManager.persist(party);
        carrierWithParty = new TestCarrier();
        carrierWithParty.setParty(party);
        entityManager.persist(carrierWithParty);

        Party party2 = new Party();
        carrierWithPartyCascade = new TestCarrier();
        carrierWithPartyCascade.setPartyWithCascade(party2);
        entityManager.persist(carrierWithPartyCascade);
        // one2many
        Contact contact = new Contact();
        entityManager.persist(contact);
        carrierWithContacts = new TestCarrier();
        carrierWithContacts.getContacts().add(contact);
        entityManager.persist(carrierWithContacts);

        Document document = new Document();
        carrierWithDocuments = new TestCarrier();
        carrierWithDocuments.getDocuments().add(document);
        entityManager.persist(carrierWithDocuments);
        // many2many
        CarrierGroup group = new CarrierGroup();
        entityManager.persist(group);
        carrierWithGroups = new TestCarrier();
        carrierWithGroups.getGroups().add(group);
        entityManager.persist(carrierWithGroups);

        CarrierTeam team = new CarrierTeam();
        carrierWithTeams = new TestCarrier();
        carrierWithTeams.getTeams().add(team);
        entityManager.persist(carrierWithTeams);
        // many2one
        Email email = new Email();
        entityManager.persist(email);
        carrierWithEmail = new TestCarrier();
        carrierWithEmail.setEmail(email);
        entityManager.persist(carrierWithEmail);

        Email email2 = new Email();
        carrierWithEmailCascade = new TestCarrier();
        carrierWithEmailCascade.setEmailWithCascade(email2);
        entityManager.persist(carrierWithEmailCascade);

        entityManager.flush();
        setUserContext(user1);

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
    public void remove(IdHolder object) {
        object = entityManager.find(object.getClass(), object.getId());
        entityManager.remove(object);
    }

    @Test
    public void test_delete_entity_with_primitive_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrier);

        assertNull(entityManager.find(TestCarrier.class, carrier.getId()));
    }

    

    @Test
    public void test_delete_entity_with_primitive_field_with_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class, carrier.getId()));

        self.get().remove(carrier);

        assertNull(entityManager.find(TestCarrier.class, carrier.getId()));
    }

    // one2one
    @Test
    public void test_delete_entity_with_one_to_one_field_no_cascade() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithParty);

        assertNull(entityManager.find(TestCarrier.class, carrierWithParty.getId()));
        assertNotNull(entityManager.find(Party.class, carrierWithParty.getParty().getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_one_field_not_permitted() {

        self.get().remove(carrierWithParty);

        assertNull(entityManager.find(TestCarrier.class, carrierWithParty.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_one_field_cascade() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Party.class));

        self.get().remove(carrierWithPartyCascade);

        assertNull(entityManager.find(TestCarrier.class, carrierWithPartyCascade.getId()));
        assertNull(entityManager.find(Party.class, carrierWithPartyCascade.getPartyWithCascade().getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_one_field_cascade_not_permitted() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithPartyCascade);
    }

    // one2many
    @Test
    public void test_delete_entity_with_one_to_many_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithContacts);

        assertNull(entityManager.find(TestCarrier.class, carrierWithContacts.getId()));
        assertNotNull(entityManager.find(Contact.class, carrierWithContacts.getContacts().iterator().next().getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_many_field_not_permitted() {
        self.get().remove(carrierWithContacts);
    }

    @Test
    public void test_delete_entity_with_one_to_many_field_cascade() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Document.class));

        self.get().remove(carrierWithDocuments);

        assertNull(entityManager.find(TestCarrier.class, carrierWithDocuments.getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_many_field_cascade_not_permitted() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithDocuments);

    }

    // many2many
    @Test
    public void test_delete_entity_with_many_to_many_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithGroups);

        assertNull(entityManager.find(TestCarrier.class, carrierWithGroups.getId()));
        assertNotNull(entityManager.find(CarrierGroup.class, carrierWithGroups.getGroups().iterator().next().getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_many_to_many_field_not_permitted() {
        self.get().remove(carrierWithGroups);
    }

    @Test
    public void test_delete_entity_with_many_to_many_field_cascade() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(CarrierTeam.class));

        self.get().remove(carrierWithTeams);

        assertNull(entityManager.find(TestCarrier.class, carrierWithTeams.getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_many_to_many_field_cascade_not_permitted() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithTeams);
    }

    // many2one
    @Test
    public void test_delete_entity_with_many_to_one_field() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithEmail);

        assertNull(entityManager.find(TestCarrier.class, carrierWithEmail.getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_many_to_one_field_not_permitted() {

        self.get().remove(carrierWithEmail);

    }

    @Test
    public void test_delete_entity_with_many_to_one_field_cascade() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Email.class));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithEmailCascade);

        assertNull(entityManager.find(TestCarrier.class, carrierWithEmailCascade.getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_many_to_one_field_cascade_not_permitted() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class));

        self.get().remove(carrierWithEmailCascade);
    }

    // again with object permissions
    @Test
    public void test_delete_entity_with_primitive_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrier));
        self.get().remove(carrier);
        assertNull(entityManager.find(TestCarrier.class, carrier.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_one_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithParty));
        self.get().remove(carrierWithParty);
        assertNull(entityManager.find(TestCarrier.class, carrierWithParty.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_one_field_cascade_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithPartyCascade));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithPartyCascade.getPartyWithCascade()));
        self.get().remove(carrierWithPartyCascade);
        assertNull(entityManager.find(TestCarrier.class, carrierWithPartyCascade.getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_one_field_cascade_with_missing_party_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithPartyCascade));
        self.get().remove(carrierWithPartyCascade);
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_one_field_with_wrong_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(TestCarrier.class, -1));
        self.get().remove(carrierWithParty);
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_one_field_cascade_with_wrong_party_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithPartyCascade));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Party.class, -1));
        self.get().remove(carrierWithPartyCascade);
    }

    @Test
    public void test_delete_entity_with_one_to_many_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithContacts));
        self.get().remove(carrierWithContacts);
        assertNull(entityManager.find(TestCarrier.class, carrierWithContacts.getId()));
    }

    @Test
    public void test_delete_entity_with_one_to_many_field_cascade_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithDocuments));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithDocuments.getDocuments().iterator().next()));

        self.get().remove(carrierWithDocuments);

        assertNull(entityManager.find(TestCarrier.class, carrierWithDocuments.getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_many_field_cascade_with_missing_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithDocuments));

        self.get().remove(carrierWithDocuments);
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_one_to_many_field_cascade_with_wrong_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithDocuments));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Document.class, -1));

        self.get().remove(carrierWithDocuments);
    }

    @Test
    public void test_delete_entity_with_many_to_many_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithGroups));

        self.get().remove(carrierWithGroups);

        assertNull(entityManager.find(TestCarrier.class, carrierWithGroups.getId()));
    }

    @Test
    public void test_delete_entity_with_many_to_many_field_cascade_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithTeams));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithTeams.getTeams().iterator().next()));

        self.get().remove(carrierWithTeams);

        assertNull(entityManager.find(TestCarrier.class, carrierWithTeams.getId()));
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_many_to_many_field_cascade_with_wrong_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithTeams));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(CarrierTeam.class, -1));

        self.get().remove(carrierWithTeams);
    }

    @Test
    public void test_delete_entity_with_many_to_one_field_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithEmail));

        self.get().remove(carrierWithEmail);

        assertNull(entityManager.find(TestCarrier.class, carrierWithEmail.getId()));
    }

    @Test
    public void test_delete_entity_with_many_to_one_field_cascade_with_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithEmailCascade));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithEmailCascade.getEmailWithCascade()));

        self.get().remove(carrierWithEmailCascade);

        assertNull(entityManager.find(TestCarrier.class, carrierWithEmailCascade.getId()));
    }
    
    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_many_to_one_field_cascade_with_no_delete_party_entity_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithEmailCascade));
        
        self.get().remove(carrierWithEmailCascade);
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_entity_with_many_to_one_field_cascade_with_wrong_entity_object_permission() {
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(carrierWithEmailCascade));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Email.class, -1));

        self.get().remove(carrierWithEmailCascade);
    }

    @Test
    public void test_delete_contact_from_carrier() {
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Carrier.class));
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(Carrier.class, "contacts"));
        securityService.grant(admin, user1, getRemoveAction(), entityFieldFactory.createResource(Carrier.class, "contacts"));
        
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Contact.class));

        Carrier carrier = new Carrier();
        self.get().persist(carrier);

        Contact contact = new Contact();
        contact.setCarrier(carrier);
        self.get().persist(contact);

        self.get().remove(contact);
    }
  
    
    @Test
    public void test_delete_contact_from_carrier_field_level_disabled() {
        setUserContext(admin);
        user1.getCompany().setFieldLevelEnabled(false);
        self.get().merge(user1.getCompany());
        
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Carrier.class));
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Carrier.class));
        
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Contact.class));
        
        

        setUserContext(user1);
        Carrier carrier = new Carrier();
        self.get().persist(carrier);

        Contact contact = new Contact();
        contact.setCarrier(carrier);
        self.get().persist(contact);

        self.get().remove(contact);
    }

    @Test(expected = PermissionActionException.class)
    public void test_delete_contact_from_carrier_not_allowed() {
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Carrier.class));
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(Carrier.class, "contacts"));
        
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Contact.class));

        Carrier carrier = new Carrier();
        self.get().persist(carrier);

        Contact contact = new Contact();
        contact.setCarrier(carrier);
        self.get().persist(contact);

        self.get().remove(contact);
    }

   

}
