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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
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
@ResourceName(name = "Carrier", module = "Carrier")
public class Carrier implements Serializable, IdHolder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String field1;
    private String field2;
    private String field3;
    private String field4;
    private String field5;
    private Party party;
    private Set<Contact> contacts = new HashSet<Contact>();
    private Set<CarrierGroup> groups = new HashSet<CarrierGroup>();
    private Email email;
    private Comment comment;

    // private Set<CarrierTeam> teams = new HashSet<CarrierTeam>();

    @ManyToOne
    @JoinColumn(name = "comment")
    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    @Id
    @GeneratedValue
    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    @Basic
    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    @Basic
    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    @Basic
    public String getField4() {
        return field4;
    }

    public void setField4(String field4) {
        this.field4 = field4;
    }

    @Basic
    public String getField5() {
        return field5;
    }

    public void setField5(String field5) {
        this.field5 = field5;
    }

    @OneToOne
    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    @OneToMany
    public Set<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }

    // @OneToMany(
    // mappedBy = "carrier")
    // public Set<Email> getEmails() {
    // return emails;
    // }
    // public void setEmails(Set<Email> emails) {
    // this.emails = emails;
    // }
    @ManyToMany
    public Set<CarrierGroup> getGroups() {
        return this.groups;
    }

    // @ManyToMany(mappedBy = "carriers")
    // public Set<CarrierTeam> getTeams() {
    // return this.teams;
    // }
    public void setGroups(Set<CarrierGroup> groups) {
        this.groups = groups;
    }

    // public void setTeams(Set<CarrierTeam> teams) {
    // this.teams = teams;
    // }
    @Override
    public String toString() {
        return "Carrier{" + "id=" + id + '}';
    }

    @ManyToOne
    @JoinColumn(name = "email")
    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }
}
