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

import javax.inject.Inject;

import org.junit.Ignore;
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

	@BeforeDatabaseAware
	public void init() {
		super.initData();

		carrier = new TestCarrier();
		carrier.setField("field1");
		persist(carrier);
		// one2one
		Party party = new Party();
		persist(party);
		carrierWithParty = new TestCarrier();
		carrierWithParty.setParty(party);
		persist(carrierWithParty);

		Party party2 = new Party();
		carrierWithPartyCascade = new TestCarrier();
		carrierWithPartyCascade.setPartyWithCascade(party2);
		persist(carrierWithPartyCascade);
		// one2many
		Contact contact = new Contact();
		persist(contact);
		carrierWithContacts = new TestCarrier();
		carrierWithContacts.getContacts().add(contact);
		persist(carrierWithContacts);

		Document document = new Document();
		carrierWithDocuments = new TestCarrier();
		document.setCarrier(carrierWithDocuments);
		carrierWithDocuments.getDocuments().add(document);
		persist(carrierWithDocuments);

		// many2many
		CarrierGroup group = new CarrierGroup();
		persist(group);
		carrierWithGroups = new TestCarrier();
		carrierWithGroups.getGroups().add(group);
		persist(carrierWithGroups);

		CarrierTeam team = new CarrierTeam();
		carrierWithTeams = new TestCarrier();
		carrierWithTeams.getTeams().add(team);
		persist(carrierWithTeams);
		// many2one
		Email email = new Email();
		persist(email);
		carrierWithEmail = new TestCarrier();
		carrierWithEmail.setEmail(email);
		persist(carrierWithEmail);

		Email email2 = new Email();
		carrierWithEmailCascade = new TestCarrier();
		carrierWithEmailCascade.setEmailWithCascade(email2);
		persist(carrierWithEmailCascade);

		setUserContext(user1);

		EntityFeatures.activateInterceptor();
	}

	@Test
	public void test_delete_entity_with_primitive_field() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrier);

		assertNull(entityManager.find(TestCarrier.class, carrier.getId()));
	}

	@Test
	public void test_delete_entity_with_primitive_field_with_object_permission() {
		securityService.grant(
				admin,
				user1,
				getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class,
						carrier.getId()));

		remove(carrier);

		assertNull(entityManager.find(TestCarrier.class, carrier.getId()));
	}

	// one2one
	@Test
	public void test_delete_entity_with_one_to_one_field_no_cascade() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithParty);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithParty.getId()));
		assertNotNull(entityManager.find(Party.class, carrierWithParty
				.getParty().getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_one_to_one_field_not_permitted() {

		remove(carrierWithParty);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithParty.getId()));
	}

	@Test
	public void test_delete_entity_with_one_to_one_field_cascade() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Party.class));

		remove(carrierWithPartyCascade);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithPartyCascade.getId()));
		assertNull(entityManager.find(Party.class, carrierWithPartyCascade
				.getPartyWithCascade().getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_one_to_one_field_cascade_not_permitted() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithPartyCascade);
	}

	// one2many
	@Test
	public void test_delete_entity_with_one_to_many_field() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithContacts);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithContacts.getId()));
		assertNotNull(entityManager.find(Contact.class, carrierWithContacts
				.getContacts().iterator().next().getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_one_to_many_field_not_permitted() {
		remove(carrierWithContacts);
	}

	@Test
	public void test_delete_entity_with_one_to_many_field_cascade() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Document.class));

		remove(carrierWithDocuments);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithDocuments.getId()));
	}

	@Test(expected = PermissionActionException.class)
	@Ignore("Since the parent annotation has been added to Document, I think this test depends on wrong assumptions")
	public void test_delete_entity_with_one_to_many_field_cascade_not_permitted() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithDocuments);

	}

	// many2many
	@Test
	public void test_delete_entity_with_many_to_many_field() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithGroups);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithGroups.getId()));
		assertNotNull(entityManager.find(CarrierGroup.class, carrierWithGroups
				.getGroups().iterator().next().getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_many_to_many_field_not_permitted() {
		remove(carrierWithGroups);
	}

	@Test
	public void test_delete_entity_with_many_to_many_field_cascade() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(CarrierTeam.class));

		remove(carrierWithTeams);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithTeams.getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_many_to_many_field_cascade_not_permitted() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithTeams);
	}

	// many2one
	@Test
	public void test_delete_entity_with_many_to_one_field() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithEmail);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithEmail.getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_many_to_one_field_not_permitted() {

		remove(carrierWithEmail);

	}

	@Test
	public void test_delete_entity_with_many_to_one_field_cascade() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Email.class));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithEmailCascade);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithEmailCascade.getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_many_to_one_field_cascade_not_permitted() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class));

		remove(carrierWithEmailCascade);
	}

	// again with object permissions
	@Test
	public void test_delete_entity_with_primitive_field_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrier));
		remove(carrier);
		assertNull(entityManager.find(TestCarrier.class, carrier.getId()));
	}

	@Test
	public void test_delete_entity_with_one_to_one_field_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithParty));
		remove(carrierWithParty);
		assertNull(entityManager.find(TestCarrier.class,
				carrierWithParty.getId()));
	}

	@Test
	public void test_delete_entity_with_one_to_one_field_cascade_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithPartyCascade));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithPartyCascade
						.getPartyWithCascade()));
		remove(carrierWithPartyCascade);
		assertNull(entityManager.find(TestCarrier.class,
				carrierWithPartyCascade.getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_one_to_one_field_cascade_with_missing_party_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithPartyCascade));
		remove(carrierWithPartyCascade);
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_one_to_one_field_with_wrong_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(TestCarrier.class, -1));
		remove(carrierWithParty);
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_one_to_one_field_cascade_with_wrong_party_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithPartyCascade));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Party.class, -1));
		remove(carrierWithPartyCascade);
	}

	@Test
	public void test_delete_entity_with_one_to_many_field_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithContacts));
		remove(carrierWithContacts);
		assertNull(entityManager.find(TestCarrier.class,
				carrierWithContacts.getId()));
	}

	@Test
	public void test_delete_entity_with_one_to_many_field_cascade_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithDocuments));
		securityService.grant(
				admin,
				user1,
				getDeleteAction(),
				entityResourceFactory.createResource(carrierWithDocuments
						.getDocuments().iterator().next()));

		remove(carrierWithDocuments);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithDocuments.getId()));
	}

	@Test(expected = PermissionActionException.class)
    @Ignore("Since the parent annotation has been added to Document, I think this test depends on wrong assumptions")
	public void test_delete_entity_with_one_to_many_field_cascade_with_missing_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithDocuments));

		remove(carrierWithDocuments);
	}

	@Test(expected = PermissionActionException.class)
    @Ignore("Since the parent annotation has been added to Document, I think this test depends on wrong assumptions")
	public void test_delete_entity_with_one_to_many_field_cascade_with_wrong_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithDocuments));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Document.class, -1));

		remove(carrierWithDocuments);
	}

	@Test
	public void test_delete_entity_with_many_to_many_field_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithGroups));

		remove(carrierWithGroups);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithGroups.getId()));
	}

	@Test
	public void test_delete_entity_with_many_to_many_field_cascade_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithTeams));
		securityService.grant(
				admin,
				user1,
				getDeleteAction(),
				entityResourceFactory.createResource(carrierWithTeams.getTeams()
						.iterator().next()));

		remove(carrierWithTeams);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithTeams.getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_many_to_many_field_cascade_with_wrong_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithTeams));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(CarrierTeam.class, -1));

		remove(carrierWithTeams);
	}

	@Test
	public void test_delete_entity_with_many_to_one_field_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithEmail));

		remove(carrierWithEmail);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithEmail.getId()));
	}

	@Test
	public void test_delete_entity_with_many_to_one_field_cascade_with_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithEmailCascade));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithEmailCascade
						.getEmailWithCascade()));

		remove(carrierWithEmailCascade);

		assertNull(entityManager.find(TestCarrier.class,
				carrierWithEmailCascade.getId()));
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_many_to_one_field_cascade_with_no_delete_party_entity_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithEmailCascade));

		remove(carrierWithEmailCascade);
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_entity_with_many_to_one_field_cascade_with_wrong_entity_object_permission() {
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(carrierWithEmailCascade));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Email.class, -1));

		remove(carrierWithEmailCascade);
	}

	@Test
	public void test_delete_contact_from_carrier() {
		securityService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Carrier.class));
		securityService.grant(admin, user1, getAddAction(),
				entityResourceFactory.createResource(Carrier.class, "contacts"));
		securityService.grant(admin, user1, getRemoveAction(),
				entityResourceFactory.createResource(Carrier.class, "contacts"));

		securityService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Contact.class));

		Carrier carrier = new Carrier();
		persist(carrier);

		Contact contact = new Contact();
		contact.setCarrier(carrier);
		persist(contact);

		remove(contact);
	}

	@Test
	public void test_delete_contact_from_carrier_field_level_disabled() {
		setUserContext(admin);
		user1.getCompany().setFieldLevelEnabled(false);
		merge(user1.getCompany());

		securityService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Carrier.class));
		securityService.grant(admin, user1, getUpdateAction(),
				entityResourceFactory.createResource(Carrier.class));

		securityService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Contact.class));

		setUserContext(user1);
		Carrier carrier = new Carrier();
		persist(carrier);

		Contact contact = new Contact();
		contact.setCarrier(carrier);
		persist(contact);

		remove(contact);
	}

	@Test(expected = PermissionActionException.class)
	public void test_delete_contact_from_carrier_not_allowed() {
		securityService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Carrier.class));
		securityService.grant(admin, user1, getAddAction(),
				entityResourceFactory.createResource(Carrier.class, "contacts"));

		securityService.grant(admin, user1, getCreateAction(),
				entityResourceFactory.createResource(Contact.class));
		securityService.grant(admin, user1, getDeleteAction(),
				entityResourceFactory.createResource(Contact.class));

		Carrier carrier = new Carrier();
		persist(carrier);

		Contact contact = new Contact();
		contact.setCarrier(carrier);
		persist(contact);

		remove(contact);
	}

}
