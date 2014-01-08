package com.blazebit.security.auth.config;

import java.io.Serializable;

import javax.enterprise.inject.Produces;

import com.blazebit.security.auth.login.ShowcaseSimpleServerLoginModule;

public class ConfigProducer implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @LoginModuleName
    @Produces
    public String produceLoginModule() {
        return ShowcaseSimpleServerLoginModule.class.getName();
    }

    //TODO not working
    @LoginConfigFile
    @Produces
    public String produceLoginConfigFilename() {
        return "/simpleServer.login";
    }

}
