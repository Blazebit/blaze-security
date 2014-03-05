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

import com.blazebit.security.entity.EntityFeatures;
import com.blazebit.security.exception.PermissionActionException;
import com.blazebit.security.impl.interceptor.ChangeInterceptor;
import com.blazebit.security.model.User;
import com.blazebit.security.model.sample.CarrierContactEntry;
import com.blazebit.security.model.sample.CarrierContactEntryId;
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
public class EntityChangeTest extends BaseTest<EntityChangeTest> {

	private static final long serialVersionUID = 1L;

	@Inject
	private PermissionService permissionService;

	private TestCarrier carrier;

	@BeforeDatabaseAware
	public void init() {
		super.initData();

		carrier = new TestCarrier();
		// primitive
		carrier.setField("field");
		carrier.setAnotherField("another_field");
		// one2one
		Party party = new Party();
		party.setPartyField1("party_1");
		persist(party);
		carrier.setParty(party);

		Party partyWithCascade = new Party();
		partyWithCascade.setPartyField1("party_2");
		carrier.setPartyWithCascade(partyWithCascade);
		// one2many
		Contact contact = new Contact();
		contact.setContactField("field_1");
		persist(contact);
		carrier.getContacts().add(contact);

		Document document = new Document();
		document.setTitle("The jungle book");
		carrier.getDocuments().add(document);
		// many2many
		CarrierGroup group = new CarrierGroup();
		group.setName("Big group");
		persist(group);
		carrier.getGroups().add(group);

		CarrierTeam team = new CarrierTeam();
		team.setName("A Team");
		carrier.getTeams().add(team);
		// many2one
		Email email = new Email();
		email.setSubject("Hey");
		email.setBody("Hello email reader!");
		persist(email);
		carrier.setEmail(email);

		Email email2 = new Email();
		email2.setSubject("Hey2");
		email2.setBody("Hello email reader!2");
		carrier.setEmailWithCascade(email2);

		persist(carrier);

        CarrierContactEntry contactEntry = new CarrierContactEntry();
        contactEntry.setCarrier(carrier);
        contactEntry.setContact(contact);

		contactEntry.setId(new CarrierContactEntryId(carrier.getId(), contact
				.getId()));
		persist(contactEntry);
        carrier.getContactEntries().add(contactEntry);

        EntityFeatures.activateInterceptor();
		setUserContext(admin);
	}

