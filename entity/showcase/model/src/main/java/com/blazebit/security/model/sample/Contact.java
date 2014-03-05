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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.blazebit.security.entity.EntityResourceType;
import com.blazebit.security.model.BaseEntity;
import com.blazebit.security.model.IdHolder;

/**
 * 
 * @author cuszk
 */
@Entity
@EntityResourceType(name = "Contact", module = "Carrier")
public class Contact extends BaseEntity<Integer> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String contactField;
    private Carrier carrier;
    private Carrier carrier2;

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContactField() {
        return contactField;
    }

    public void setContactField(String contactField) {
        this.contactField = contactField;
    }

    @Override
    public String toString() {
        return "Contact [id=" + id + ", contactField=" + contactField + "]";
    }

    @ManyToOne
    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    @ManyToOne
    public Carrier getCarrier2() {
        return carrier2;
    }

    public void setCarrier2(Carrier carrier2) {
        this.carrier2 = carrier2;
    }
    
    

}
