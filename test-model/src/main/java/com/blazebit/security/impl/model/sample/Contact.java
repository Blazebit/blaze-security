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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.blazebit.security.annotation.ResourceName;
import com.blazebit.security.model.BaseEntity;

/**
 * 
 * @author cuszk
 */
@Entity
@ResourceName(name = "Contact", module = "Carrier")
public class Contact implements Serializable, BaseEntity<Integer> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String contactField;
    private Carrier carrier;
    private Carrier carrier2;

    @Id
    @GeneratedValue
    @Override
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((contactField == null) ? 0 : contactField.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Contact other = (Contact) obj;
        if (contactField == null) {
            if (other.contactField != null)
                return false;
        } else if (!contactField.equals(other.contactField))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
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
