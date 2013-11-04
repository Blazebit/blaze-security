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

import com.blazebit.security.Action;
import com.blazebit.security.PermissionException;
import com.blazebit.security.PermissionService;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
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
public class EntityPropertyChangeTest extends BaseTest<EntityPropertyChangeTest> {

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
        Party party = new Party();
        party.setPartyField1("party_field_1");
        entityManager.persist(party);
        carrier.setParty(party);
        Contact contact = new Contact();
        contact.setContactField("field_1");
        entityManager.persist(contact);
        carrier.getContacts().add(contact);
        CarrierGroup g1 = new CarrierGroup();
        g1.setName("g1");
        entityManager.persist(g1);
        carrier.getGroups().add(g1);
        entityManager.persist(carrier);

        permissionManager.save(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD)));
        permissionManager.save(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD)));
        permissionManager.save(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD)));
        permissionManager.save(permissionFactory.create(admin, createAction, entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD)));
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
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, "field1"));
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, "field2"));
        setUserContext(user1);
        carrier.setField1("field1_changed");
        carrier = (Carrier) self.get().merge(carrier);
        permissionManager.getPermissions(admin);
        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getField1(), "field1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_entity_primitive_field_not_permitted() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, "field1"));
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, "field2"));
        setUserContext(user1);
        carrier.setField1("field1_changed");
        carrier.setField3("field3_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getField1(), "field1");
        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getField3(), "field3");
    }

    // one-to-one
    @Test
    public void test_change_one_to_one_field_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_with_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Party.class, "partyField1"));
        setUserContext(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_with_wrong_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Party.class, "partyField2"));
        setUserContext(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_with_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Party.class, "partyField1", carrier.getParty().getId()));
        setUserContext(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_with_wrong_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Party.class, "partyField1", -2));
        setUserContext(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_with_wrong_entity_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(User.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        carrier.getParty().setPartyField1("party_field_1_changed");
        carrier = (Carrier) self.get().merge(carrier);
    
        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, "party"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test
    public void test_change_one_to_one_field_reference_with_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, carrier.getId()));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Party.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_reference_with_wrong_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Carrier.class, "-2"));
        setUserContext(user1);

        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(entityManager.find(Carrier.class, carrier.getId()).getParty().getPartyField1(), "party_field_1_changed");
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_one_field_reference_not_permitted() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Party.class, "partyField1"));
        setUserContext(user1);
        Party newParty = new Party();
        newParty.setPartyField1("party_field_1_changed");
        self.get().persist(newParty);
        carrier.setParty(newParty);
        carrier = (Carrier) self.get().merge(carrier);

    }

    // one-to-many
    @Test
    public void test_change_one_to_many_field_from_owner_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());
    }

    @Test
    public void test_change_one_to_many_field_from_owner_with_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Contact.class, "contactField"));
        setUserContext(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_field_from_owner_with_wrong_entity_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Contact.class, "id"));
        setUserContext(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test
    public void test_change_one_to_many_field_from_owner_with_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(),
                              entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD, carrier.getContacts().iterator().next().getId()));
        setUserContext(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_field_from_owner_with_wrong_entity_object_field_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD, 2));
        setUserContext(user1);
        carrier.getContacts().iterator().next().setContactField("changed_contact_field");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_contact_field", carrier.getContacts().iterator().next().getContactField());

    }

    @Test
    public void test_change_one_to_many_add_new_with_entity_field_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(Carrier.class, "contacts"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        Contact newContact = new Contact();
        newContact.setContactField("new_contact_field");
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(2, carrier.getContacts().size());

    }

    @Test
    public void test_change_one_to_many_add_new_with_entity_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));

        setUserContext(user1);
        Contact newContact = new Contact();
        newContact.setContactField("new_contact_field");
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(2, carrier.getContacts().size());
    }

    @Test
    public void test_change_one_to_many_add_new_with_entity_object_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(Carrier.class, EntityField.EMPTY_FIELD, carrier.getId()));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        Contact newContact = new Contact();
        newContact.setContactField("new_contact_field");
        self.get().persist(newContact);
        carrier.getContacts().add(newContact);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(2, carrier.getContacts().size());

    }

    @Test
    public void test_change_one_to_many_remove_with_entity_field_permission() {
        securityService.grant(admin, user1, getRemoveAction(), entityFieldFactory.createResource(Carrier.class, "contacts"));
        securityService.grant(admin, user1, getDeleteAction(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        carrier.getContacts().remove(carrier.getContacts().iterator().next());
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(0, carrier.getContacts().size());
    }

    @Test(expected = PermissionException.class)
    public void test_change_one_to_many_remove_not_permitted() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(Contact.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        carrier.getContacts().remove(carrier.getContacts().iterator().next());
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(0, carrier.getContacts().size());
    }

    // many-to-many
    @Test
    public void test_change_many_to_many_from_owner_with_entity_permission() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD));
        setUserContext(user1);
        carrier.getGroups().iterator().next().setName("changed_name");
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals("changed_name", carrier.getGroups().iterator().next().getName());
    }

    @Test
    public void test_change_many_to_many_from_owner_add_new_with_entity_field_permission() {
        securityService.grant(admin, user1, getAddAction(), entityFieldFactory.createResource(Carrier.class, "groups"));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD));

        setUserContext(user1);
        CarrierGroup g2 = new CarrierGroup();
        g2.setName("g2");
        self.get().persist(g2);
        carrier.getGroups().add(g2);
        carrier = (Carrier) self.get().merge(carrier);

        assertEquals(2, carrier.getGroups().size());
    }

    @Test(expected = PermissionException.class)
    public void test_change_many_to_many_add_new_from_owner_not_permitted() {
        securityService.grant(admin, user1, getUpdateAcion(), entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD));
        securityService.grant(admin, user1, getCreateAction(), entityFieldFactory.createResource(CarrierGroup.class, EntityField.EMPTY_FIELD));

        setUserContext(user1);
        CarrierGroup g2 = new CarrierGroup();
        g2.setName("g2");
        self.get().persist(g2);
        carrier.getGroups().add(g2);
        carrier = (Carrier) self.get().merge(carrier);

    }


    public Action getUpdateAcion() {
        return actionFactory.createAction(ActionConstants.UPDATE);
    }

    public Action getCreateAction() {
        return actionFactory.createAction(ActionConstants.CREATE);
    }

    public Action getAddAction() {
        return actionFactory.createAction(ActionConstants.ADD);
    }

    public Action getRemoveAction() {
        return actionFactory.createAction(ActionConstants.REMOVE);
    }

    public Action getDeleteAction() {
        return actionFactory.createAction(ActionConstants.DELETE);
    }
}
