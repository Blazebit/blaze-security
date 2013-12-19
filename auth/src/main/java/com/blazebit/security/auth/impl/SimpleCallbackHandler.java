package com.blazebit.security.auth.impl;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.blazebit.security.auth.model.Credentials;

/**
 * @author blep Date: 12/02/12 Time: 12:29
 */

@Named
public class SimpleCallbackHandler implements CallbackHandler {

    // ======================================
    // = Attributes =
    // ======================================

    @Inject
    @RequestScoped
    private Credentials credentials;

    // ======================================
    // = Business methods =
    // ======================================

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callback;
                nameCallback.setName(credentials.getLogin());
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback) callback;
                passwordCallback.setPassword(credentials.getPassword().toCharArray());
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}