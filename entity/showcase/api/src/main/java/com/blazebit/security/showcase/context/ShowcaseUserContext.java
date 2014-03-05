package com.blazebit.security.showcase.context;

import com.blazebit.security.entity.UserContext;
import com.blazebit.security.model.Company;
import com.blazebit.security.model.User;


public interface ShowcaseUserContext extends UserContext {

    public User getUser();
    
    public Company getCompany();
}
