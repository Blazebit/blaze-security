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
import javax.persistence.ManyToOne;

import com.blazebit.security.entity.Parent;
import com.blazebit.security.entity.EntityResourceType;
import com.blazebit.security.model.BaseEntity;
import com.blazebit.security.model.IdHolder;

/**
 * 
 * @author cuszk
 */
@Entity
@EntityResourceType(name = "Document", module = "DM")
public class Document extends BaseEntity<Integer> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String content;
    private String title;
    private Integer size;
    private TestCarrier carrier;

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Basic
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @ManyToOne
    @Parent
	public TestCarrier getCarrier() {
		return carrier;
	}

	public void setCarrier(TestCarrier carrier) {
		this.carrier = carrier;
	}

}
