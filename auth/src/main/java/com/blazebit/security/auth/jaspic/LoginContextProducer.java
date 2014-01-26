package com.blazebit.security.auth.jaspic;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.blazebit.security.auth.config.LoginConfigFile;
import com.blazebit.security.auth.config.LoginModuleName;

/**
 * @author blep Date: 16/02/12 Time: 07:28
 */
@SessionScoped
public class LoginContextProducer implements Serializable {

    // ======================================
    // = Attributes =
    // ======================================

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected Logger log = Logger.getLogger(LoginContextProducer.class.getName());

    @Inject
    private SimpleCallbackHandler callbackHandler;

    private volatile LoginContext loginContext;
    private final Object LOCK = new Object();

    // ======================================
    // = Business methods =
    // ======================================

    @Produces
    public LoginContext produceLoginContext(@LoginConfigFile String loginConfigFileName, @LoginModuleName final String loginModuleName) throws LoginException, URISyntaxException {
        if (loginContext == null) {
            synchronized (LOCK) {
                if(loginContext == null) {
                    loginContext = createLoginContext(loginConfigFileName, loginModuleName);
                }
            }
        }
        return loginContext;
    }

    public LoginContext createLoginContext(String loginConfigFileName, final String loginModuleName) throws LoginException, URISyntaxException {
        // System.setProperty("java.security.auth.login.config", new
        // File(LoginContextProducer.class.getResource(loginConfigFileName).getFile()).getPath());
        try {
            return new LoginContext(loginModuleName, new Subject(), callbackHandler, new Configuration() {

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
            log.log(Level.SEVERE, "Failed to create login context!", e);
        }
        return loginContext;
    }
}
