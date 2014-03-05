package com.blazebit.security.showcase.service;

import java.util.List;

import com.blazebit.security.model.Company;

public interface CompanyService {

    /**
     * list of companies
     * 
     * @return
     */
    public List<Company> findCompanies();

    /**
     * merges company
     * 
     * @param selectedCompany
     * @return merged company
     */
    public Company saveCompany(Company selectedCompany);

}
