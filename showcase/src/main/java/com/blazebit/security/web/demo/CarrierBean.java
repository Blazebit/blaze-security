package com.blazebit.security.web.demo;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.faces.bean.ViewScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.Resource;
import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.bean.SecurityBaseBean;

@Named
@ViewScoped
@Stateless
public class CarrierBean extends SecurityBaseBean {

    private List<Carrier> carriers = new ArrayList<Carrier>();

    @PersistenceContext
    EntityManager entityManager;

    private Carrier newCarrier = new Carrier();
    private Carrier selectedCarrier = new Carrier();

    private List<Contact> contacts = new ArrayList<Contact>();
    private Contact newContact = new Contact();
    private Contact selectedContact;

    private Party party;

    @PostConstruct
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
        setContacts(new ArrayList<Contact>(selectedCarrier.getContacts()));
    }

    public void selectContact(Contact contact) {
        setSelectedContact(contact);
    }

    public void saveNewCarrier() {
        entityManager.persist(newCarrier);
        selectCarrier(newCarrier);
        init();
        newCarrier = new Carrier();
       
    }

    public void saveNewContact() {
        entityManager.persist(newContact);
        selectedCarrier.getContacts().add(newContact);
        entityManager.merge(selectedCarrier);
        setContacts(new ArrayList<Contact>(selectedCarrier.getContacts()));
        newContact = new Contact();
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

    @Override
    public Resource getResource() {
        return entityFieldFactory.createResource(Carrier.class);
    }

    @Override
    public Resource getResource(String field) {
        return entityFieldFactory.createResource(Carrier.class, field);
    }

    @Override
    public Resource getResource(Integer id) {
        return entityFieldFactory.createResource(Carrier.class, id);
    }

    @Override
    public Resource getResource(String field, Integer id) {
        return entityFieldFactory.createResource(Carrier.class, field, id);
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
}
