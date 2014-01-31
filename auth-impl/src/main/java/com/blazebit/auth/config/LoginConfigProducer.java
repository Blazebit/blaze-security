package com.blazebit.auth.config;

import java.io.Serializable;

import javax.enterprise.inject.Produces;

import com.blazebit.security.auth.config.LoginModuleName;
import com.blazebit.security.auth.login.ShowcaseSimpleServerLoginModule;

public class LoginConfigProducer implements Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	@LoginModuleName
	@Produces
	public String produceLoginModule() {
		return ShowcaseSimpleServerLoginModule.class.getName();
	}

}
