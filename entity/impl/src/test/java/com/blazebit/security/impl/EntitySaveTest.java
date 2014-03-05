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

import com.blazebit.security.entity.EntityFeatures;
import com.blazebit.security.exception.PermissionActionException;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.model.sample.Carrier;
import com.blazebit.security.model.sample.CarrierGroup;
import com.blazebit.security.model.sample.CarrierTeam;
import com.blazebit.security.model.sample.Contact;
import com.blazebit.security.model.sample.Document;
import com.blazebit.security.model.sample.Email;
import com.blazebit.security.model.sample.Party;
import com.blazebit.security.model.sample.TestCarrier;
import com.blazebit.security.service.PermissionService;
import com.blazebit.security.test.BeforeDatabaseAware;
import com.blazebit.security.test.DatabaseAware;

/**
 * 
 * @author cuszk
 */
@DatabaseAware
public class EntitySaveTest extends BaseTest<EntitySaveTest> {

    private static final long serialVersionUID = 1L;
    @Inject
    private PermissionService permissionService;
    private TestCarrier carrier;

    @BeforeDatabaseAware
    public void init() {
        super.initData();
        carrier = new TestCarrier();
        // carrier.setField("field1");
        setUserContext(admin);
        EntityFeatures.activateInterceptor();
    }

