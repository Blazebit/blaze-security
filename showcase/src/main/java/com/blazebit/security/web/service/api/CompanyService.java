package com.blazebit.security.web.service.api;

import java.util.List;

import com.blazebit.security.impl.model.Company;

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
