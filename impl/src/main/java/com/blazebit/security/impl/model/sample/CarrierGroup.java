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

import com.blazebit.security.IdHolder;
import com.blazebit.security.impl.model.ResourceName;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

/**
 * 
 * @author cuszk
 */
@Entity
@ResourceName(name = "Carrier group", module="Carrier")
public class CarrierGroup implements IdHolder, Serializable {

    private Integer id;
    private String name;
    private Set<Carrier> carriers = new HashSet<Carrier>();

    @Override
    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToMany(mappedBy = "groups")
    public Set<Carrier> getCarriers() {
        return this.carriers;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setCarriers(Set<Carrier> carriers) {
        this.carriers = carriers;
    }
}
