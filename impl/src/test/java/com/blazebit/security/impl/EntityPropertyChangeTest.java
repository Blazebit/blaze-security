/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl;

import com.blazebit.security.Action;
import com.blazebit.security.PermissionException;
import com.blazebit.security.SecurityService;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.EntityConstants;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.impl.utils.ActionUtils;
import com.blazebit.security.impl.utils.EntityUtils;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author cuszk
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class EntityPropertyChangeTest extends BaseTest<EntityPropertyChangeTest> {

    @Inject
    private SecurityService securityService;
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

        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(Carrier.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(Party.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(Contact.class, EntityField.EMPTY_FIELD));
        permissionFactory.create(admin, createAction, EntityUtils.getEntityFieldFor(CarrierGroup.class, EntityField.EMPTY_FIELD));
        ChangeInterceptor.activate();
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

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(Object object) {
        entityManager.remove(object);
    }

    @Test
    public void test_change_entity_primitive_field() {
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "field1"));
        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "field2"));
        userContext.setUser(user1);
        carrier.setField1("field1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getField1(), "field1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_entity_primitive_field_not_permitted() {
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "field1"));
        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "field2"));
        userContext.setUser(user1);
        carrier.setField1("field1_changed");
        carrier.setField3("field3_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getField1(), "field1");
        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getField3(), "field3");
    }

    //one-to-one
    @Test
    public void test_change_one_to_one_field_with_entity_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.PARTY, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }
    

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_with_wrong_entity_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.EMAIL, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_with_entity_field_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.PARTY, "partyField1"));
        userContext.setUser(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);


        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_with_wrong_entity_field_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.PARTY, "partyField2"));
        userContext.setUser(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_with_entity_object_field_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        carrier.setParty(party);
        self.get().persist(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityObjectFieldFor(EntityConstants.PARTY, "partyField1", carrier.getParty().getEntityId()));
        userContext.setUser(user1);
        party.setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_with_wrong_entity_object_field_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityObjectFieldFor(EntityConstants.PARTY, "partyField1", "-2"));
        userContext.setUser(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.PARTY, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_field_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "party"));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.PARTY, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_object_field_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityObjectFieldFor(EntityConstants.CARRIER, EntityField.EMPTY_FIELD, carrier.getEntityId()));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.PARTY, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);


        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_reference_with_wrong_entity_object_field_permission() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "-2"));
        userContext.setUser(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);


        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_reference_not_permitted() {
        Party party = new Party();
        party.setPartyField1("party_field_1");
        self.get().persist(party);
        carrier.setParty(party);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.PARTY, "partyField1"));
        userContext.setUser(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);


    }

    //one-to-many
    @Test
    public void test_change_one_to_many_field_from_owner_with_entity_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);

        carrier.getContacts().add(contact);
        self.get().persist(carrier);


        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());
    }

    @Test
    public void test_change_one_to_many_field_from_owner_with_entity_field_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, "contactField"));
        userContext.setUser(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_field_from_owner_with_wrong_entity_field_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, "id"));
        userContext.setUser(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test
    public void test_change_one_to_many_field_from_owner_with_entity_object_field_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityObjectFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD, carrier.getContacts().iterator().next().getEntityId()));
        userContext.setUser(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_field_from_owner_with_wrong_entity_object_field_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityObjectFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD, "2"));
        userContext.setUser(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test
    public void test_change_one_to_many_add_new_with_entity_field_permission() {
        self.get().persist(carrier);
        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "contacts"));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        Contact newContact = new Contact();
        newContact.setContactField("new_contact_field");
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(1, carrier.getContacts().size());

    }

    @Test
    public void test_change_one_to_many_add_new_with_entity_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD));

        userContext.setUser(user1);
        Contact newContact = new Contact();
        newContact.setContactField("new_contact_field");
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(2, carrier.getContacts().size());
    }

    @Test
    public void test_change_one_to_many_add_new_with_entity_object_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityObjectFieldFor(EntityConstants.CARRIER, EntityField.EMPTY_FIELD, carrier.getEntityId()));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        Contact newContact = new Contact();
        newContact.setContactField("new_contact_field");
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(2, carrier.getContacts().size());

    }

    @Test
    public void test_change_one_to_many_remove_with_entity_field_permission() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "contacts"));
        securityService.grant(admin, user1, getDeleteAction(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        carrier.getContacts().remove(contact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(0, carrier.getContacts().size());
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_remove_not_permitted() {
        Contact contact = new Contact();
        contact.setContactField("field_1");
        self.get().persist(contact);
        carrier.getContacts().add(contact);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CONTACT, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        carrier.getContacts().remove(contact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(0, carrier.getContacts().size());
    }

    //many-to-many 
    @Test
    public void test_change_many_to_many_from_owner_with_entity_permission() {
        CarrierGroup g1 = new CarrierGroup();
        g1.setName("g1");
        self.get().persist(g1);
        carrier.getGroups().add(g1);
        self.get().persist(carrier);


        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIERGROUP, EntityField.EMPTY_FIELD));
        userContext.setUser(user1);
        carrier.getGroups().iterator().next().setName("changed_name");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_name", carrier.getGroups().iterator().next().getName());
    }

    @Test
    public void test_change_many_to_many_from_owner_add_new_with_entity_field_permission() {
        CarrierGroup g1 = new CarrierGroup();
        g1.setName("g1");
        self.get().persist(g1);
        carrier.getGroups().add(g1);
        self.get().persist(carrier);
        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIER, "groups"));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIERGROUP, EntityField.EMPTY_FIELD));

        userContext.setUser(user1);
        CarrierGroup g2 = new CarrierGroup();
        g2.setName("g2");
        self.get().persist(g2);
        carrier.getGroups().add(g2);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(2, carrier.getGroups().size());
    }

    @Test(expected = PermissionException.class)
    public void test_change_many_to_many_add_new_from_owner_not_permitted() {
        CarrierGroup g1 = new CarrierGroup();
        g1.setName("g1");
        entityManager.persist(g1);
        carrier.getGroups().add(g1);
        self.get().persist(carrier);

        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIERGROUP, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getAddAction(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIERGROUP, EntityField.EMPTY_FIELD));

        userContext.setUser(user1);
        CarrierGroup g2 = new CarrierGroup();
        g2.setName("g2");
        self.get().persist(g2);
        carrier.getGroups().add(g2);
        carrier = (Carrier) self.get().merge(carrier);

    }
    //!!!! ---- saving from not owner side does not work and it shouldnt ---- !!!
