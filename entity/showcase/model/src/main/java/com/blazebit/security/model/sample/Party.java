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
package com.blazebit.security.model.sample;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.blazebit.security.entity.EntityResourceType;
import com.blazebit.security.model.BaseEntity;
import com.blazebit.security.model.IdHolder;

/**
 * 
 * @author cuszk
 */
@Entity
@EntityResourceType(name = "Party", module = "Core", test = "object.type eq 'Core'")
public class Party  extends BaseEntity<Integer> {

    @Override
    public String toString() {
        return "Party [id=" + id + ", partyField1=" + partyField1 + ", partyField2=" + partyField2 + ", type=" + type + "]";
    }

    public Party() {
        this.type = "Core";
    }

    public Party(String type) {
        this.type = type;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String partyField1;
    private String partyField2;
    private String type;

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    public String getPartyField1() {
        return partyField1;
    }

    public void setPartyField1(String partyField1) {
        this.partyField1 = partyField1;
    }

    @Basic
    public String getPartyField2() {
        return partyField2;
    }

    public void setPartyField2(String partyField2) {
        this.partyField2 = partyField2;
    }

    @Basic(optional = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
