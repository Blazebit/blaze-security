package com.blazebit.security.auth.impl;

import java.net.URISyntaxException;
import java.util.Collections;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * @author blep Date: 16/02/12 Time: 07:28
 */
public class LoginContextProducer {

    // ======================================
    // = Attributes =
    // ======================================

    @Inject
    private SimpleCallbackHandler callbackHandler;

    // ======================================
    // = Business methods =
    // ======================================

    @Produces
    public LoginContext produceLoginContext(@LoginConfigFile String loginConfigFileName, @LoginModuleName final String loginModuleName) throws LoginException, URISyntaxException {

        //System.setProperty("java.security.auth.login.config", new File(LoginContextProducer.class.getResource(loginConfigFileName).getFile()).getPath());

        try {
            return new LoginContext(loginModuleName, null, callbackHandler, new Configuration() {

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    AppConfigurationEntry[] entry = new AppConfigurationEntry[1];
                    entry[0] = new AppConfigurationEntry("com.blazebit.security.auth."+loginModuleName, LoginModuleControlFlag.REQUIRED, Collections.EMPTY_MAP);
                    return entry;
                }

            });
        } catch (Exception e) {
            System.out.println("ouch!!!");
            return null;
        }
    }
}
