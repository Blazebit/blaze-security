package com.blazebit.security.web.service.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.impl.model.Company;
import com.blazebit.security.web.service.api.CompanyService;

@Stateless
public class CompanyServiceImpl implements CompanyService {

    @PersistenceContext(unitName = "TestPU")
    EntityManager entityManager;

    @Override
    public List<Company> findCompanies() {
        return entityManager.createQuery("SELECT company FROM " + Company.class.getCanonicalName() + " company order by company.name", Company.class).getResultList();
    }

    @Override
    public Company saveCompany(Company selectedCompany) {
        Company company = entityManager.merge(selectedCompany);
        return company;
    }

}
