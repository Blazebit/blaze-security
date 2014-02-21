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

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.blazebit.security.annotation.ResourceName;
import com.blazebit.security.model.BaseEntity;

/**
 * 
 * @author cuszk
 */
@Entity
@ResourceName(name = "Email", module = "DM")
public class Email implements Serializable, BaseEntity<Integer> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String subject;
    private String body;

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
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Basic
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
