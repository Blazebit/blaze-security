package com.blazebit.security.auth.impl;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.logging.Logger;

/**
 * @author blep Date: 16/02/12 Time: 07:28
 */
public class LoginContextProducer {

    // ======================================
    // = Attributes =
    // ======================================

    protected Logger log = Logger.getLogger(LoginContextProducer.class);

    @Inject
    private SimpleCallbackHandler callbackHandler;

    // ======================================
    // = Business methods =
    // ======================================

    @Produces
    public LoginContext produceLoginContext(@LoginConfigFile String loginConfigFileName, @LoginModuleName final String loginModuleName) throws LoginException, URISyntaxException {

        // System.setProperty("java.security.auth.login.config", new
        // File(LoginContextProducer.class.getResource(loginConfigFileName).getFile()).getPath());

        try {
            return new LoginContext(loginModuleName, null, callbackHandler, new Configuration() {

                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    AppConfigurationEntry[] entry = new AppConfigurationEntry[1];
                    Map<String, String> options = new HashMap<String, String>();
                    options.put("password-stacking", "useFirstPass");
                    entry[0] = new AppConfigurationEntry(loginModuleName, LoginModuleControlFlag.REQUIRED, options);
                    return entry;
                }

            });
        } catch (Exception e) {
            log.error("Failed to create login context!", e);
            return null;
        }
    }
}
