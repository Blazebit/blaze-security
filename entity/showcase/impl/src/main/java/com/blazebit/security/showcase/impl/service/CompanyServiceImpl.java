package com.blazebit.security.showcase.impl.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.model.Company;
import com.blazebit.security.showcase.service.CompanyService;

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
