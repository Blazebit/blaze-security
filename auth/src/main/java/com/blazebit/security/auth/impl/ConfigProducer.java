package com.blazebit.security.auth.impl;

import javax.enterprise.inject.Produces;

import com.blazebit.security.auth.ShowcaseSimpleServerLoginModule;

public class ConfigProducer {

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