    @Test
    public void test_create_entity_with_primitive_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        setUserContext(user1);
        persist(carrier);
        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_primitive_field_with_create_field_permissions() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class, "field"));
        setUserContext(user1);
        carrier.setField("Field1");
        persist(carrier);
        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_primitive_field_with_create_field_permissions_not_permitted() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class, "field"));
        setUserContext(user1);
        carrier.setAnotherField("anotherField");
        persist(carrier);
        assertNotNull(carrier.getId());
    }

    // One2One
    @Test
    public void test_create_entity_with_one_to_one_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Party.class));
        setUserContext(user1);

        Party party = new Party();
        persist(party);
        carrier.setParty(party);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_one_to_one_field_with_only_field_permission() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class, "party"));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Party.class));
        setUserContext(user1);

        Party party = new Party();
        persist(party);
        carrier.setParty(party);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    // One2One+Cascade.ALL
    @Test
    public void test_create_entity_with_one_to_one_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Party.class));
        setUserContext(user1);

        Party party = new Party();
        carrier.setPartyWithCascade(party);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_one_to_one_field_not_permitted_field_create_party_field_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class, "field"));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Party.class));
        setUserContext(user1);

        Party party = new Party();
        persist(party);
        carrier.setParty(party);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_one_to_one_field_not_permitted_related_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        setUserContext(user1);

        Party party = new Party();
        persist(party);
        carrier.setParty(party);

        persist(carrier);
    }

    // One2One+Cascade.ALL
    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_one_to_one_field_with_cascade_not_permitted_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        setUserContext(user1);

        Party party = new Party();
        carrier.setPartyWithCascade(party);
        persist(carrier);
    }

    // One2Many
    @Test
    public void test_create_entity_with_one_to_many_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Contact.class));
        permissionService.grant(admin, user1, getAddAction(), entityResourceFactory.createResource(TestCarrier.class, "contacts"));
        setUserContext(user1);

        Contact contact = new Contact();
        contact.setContactField("contact_field");
        persist(contact);
        carrier.getContacts().add(contact);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_one_to_many_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Document.class));
        setUserContext(user1);

        Document document = new Document();
        carrier.getDocuments().add(document);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_one_to_many_field_field_level_disabled() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Contact.class));
        user1.getCompany().setFieldLevelEnabled(false);
        merge(user1.getCompany());
        setUserContext(user1);

        Contact contact = new Contact();
        contact.setContactField("contact_field");
        persist(contact);
        carrier.getContacts().add(contact);

        persist(carrier);
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_one_to_many_field_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getAddAction(), entityResourceFactory.createResource(TestCarrier.class, "contacts"));
        setUserContext(user1);

        Contact contact = new Contact();
        persist(contact);
        carrier.getContacts().add(contact);
        persist(carrier);
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_one_to_many_field_with_cascade_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        setUserContext(user1);

        Document document = new Document();
        carrier.getDocuments().add(document);
        persist(carrier);
    }

    @Test
    public void test_create_entity_with_one_to_many_field_more_entries() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Contact.class));
        permissionService.grant(admin, user1, getAddAction(), entityResourceFactory.createResource(TestCarrier.class, "contacts"));
        setUserContext(user1);

        Contact contact = new Contact();
        persist(contact);

        Contact contact1 = new Contact();
        persist(contact1);

        carrier.getContacts().add(contact);
        carrier.getContacts().add(contact1);
        carrier.getContacts().remove(contact);

        Contact contact2 = new Contact();
        persist(contact2);

        carrier.getContacts().add(contact2);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_contact_with_carrier_many2one_reference() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Carrier.class));
        permissionService.grant(admin, user1, getAddAction(), entityResourceFactory.createResource(Carrier.class, "contacts"));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Contact.class));
        setUserContext(user1);

        Carrier carrier = new Carrier();
        persist(carrier);

        Contact contact = new Contact();
        contact.setCarrier(carrier);
        persist(contact);

    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_contact_with_carrier_many2one_reference_missing_add_permission_for_contact() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Carrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Contact.class));
        setUserContext(user1);

        Carrier carrier = new Carrier();
        persist(carrier);

        Contact contact = new Contact();
        contact.setCarrier(carrier);
        persist(contact);

    }

    // Many2Many
    @Test
    public void test_create_entity_with_many_to_many_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(CarrierGroup.class));
        permissionService.grant(admin, user1, getAddAction(), entityResourceFactory.createResource(TestCarrier.class, "groups"));
        setUserContext(user1);

        CarrierGroup group = new CarrierGroup();
        persist(group);
        carrier.getGroups().add(group);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_many_to_many_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(CarrierTeam.class));
        permissionService.grant(admin, user1, getAddAction(), entityResourceFactory.createResource(TestCarrier.class, "teams"));
        setUserContext(user1);

        CarrierTeam team = new CarrierTeam();
        carrier.getTeams().add(team);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_many_to_many_field_not_permitted_update_field_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(CarrierGroup.class));
        setUserContext(user1);

        CarrierGroup group = new CarrierGroup();
        persist(group);
        carrier.getGroups().add(group);
        persist(carrier);
    }

    @Test
    public void test_create_entity_with_many_to_many_field_with_cascade_with_create_entity_permissions() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(CarrierTeam.class));
        setUserContext(user1);

        CarrierTeam team = new CarrierTeam();
        carrier.getTeams().add(team);
        persist(carrier);
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_many_to_many_field_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        setUserContext(user1);

        CarrierGroup group = new CarrierGroup();
        persist(group);
        carrier.getGroups().add(group);
        persist(carrier);
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_many_to_many_field_with_cascade_not_permitted_create_entity_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        setUserContext(user1);

        CarrierTeam team = new CarrierTeam();
        carrier.getTeams().add(team);
        persist(carrier);
    }

    // Many2One
    @Test
    public void test_create_entity_with_many_to_one_field() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Email.class));

        setUserContext(user1);

        Email email = new Email();
        persist(email);
        carrier.setEmail(email);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test
    public void test_create_entity_with_many_to_one_field_with_cascade() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Email.class));

        setUserContext(user1);

        Email email = new Email();
        carrier.setEmailWithCascade(email);
        persist(carrier);

        assertNotNull(carrier.getId());
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_many_to_one_field_not_permitted_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));

        setUserContext(user1);

        Email email = new Email();
        persist(email);
        carrier.setEmail(email);
        persist(carrier);
    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_many_to_one_field_with_cascade_not_permitted_entity_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class));

        setUserContext(user1);

        Email email = new Email();
        carrier.setEmailWithCascade(email);
        persist(carrier);

    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_many_to_one_field_not_permitted_field_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class, "field"));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Email.class));
        setUserContext(user1);

        Email email = new Email();
        persist(email);
        carrier.setEmail(email);
        persist(carrier);

    }

    @Test(expected = PermissionActionException.class)
    public void test_create_entity_with_many_to_one_field_with_cascade_not_permitted_field_create_missing() {
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(TestCarrier.class, "field"));
        permissionService.grant(admin, user1, getCreateAction(), entityResourceFactory.createResource(Email.class));
        setUserContext(user1);

        Email email = new Email();
        carrier.setEmailWithCascade(email);
        persist(carrier);

    }

}
