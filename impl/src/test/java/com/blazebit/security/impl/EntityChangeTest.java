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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionService;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.sample.Carrier;
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
public class EntityChangeTest extends BaseTest<EntityChangeTest> {

    private static final long serialVersionUID = 1L;

    @Inject
    private PermissionService securityService;
    private TestCarrier carrier;

    @Before
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void init() {
        super.initData();

        carrier = new TestCarrier();
        // primitive
        carrier.setField("field");
        carrier.setAnotherField("another_field");
        // one2one
        Party party = new Party();
        party.setPartyField1("party_1");
        entityManager.persist(party);
        carrier.setParty(party);

        Party partyWithCascade = new Party();
        partyWithCascade.setPartyField1("party_2");
        carrier.setPartyWithCascade(partyWithCascade);
        // one2many
        Contact contact = new Contact();
        contact.setContactField("field_1");
        entityManager.persist(contact);
        carrier.getContacts().add(contact);

        Document document = new Document();
        document.setTitle("The jungle book");
        carrier.getDocuments().add(document);
        // many2many
        CarrierGroup group = new CarrierGroup();
        group.setName("Big group");
        entityManager.persist(group);
        carrier.getGroups().add(group);

        CarrierTeam team = new CarrierTeam();
        team.setName("A Team");
        carrier.getTeams().add(team);
        // many2one
        Email email = new Email();
        email.setSubject("Hey");
        email.setBody("Hello email reader!");
        entityManager.persist(email);
        carrier.setEmail(email);

        Email email2 = new Email();
        email2.setSubject("Hey2");
        email2.setBody("Hello email reader!2");
        carrier.setEmailWithCascade(email2);

        entityManager.persist(carrier);
        ChangeInterceptor.activate();
        setUserContext(admin);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persist(Object object) {
        entityManager.persist(object);
        entityManager.flush();
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
        entityManager.flush();
    }

    @Test
    public void test_change_entity_primitive_field() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "field"));
        setUserContext(user1);

        carrier.setField("field_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getField(), "field_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_entity_primitive_field_not_permitted() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "field"));
        setUserContext(user1);

        carrier.setField("field_changed");
        carrier.setAnotherField("another_field_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

    }

    // one-to-one -
    // ! without cascade related field is not updated - no need for party update permission
    @Test
    public void test_change_one_to_one_field_with_entity_permission() {
        setUserContext(user1);

        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getParty().getPartyField1(), "party_1");
    }

    // ! important: in this case carrier update permission is not required because only the existing party relation is updated
    @Test
    public void test_change_one_to_one_field_cascade_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Party.class));
        setUserContext(user1);

        carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getPartyWithCascade().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_cascade_with_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Party.class, "partyField1"));
        setUserContext(user1);
        carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getPartyWithCascade().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_cascade_with_wrong_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Party.class, "partyField2"));
        setUserContext(user1);
        carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getPartyWithCascade().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_cascade_with_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Party.class, "partyField1", carrier.getPartyWithCascade().getId()));
        setUserContext(user1);
        carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getPartyWithCascade().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_cascade_with_wrong_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Party.class, "partyField1", -2));
        setUserContext(user1);
        carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getPartyWithCascade().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_cascade_with_wrong_entity_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(User.class));
        setUserContext(user1);
        carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getPartyWithCascade().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_no_cascade_reference_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class));

        setUserContext(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);

        carrier.setParty(newParty);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_cascade_reference_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class));
        setUserContext(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");

        carrier.setPartyWithCascade(newParty);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getPartyWithCascade().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "party"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class));
        setUserContext(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, carrier.getId()));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class));
        setUserContext(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(entityManager.find(TestCarrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_reference_with_wrong_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "-2"));
        setUserContext(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (TestCarrier) self.get().merge(carrier);

    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_reference_not_permitted() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Party.class, "partyField1"));
        setUserContext(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (TestCarrier) self.get().merge(carrier);

    }

    // one-to-many - no cascade- doesnt change contact entity
    // change related entity field
    @Test
    public void test_change_one_to_many_field_no_cascade_with_entity_permission() {
        setUserContext(user1);

        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("field_1", carrier.getContacts().iterator().next().getContactField());
    }

    @Test
    public void test_change_one_to_many_field_cascade_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Document.class));
        setUserContext(user1);
        carrier.getDocuments().iterator().next().setTitle("changed_title");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("changed_title", carrier.getDocuments().iterator().next().getTitle());
    }

    @Test
    public void test_change_one_to_many_field_cascade_with_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Document.class, "title"));
        setUserContext(user1);

        carrier.getDocuments().iterator().next().setTitle("changed_title");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("changed_title", carrier.getDocuments().iterator().next().getTitle());

    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_field_cascade_with_wrong_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Document.class, "id"));
        setUserContext(user1);

        carrier.getDocuments().iterator().next().setTitle("changed_title");
        carrier = (TestCarrier) self.get().merge(carrier);
    }

    @Test
    public void test_change_one_to_many_field_cascade_with_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(carrier.getDocuments().iterator().next()));
        setUserContext(user1);

        carrier.getDocuments().iterator().next().setTitle("changed_title");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("changed_title", carrier.getDocuments().iterator().next().getTitle());

    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_field_cascade_with_wrong_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Document.class, -1));
        setUserContext(user1);

        carrier.getDocuments().iterator().next().setTitle("changed_title");
        carrier = (TestCarrier) self.get().merge(carrier);

    }

    @Test
    public void test_change_one_to_many_cascade_add_new_with_entity_field_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "documents"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Document.class));
        setUserContext(user1);

        Document newDocument = new Document();
        carrier.getDocuments().add(newDocument);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(2, carrier.getDocuments().size());

    }

    @Test
    public void test_change_one_to_many_no_cascade_add_new_with_entity_field_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "contacts"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        setUserContext(user1);

        Contact newContact = new Contact();
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(2, carrier.getContacts().size());

    }

    // without add permission
    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_cascade_add_new_not_permitted() {
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Document.class));
        setUserContext(user1);

        Document newDocument = new Document();
        carrier.getDocuments().add(newDocument);
        carrier = (TestCarrier) self.get().merge(carrier);
    }

    // without add permission
    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_no_cascade_add_new_not_permitted() {
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        setUserContext(user1);

        Contact newContact = new Contact();
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (TestCarrier) self.get().merge(carrier);
    }

    @Test
    public void test_change_one_to_many_add_new_with_entity_object_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, carrier.getId()));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class));
        setUserContext(user1);

        Contact newContact = new Contact();
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(2, carrier.getContacts().size());

    }

    @Test
    public void test_change_one_to_many_remove_with_entity_field_permission() {
        securityService.grant(admin, user1, getRemoveAction(), entityFieldFactory.createResource(TestCarrier.class, "contacts"));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Contact.class));
        setUserContext(user1);

        carrier.getContacts().remove(carrier.getContacts().iterator().next());
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(0, carrier.getContacts().size());
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_remove_not_permitted() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Contact.class));
        setUserContext(user1);

        carrier.getContacts().remove(carrier.getContacts().iterator().next());
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(0, carrier.getContacts().size());
    }

    // many-to-many - does not change many to many related entity property when not cascaded
    @Test
    public void test_change_many_to_many_no_cascade_with_entity_permission() {
        setUserContext(user1);

        carrier.getGroups().iterator().next().setName("changed_name");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("Big group", carrier.getGroups().iterator().next().getName());
    }

    @Test
    public void test_change_many_to_many_cascade_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(CarrierTeam.class));
        setUserContext(user1);

        carrier.getTeams().iterator().next().setName("changed_name");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("changed_name", carrier.getTeams().iterator().next().getName());
    }

    @Test
    public void test_change_many_to_many_add_new_with_entity_field_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(TestCarrier.class, "groups"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierGroup.class));

        setUserContext(user1);
        CarrierGroup g2 = new CarrierGroup();
        self.get().persist(g2);
        carrier.getGroups().add(g2);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals(2, carrier.getGroups().size());
    }

    @Test(expected = PermissionException.class)
    public void test_change_many_to_many_add_new_not_permitted() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierGroup.class));

        setUserContext(user1);
        CarrierGroup g2 = new CarrierGroup();
        self.get().persist(g2);
        carrier.getGroups().add(g2);
        carrier = (TestCarrier) self.get().merge(carrier);
    }

    // many2one- does not change entity property when not cascaded
    @Test
    public void test_change_many_to_one_no_cascade() {
        setUserContext(user1);

        carrier.getEmail().setSubject("A");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("Hey", carrier.getEmail().getSubject());
    }

    @Test
    public void test_change_many_to_one_cascade() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class));
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Email.class));
        setUserContext(user1);

        carrier.getEmailWithCascade().setSubject("A");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("A", carrier.getEmailWithCascade().getSubject());
    }

    @Test
    public void test_change_many_to_one_cascade_with_field_permissions() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "emailWithCascade"));
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(Email.class, "subject"));
        setUserContext(user1);

        carrier.getEmailWithCascade().setSubject("A");
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("A", carrier.getEmailWithCascade().getSubject());
    }
    
    @Test
    public void test_change_many_to_one_no_cascade_with_entity_change() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "email"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Email.class));
        setUserContext(user1);

        Email newEmail=new Email();
        newEmail.setSubject("Changed_Email_Subject");
        carrier.setEmail(newEmail);
        self.get().persist(newEmail);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("Changed_Email_Subject", carrier.getEmail().getSubject());
    }
    
    @Test
    public void test_change_many_to_one_cascade_with_entity_change() {
        securityService.grant(admin, user1, getUpdateAction(), entityFieldFactory.createResource(TestCarrier.class, "emailWithCascade"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Email.class));
        setUserContext(user1);

        Email newEmail=new Email();
        newEmail.setSubject("Changed_Email_Subject");
        carrier.setEmailWithCascade(newEmail);
        carrier = (TestCarrier) self.get().merge(carrier);

        assertEquals("Changed_Email_Subject", carrier.getEmailWithCascade().getSubject());
    }
}