//    //one-to-many - not owner side
//    @Test
//    public void test_change_one_to_many_not_from_owner() {
//        self.get().persist(carrier);
//
//        Email email = new Email();
//        email.setEmailField("email_field");
//        email.setCarrier(carrier);
//        self.get().persist(email);
//
//        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.EMAIL, EntityField.EMPTY_FIELD));
//
//        email.setEmailField("changed_email_field");
//        carrier = (Carrier) self.get().merge(carrier);
//
//        assertEquals("changed_email_field", entityManager.find(Email.class, email.getId()).getEmailField());
//    }
//
//    //many-to-many - not owner side
//    @Test
//    public void test_change_many_to_many_not_from_owner() {
//        self.get().persist(carrier);
//
//        CarrierTeam t1 = new CarrierTeam();
//        t1.setName("t1");
//        t1.getCarriers().add(carrier);
//        self.get().persist(t1);
//
//        securityService.grant(admin, user1, getUpdateAcion(), EntityUtils.getEntityFieldFor(EntityConstants.CARRIERTEAM, EntityField.EMPTY_FIELD));
//
//        t1.setName("changed_name");
//        carrier = (Carrier) self.get().merge(carrier);
//
//        assertEquals("changed_name", entityManager.find(CarrierTeam.class, t1.getId()).getName());
//    }

    public Action getUpdateAcion() {
        return ActionUtils.getAction(ActionUtils.ActionConstants.UPDATE);
    }

    public Action getAddAction() {
        return ActionUtils.getAction(ActionUtils.ActionConstants.CREATE);
    }

    public Action getDeleteAction() {
        return ActionUtils.getAction(ActionUtils.ActionConstants.DELETE);
    }
}