	@Test
	public void test_change_entity_primitive_field() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory
						.createResource(TestCarrier.class, "field"));
		setUserContext(user1);

		carrier.setField("field_changed");
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getField(), "field_changed");
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_entity_primitive_field_not_permitted() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory
						.createResource(TestCarrier.class, "field"));
		setUserContext(user1);

		carrier.setField("field_changed");
		carrier.setAnotherField("another_field_changed");
		carrier = (TestCarrier) merge(carrier);

	}

	// one-to-one -
	// ! without cascade related field is not updated - no need for party update
	// permission
	@Test
	public void test_change_one_to_one_field_with_entity_permission() {
		setUserContext(user1);

		carrier.getParty().setPartyField1("party_field_1_changed");
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getParty().getPartyField1(), "party_1");
	}

	// ! important: in this case carrier update permission is not required
	// because only the existing party relation is updated
	@Test
	public void test_change_one_to_one_field_cascade_with_entity_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Party.class));
		setUserContext(user1);

		carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getPartyWithCascade().getPartyField1(),
				"party_field_1_changed");
	}

	@Test
	public void test_change_one_to_one_field_cascade_with_entity_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory
						.createResource(Party.class, "partyField1"));
		setUserContext(user1);
		carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getPartyWithCascade().getPartyField1(),
				"party_field_1_changed");
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_one_field_cascade_with_wrong_entity_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory
						.createResource(Party.class, "partyField2"));
		setUserContext(user1);
		carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
		carrier = (TestCarrier) merge(carrier);
	}

	@Test
	public void test_change_one_to_one_field_cascade_with_entity_object_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Party.class,
						"partyField1", carrier.getPartyWithCascade().getId()));
		setUserContext(user1);
		carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getPartyWithCascade().getPartyField1(),
				"party_field_1_changed");
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_one_field_cascade_with_wrong_entity_object_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Party.class,
						"partyField1", -2));
		setUserContext(user1);
		carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
		carrier = (TestCarrier) merge(carrier);
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_one_field_cascade_with_wrong_entity_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(User.class));
		setUserContext(user1);
		carrier.getPartyWithCascade().setPartyField1("party_field_1_changed");
		carrier = (TestCarrier) merge(carrier);
	}

	@Test
	public void test_change_one_to_one_field_no_cascade_reference_with_entity_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Party.class));

		setUserContext(user1);

		Party newParty = new Party();
		newParty.setPartyField1("party_field_1_changed");
		persist(newParty);

		carrier.setParty(newParty);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getParty().getPartyField1(), "party_field_1_changed");
	}

	@Test
	public void test_change_one_to_one_field_cascade_reference_with_entity_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Party.class));
		setUserContext(user1);

		Party newParty = new Party();
		newParty.setPartyField1("party_field_1_changed");

		carrier.setPartyWithCascade(newParty);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getPartyWithCascade().getPartyField1(),
				"party_field_1_changed");
	}

	@Test
	public void test_change_one_to_one_field_reference_with_entity_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory
						.createResource(TestCarrier.class, "party"));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Party.class));
		setUserContext(user1);
		Party newParty = new Party();
		newParty.setPartyField1("party_field_1_changed");
		persist(newParty);
		carrier.setParty(newParty);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getParty().getPartyField1(), "party_field_1_changed");
	}

	@Test
	public void test_change_one_to_one_field_reference_with_entity_object_field_permission() {
		permissionService.grant(
				admin,
				user1,
				getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						carrier.getId()));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Party.class));
		setUserContext(user1);

		Party newParty = new Party();
		newParty.setPartyField1("party_field_1_changed");
		persist(newParty);
		carrier.setParty(newParty);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(entityManager.find(TestCarrier.class, carrier.getId())
				.getParty().getPartyField1(), "party_field_1_changed");
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_one_field_reference_with_wrong_entity_object_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class, "-2"));
		setUserContext(user1);

		Party newParty = new Party();
		newParty.setPartyField1("party_field_1_changed");
		persist(newParty);
		carrier.setParty(newParty);
		carrier = (TestCarrier) merge(carrier);

	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_one_field_reference_not_permitted() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory
						.createResource(Party.class, "partyField1"));
		setUserContext(user1);
		Party newParty = new Party();
		newParty.setPartyField1("party_field_1_changed");
		persist(newParty);
		carrier.setParty(newParty);
		carrier = (TestCarrier) merge(carrier);

	}

	// one-to-many - no cascade- doesnt change contact entity
	// change related entity field
	@Test
	public void test_change_one_to_many_field_no_cascade_with_entity_permission() {
		setUserContext(user1);

		carrier.getContacts().iterator().next()
				.setContactField("changed_contact_field");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("field_1", carrier.getContacts().iterator().next()
				.getContactField());
	}

	@Test
	public void test_change_one_to_many_no_cascade_add_new_with_entity_field_permission_without_field_level() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		user1.getCompany().setFieldLevelEnabled(false);
		merge(user1.getCompany());
		setUserContext(user1);

		Contact newContact = new Contact();
		newContact.setContactField("changed_contact_field");
		persist(newContact);

		carrier.getContacts().add(newContact);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(2, carrier.getContacts().size());

	}

	@Test
	public void test_change_one_to_many_no_cascade_add_new_with_entity_field_permission() {
		permissionService.grant(admin, user1, getAddAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						"contacts"));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		setUserContext(user1);

		Contact newContact = new Contact();
		persist(newContact);
		carrier.getContacts().add(newContact);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(2, carrier.getContacts().size());

	}

	@Test
	public void test_change_one_to_many_no_cascade_add_new_with_entity_field_permission_Without_field_level() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		user1.getCompany().setFieldLevelEnabled(false);
		merge(user1.getCompany());
		setUserContext(user1);

		Contact newContact = new Contact();
		persist(newContact);
		carrier.getContacts().add(newContact);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(2, carrier.getContacts().size());

	}

	// onetomany_embeddedid
	@Test
	public void test_change_one_to_many_field_embeddedid_with_update_field_permissions() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(CarrierContactEntry.class,
						"contact"));
		setUserContext(user1);

		Contact newContact = new Contact();
		self.get().persist(newContact);

		int sizeBefore = carrier.getContactEntries().size();
		CarrierContactEntry contactEntry = carrier.getContactEntries()
				.iterator().next();
		contactEntry.setContact(newContact);
		contactEntry = (CarrierContactEntry) self.get().merge(contactEntry);
		CarrierContactEntry reloadedCarrierContactEntry = entityManager.find(
				CarrierContactEntry.class, contactEntry.getId());

		assertEquals(sizeBefore, carrier.getContactEntries().size());
		assertEquals(newContact.getId(), reloadedCarrierContactEntry
				.getContact().getId());
	}

	@Test
	public void test_change_one_to_many_field_embeddedid_with_update_permission() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		permissionService
				.grant(admin, user1, getUpdateAction(), entityResourceFactory
						.createResource(CarrierContactEntry.class));
		setUserContext(user1);

		Contact newContact = new Contact();
		self.get().persist(newContact);

		int sizeBefore = carrier.getContactEntries().size();
		CarrierContactEntry contactEntry = carrier.getContactEntries()
				.iterator().next();
		contactEntry.setContact(newContact);
		contactEntry = (CarrierContactEntry) self.get().merge(contactEntry);
		CarrierContactEntry reloadedCarrierContactEntry = entityManager.find(
				CarrierContactEntry.class, contactEntry.getId());

		assertEquals(sizeBefore, carrier.getContactEntries().size());
		assertEquals(newContact.getId(), reloadedCarrierContactEntry
				.getContact().getId());
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_many_field_embeddedid_with_missing_update_permission() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		setUserContext(user1);

		Contact newContact = new Contact();
		self.get().persist(newContact);

		int sizeBefore = carrier.getContactEntries().size();
		CarrierContactEntry contactEntry = carrier.getContactEntries()
				.iterator().next();
		contactEntry.setContact(newContact);
		contactEntry = (CarrierContactEntry) self.get().merge(contactEntry);
		CarrierContactEntry reloadedCarrierContactEntry = entityManager.find(
				CarrierContactEntry.class, contactEntry.getId());

		assertEquals(sizeBefore, carrier.getContactEntries().size());
		assertEquals(newContact.getId(), reloadedCarrierContactEntry
				.getContact().getId());

	}

	// one to many cascade
	@Test
	public void test_change_one_to_many_field_cascade_with_entity_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Document.class));
		setUserContext(user1);
		carrier.getDocuments().iterator().next().setTitle("changed_title");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("changed_title", carrier.getDocuments().iterator().next()
				.getTitle());
	}

	@Test
	public void test_change_one_to_many_field_cascade_with_entity_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Document.class, "title"));
		setUserContext(user1);

		carrier.getDocuments().iterator().next().setTitle("changed_title");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("changed_title", carrier.getDocuments().iterator().next()
				.getTitle());

	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_many_field_cascade_with_wrong_entity_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Document.class, "id"));
		setUserContext(user1);

		carrier.getDocuments().iterator().next().setTitle("changed_title");
		carrier = (TestCarrier) merge(carrier);
	}

	@Test
	public void test_change_one_to_many_field_cascade_with_entity_object_field_permission() {
		permissionService.grant(
				admin,
				user1,
				getUpdateAction(),
				entityResourceFactory.createResource(carrier.getDocuments()
						.iterator().next()));
		setUserContext(user1);

		carrier.getDocuments().iterator().next().setTitle("changed_title");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("changed_title", carrier.getDocuments().iterator().next()
				.getTitle());

	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_many_field_cascade_with_wrong_entity_object_field_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Document.class, -1));
		setUserContext(user1);

		carrier.getDocuments().iterator().next().setTitle("changed_title");
		carrier = (TestCarrier) merge(carrier);

	}

	@Test
	public void test_change_one_to_many_cascade_add_new_with_entity_field_permission() {
		permissionService.grant(admin, user1, getAddAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						"documents"));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Document.class));
		setUserContext(user1);

		Document newDocument = new Document();
		carrier.getDocuments().add(newDocument);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(2, carrier.getDocuments().size());

	}

	@Test
	public void test_change_one_to_many_cascade_add_new_with_entity_field_permission_without_field_level_not_allowed() {
		permissionService.grant(admin, user1, getAddAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						"documents"));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Document.class));
		user1.getCompany().setFieldLevelEnabled(false);
		merge(user1.getCompany());
		setUserContext(user1);

		Document newDocument = new Document();
		carrier.getDocuments().add(newDocument);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(2, carrier.getDocuments().size());

	}

	@Test
	public void test_change_one_to_many_cascade_add_new_with_entity_field_permission_without_field_level() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Document.class));
		user1.getCompany().setFieldLevelEnabled(false);
		merge(user1.getCompany());
		setUserContext(user1);

		Document newDocument = new Document();
		carrier.getDocuments().add(newDocument);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(2, carrier.getDocuments().size());

	}

	// without add permission
	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_many_cascade_add_new_not_permitted() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Document.class));
		setUserContext(user1);

		Document newDocument = new Document();
		carrier.getDocuments().add(newDocument);
		carrier = (TestCarrier) merge(carrier);
	}

	// without add permission
	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_many_no_cascade_add_new_not_permitted() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		setUserContext(user1);

		Contact newContact = new Contact();
		persist(newContact);
		carrier.getContacts().add(newContact);
		carrier = (TestCarrier) merge(carrier);
	}

	@Test
	public void test_change_one_to_many_add_new_with_entity_object_permission() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		permissionService.grant(admin, user1, getAddAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						"contacts", carrier.getId()));
		setUserContext(user1);

		Contact newContact = new Contact();
		persist(newContact);
		carrier.getContacts().add(newContact);
		carrier = (TestCarrier) merge(carrier);

		assertEquals(2, carrier.getContacts().size());

	}

	@Test
	public void test_change_one_to_many_remove_with_entity_field_permission() {
		permissionService.grant(admin, user1, getRemoveAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						"contacts"));
		permissionService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Contact.class));
		setUserContext(user1);

		carrier.getContacts().remove(carrier.getContacts().iterator().next());
		carrier = (TestCarrier) merge(carrier);

		assertEquals(0, carrier.getContacts().size());
	}

	@Test
	public void test_change_one_to_many_remove_with_entity_field_permission_without_field_level() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		permissionService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Contact.class));
		user1.getCompany().setFieldLevelEnabled(false);
		merge(user1.getCompany());
		setUserContext(user1);

		carrier.getContacts().remove(carrier.getContacts().iterator().next());
		carrier = (TestCarrier) merge(carrier);

		assertEquals(0, carrier.getContacts().size());
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_one_to_many_remove_not_permitted() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Contact.class));
		setUserContext(user1);

		carrier.getContacts().remove(carrier.getContacts().iterator().next());
		carrier = (TestCarrier) merge(carrier);
	}

	// many-to-many - does not change many to many related entity property when
	// not cascaded
	@Test
	public void test_change_many_to_many_no_cascade_with_entity_permission() {
		setUserContext(user1);

		carrier.getGroups().iterator().next().setName("changed_name");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("Big group", carrier.getGroups().iterator().next()
				.getName());
	}

	@Test
	public void test_change_many_to_many_cascade_with_entity_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(CarrierTeam.class));
		setUserContext(user1);

		carrier.getTeams().iterator().next().setName("changed_name");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("changed_name", carrier.getTeams().iterator().next()
				.getName());
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_many_to_many_cascade_with_no_entity_permission() {
		setUserContext(user1);

		carrier.getTeams().iterator().next().setName("changed_name");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("changed_name", carrier.getTeams().iterator().next()
				.getName());
	}

	@Test
	public void test_change_many_to_many_no_cascade_add_new() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(CarrierGroup.class));
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		setUserContext(user1);
		CarrierGroup g2 = new CarrierGroup();
		persist(g2);

		carrier.getGroups().add(g2);
		int sizeBefore = carrier.getGroups().size();
		carrier = (TestCarrier) merge(carrier);
		assertEquals(sizeBefore, carrier.getGroups().size());
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_many_to_many_no_cascade_add_new_not_permitted() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(CarrierGroup.class));

		setUserContext(user1);
		CarrierGroup g2 = new CarrierGroup();
		persist(g2);

		carrier.getGroups().add(g2);
		carrier = (TestCarrier) merge(carrier);
	}

	@Test
	public void test_change_many_to_many_no_cascade_add_with_update_permission() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(CarrierGroup.class));
		user1.getCompany().setFieldLevelEnabled(false);
		merge(user1.getCompany());
		setUserContext(user1);
		CarrierGroup g2 = new CarrierGroup();
		persist(g2);

		carrier.getGroups().add(g2);
		carrier = (TestCarrier) merge(carrier);
	}

	// many2one- does not change entity property when not cascaded
	@Test
	public void test_change_many_to_one_no_cascade() {
		setUserContext(user1);

		carrier.getEmail().setSubject("A");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("Hey", carrier.getEmail().getSubject());
	}

	@Test
	public void test_change_many_to_one_cascade() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Email.class));
		setUserContext(user1);

		carrier.getEmailWithCascade().setSubject("A");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("A", carrier.getEmailWithCascade().getSubject());
	}

	@Test
	public void test_change_many_to_one_cascade_with_field_permissions() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Email.class, "subject"));
		setUserContext(user1);

		carrier.getEmailWithCascade().setSubject("A");
		carrier = (TestCarrier) merge(carrier);

		assertEquals("A", carrier.getEmailWithCascade().getSubject());
	}

	@Test
	public void test_change_many_to_one_no_cascade_with_entity_change() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory
						.createResource(TestCarrier.class, "email"));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Email.class));
		setUserContext(user1);

		Email newEmail = new Email();
		newEmail.setSubject("Changed_Email_Subject");
		carrier.setEmail(newEmail);
		persist(newEmail);
		carrier = (TestCarrier) merge(carrier);

		assertEquals("Changed_Email_Subject", carrier.getEmail().getSubject());
	}

	@Test(expected = PermissionActionException.class)
	public void test_change_many_to_one_no_cascade_without_entity_update_permission() {
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Email.class));
		setUserContext(user1);

		Email newEmail = new Email();
		newEmail.setSubject("Changed_Email_Subject");
		carrier.setEmail(newEmail);
		persist(newEmail);
		carrier = (TestCarrier) merge(carrier);
	}

	@Test
	public void test_change_many_to_one_cascade_with_entity_change() {
		permissionService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						"emailWithCascade"));
		permissionService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Email.class));
		setUserContext(user1);

		Email newEmail = new Email();
		newEmail.setSubject("Changed_Email_Subject");
		carrier.setEmailWithCascade(newEmail);
		carrier = (TestCarrier) merge(carrier);

		assertEquals("Changed_Email_Subject", carrier.getEmailWithCascade()
				.getSubject());
	}
}
