package com.blazebit.security.auth.impl;

import javax.enterprise.inject.Produces;

public class ConfigProducer {

    @LoginModuleName
    @Produces
    public String produceLoginModule() {
        return "ShowcaseSimpleServerLoginModule";
    }

    @LoginConfigFile
    @Produces
    public String produceLoginConfigFilename() {
        return "/simpleServer.login";
    }

}
