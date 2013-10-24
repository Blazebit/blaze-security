package com.blazebit.security.web.demo;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.Resource;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.Comment;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.bean.SecurityBaseBean;
import com.blazebit.security.web.util.WebUtil;

@Named
@ViewScoped
@Stateless
public class CarrierBean extends SecurityBaseBean {

    private List<Carrier> carriers = new ArrayList<Carrier>();

    @PersistenceContext
    EntityManager entityManager;

    private Carrier newCarrier = new Carrier();
    private Carrier selectedCarrier;

    private List<Contact> contacts = new ArrayList<Contact>();
    private Contact newContact = new Contact();
    private Contact selectedContact;

    private List<CarrierGroup> groups = new ArrayList<CarrierGroup>();
    private CarrierGroup newGroup = new CarrierGroup();

    public List<CarrierGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<CarrierGroup> groups) {
        this.groups = groups;
    }

    public CarrierGroup getNewGroup() {
        return newGroup;
    }

    public void setNewGroup(CarrierGroup newGroup) {
        this.newGroup = newGroup;
    }

    public CarrierGroup getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(CarrierGroup selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    private CarrierGroup selectedGroup;

    private Party party;

    public void init() {
        setCarriers(entityManager.createQuery("select carrier from " + Carrier.class.getCanonicalName() + " carrier", Carrier.class).getResultList());
    }

    public void tabChange() {
        if (party == null) {
            party = new Party();
        }
    }

    public void selectCarrier(Carrier carrier) {
        selectedCarrier = entityManager.find(Carrier.class, carrier.getId());
        party = selectedCarrier.getParty();
        contacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
    }

    public void selectContact(Contact contact) {
        setSelectedContact(contact);
    }

    public void selectGroup(CarrierGroup group) {
        selectedGroup = group;
    }

    public void saveNewCarrier() {
        entityManager.persist(newCarrier);
        init();
        newCarrier = new Carrier();
    }

    public void saveCarrier() {
        entityManager.merge(selectedCarrier);
        init();

    }

    public void saveNewContact() {
        entityManager.persist(newContact);
        selectedCarrier.getContacts().add(newContact);
        selectedCarrier = entityManager.merge(selectedCarrier);
        contacts = new ArrayList<Contact>(selectedCarrier.getContacts());
        newContact = new Contact();
    }

    public void saveNewGroup() {
        newGroup.getCarriers().add(selectedCarrier);
        entityManager.persist(newGroup);
        selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
        selectedCarrier.getGroups().add(newGroup);
        selectedCarrier = entityManager.merge(selectedCarrier);
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
        newGroup = new CarrierGroup();
    }

    public void deleteGroup(CarrierGroup carrierGroup) {
        if (carrierGroup.equals(selectedGroup)) {
            selectedGroup = null;
        }
        entityManager.remove(entityManager.find(CarrierGroup.class, carrierGroup.getId()));
        selectedCarrier.getGroups().remove(carrierGroup);
        selectedCarrier = entityManager.merge(selectedCarrier);
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
    }

    public void deleteContact(Contact contact) {
        if (contact.equals(selectedContact)) {
            selectedContact = null;
        }
        entityManager.remove(entityManager.find(Contact.class, contact.getId()));
        selectedCarrier.getContacts().remove(contact);
        selectedCarrier = entityManager.merge(selectedCarrier);
        contacts = new ArrayList<Contact>(selectedCarrier.getContacts());

    }

    public void deleteCarrier(Carrier carrier) {
        if (carrier.equals(selectedCarrier)) {
            selectedCarrier = null;
        }
        entityManager.remove(entityManager.find(Carrier.class, carrier.getId()));
        init();
    }

    public void saveParty() {
        if (party.getId() == null) {
            entityManager.persist(party);
        } else {
            entityManager.merge(party);
        }
        selectedCarrier.setParty(party);
        entityManager.merge(selectedCarrier);
        // selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
    }

    public void saveContact() {
        entityManager.merge(selectedContact);
        selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
        setContacts(new ArrayList<Contact>(selectedCarrier.getContacts()));
    }

    public void saveGroup() {
        entityManager.merge(selectedGroup);
        selectedCarrier = entityManager.find(Carrier.class, selectedCarrier.getId());
        groups = new ArrayList<CarrierGroup>(selectedCarrier.getGroups());
    }

    public Carrier getSelectedCarrier() {
        return selectedCarrier;
    }

    public void setSelectedCarrier(Carrier selectedCarrier) {
        this.selectedCarrier = selectedCarrier;
    }

    public List<Carrier> getCarriers() {
        return carriers;
    }

    public void setCarriers(List<Carrier> carriers) {
        this.carriers = carriers;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public Contact getSelectedContact() {
        return selectedContact;
    }

    public void setSelectedContact(Contact selectedContact) {
        this.selectedContact = selectedContact;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public Carrier getNewCarrier() {
        return newCarrier;
    }

    public Contact getNewContact() {
        return newContact;
    }

    public void setNewContact(Contact newContact) {
        this.newContact = newContact;
    }

    public void setNewCarrier(Carrier newCarrier) {
        this.newCarrier = newCarrier;
    }

    public void grant(Carrier carrier, String field) {
        WebUtil.redirect(FacesContext.getCurrentInstance(), "/blaze-security-showcase/resource/resources.xhtml?id=" + carrier.getId() + "&resource=" + carrier.getClass().getName()
            + "&field=" + field, false);
    }

    public void grant(Carrier carrier) {
        WebUtil.redirect(FacesContext.getCurrentInstance(),
                         "/blaze-security-showcase/resource/resources.xhtml?id=" + carrier.getId() + "&resource=" + carrier.getClass().getName(), false);
    }

    public void revoke(Carrier carrier) {
    }

    public void revoke(Carrier carrier, String field) {
    }
}
