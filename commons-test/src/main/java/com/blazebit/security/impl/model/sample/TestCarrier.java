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
package com.blazebit.security.impl.model.sample;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.blazebit.security.IdHolder;
import com.blazebit.security.impl.model.ResourceName;

/**
 * 
 * @author cuszk
 */
@Entity
@ResourceName(name="TestCarrier")
public class TestCarrier implements Serializable, IdHolder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Integer id;
    // one2many fields
    private Set<Contact> contacts = new HashSet<Contact>();
    private Set<Document> documents = new HashSet<Document>();
    // many2one fields
    private Email email;
    private Email emailWithCascade;
    // primitive
    private String field;
    private String anotherField;
    // many2many
    private Set<CarrierGroup> groups = new HashSet<CarrierGroup>();
    private Set<CarrierTeam> teams = new HashSet<CarrierTeam>();
    // one2one field
    private Party party;
    private Party partyWithCascade;
    

    @OneToMany
    public Set<Contact> getContacts() {
        return contacts;
    }

    @OneToMany(cascade = CascadeType.ALL)
    public Set<Document> getDocuments() {
        return documents;
    }

    @ManyToOne
    @JoinColumn(name = "email")
    public Email getEmail() {
        return email;
    }

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "emailWithCascade")
    public Email getEmailWithCascade() {
        return emailWithCascade;
    }

    @Column
    public String getField() {
        return field;
    }

    @ManyToMany
    public Set<CarrierGroup> getGroups() {
        return this.groups;
    }

    @Id
    @GeneratedValue
    @Override
    public Integer getId() {
        return id;
    }

    @OneToOne
    public Party getParty() {
        return party;
    }

    @OneToOne(cascade = CascadeType.ALL)
    public Party getPartyWithCascade() {
        return partyWithCascade;
    }

    @ManyToMany(cascade = CascadeType.ALL)
    public Set<CarrierTeam> getTeams() {
        return teams;
    }

    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }

    public void setDocuments(Set<Document> contactsWithCascade) {
        this.documents = contactsWithCascade;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public void setEmailWithCascade(Email emailWithCascade) {
        this.emailWithCascade = emailWithCascade;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setGroups(Set<CarrierGroup> groups) {
        this.groups = groups;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public void setPartyWithCascade(Party partyWithCascade) {
        this.partyWithCascade = partyWithCascade;
    }

    public void setTeams(Set<CarrierTeam> teams) {
        this.teams = teams;
    }

    @Override
    public String toString() {
        return "Carrier{" + "id=" + id + '}';
    }

    @Column
    public String getAnotherField() {
        return anotherField;
    }

    public void setAnotherField(String anotherField) {
        this.anotherField = anotherField;
    }
}
