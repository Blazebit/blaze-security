package com.blazebit.security.web.service.impl;

import java.util.List;

import javax.persistence.EntityManager;

import javax.persistence.PersistenceContext;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.web.service.api.CompanyService;

public class CompanyServiceImpl implements CompanyService {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<Company> findCompanies() {
        return entityManager.createQuery("SELECT company FROM " + Company.class.getCanonicalName() + " company order by company.name", Company.class).getResultList();
    }

}