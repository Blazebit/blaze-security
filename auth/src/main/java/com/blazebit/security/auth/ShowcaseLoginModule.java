package com.blazebit.security.auth;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.spi.LoginModule;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.service.resource.UserDataAccess;
import com.blazebit.security.impl.service.resource.UserGroupDataAccess;

public class ShowcaseLoginModule implements LoginModule {

    Subject subject;
    CallbackHandler callbackHandler;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    private boolean authenticated;

    @Override
    public boolean login() throws LoginException {
        authenticated = false;

        Callback[] cb = new Callback[2];
        NameCallback nameCallback = new NameCallback("name: ");
        PasswordCallback passwordCallback = new PasswordCallback("password: ", true);
        cb[0] = nameCallback;
        cb[1] = passwordCallback;

        try {
            callbackHandler.handle(cb);

            String userId = nameCallback.getName();
            char[] pass = passwordCallback.getPassword();
            // TODO validate!

            authenticated = true;
        } catch (IOException e) {
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            throw new LoginException(e.getMessage());
        }
        return authenticated;
    }

    @Override
    public boolean commit() throws LoginException {
        return authenticated;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        authenticated = false;
        return true;
    }

}
