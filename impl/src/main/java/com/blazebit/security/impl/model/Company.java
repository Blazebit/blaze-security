package com.blazebit.security.impl.model;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.blazebit.security.IdHolder;

@Entity
@ResourceName(name = "Company", module = "Core")
public class Company implements Serializable, IdHolder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String name;

    public Company(String name) {
        this.name = name;
    }

    public Company() {
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
