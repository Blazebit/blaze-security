package com.blazebit.security.auth.jaspic;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserModule;
import com.blazebit.security.impl.service.resource.UserDataAccess;

/**
 * The actual Server Authentication Module AKA SAM.
 * http://arjan-tijms.blogspot.co.at/2012/11/implementing-container-authentication.html
 * 
 */
public class ShowcaseServerAuthModule implements ServerAuthModule {

    private CallbackHandler callbackHandler;
    private Class<?>[] supportedMessageTypes = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
    private MessagePolicy requestPolicy;
    private Map options;

    private UserDataAccess userDataAccess;
    private String loginModuleName;

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, @SuppressWarnings("rawtypes") Map options) throws AuthException {
        this.callbackHandler = handler;
        this.requestPolicy = requestPolicy;
        this.options = options;
        this.userDataAccess = BeanProvider.getContextualReference(UserDataAccess.class);
        this.loginModuleName = (String) options.get("login-module-delegate");

    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {

        if (requestPolicy.isMandatory()) {
            // LoginContext is produced per session
            LoginContext loginContext = BeanProvider.getContextualReference(LoginContext.class);

            // get subject
            Subject subject = loginContext.getSubject();

            User user = getUser(subject);
            CallerPrincipalCallback callerPrincipalCallback = new CallerPrincipalCallback(clientSubject, user != null ? user : new Principal() {

                @Override
                public String getName() {
                    return "guest";
                }
            });
            List<String> roles = getUserRoles(subject);
            GroupPrincipalCallback groupPrincipalCallback = new GroupPrincipalCallback(clientSubject, roles.toArray(new String[roles.size()]));
            try {
                callbackHandler.handle(new Callback[] { callerPrincipalCallback, groupPrincipalCallback });
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnsupportedCallbackException ee) {
                ee.printStackTrace();
            }
            return AuthStatus.SUCCESS;
        } else {
            // if requestPolicy is not mandatory, everything OK
            return AuthStatus.SUCCESS;
        }
    }

    public User getUser(Subject subject) {
        Iterator<Principal> principals = subject.getPrincipals().iterator();
        User user = null;
        while (principals.hasNext()) {
            Principal next = principals.next();
            if (next instanceof User) {
                user = userDataAccess.findUser(Integer.valueOf(next.getName()));
            }
        }
        return user;
    }

    public List<String> getUserRoles(Subject subject) {
        List<String> roles = new ArrayList<String>();

        Iterator<Principal> principals = subject.getPrincipals().iterator();
        while (principals.hasNext()) {
            Principal next = principals.next();
            if (next instanceof UserModule) {
                if ("Roles".equals(next.getName())) {
                    Enumeration<? extends Principal> groups = ((UserModule) next).members();
                    while (groups.hasMoreElements()) {
                        String resourceName = groups.nextElement().getName();
                        roles.add(resourceName);
                    }
                }
            }
        }
        return roles;
    }

    /**
     * A compliant implementation should return HttpServletRequest and HttpServletResponse, so the delegation class
     * {@link ServerAuthContext} can choose the right SAM to delegate to. In this example there is only one SAM and thus the
     * return value actually doesn't matter here.
     */
    @Override
    public Class<?>[] getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    /**
     * WebLogic 12c calls this before Servlet is called, Geronimo v3 after, JBoss EAP 6 and GlassFish 3.1.2.2 don't call this at
     * all. WebLogic (seemingly) only continues if SEND_SUCCESS is returned, Geronimo completely ignores return value.
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        // Example: unwrap response
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        if (subject != null) {
            subject.getPrincipals().clear();
            subject.getPublicCredentials().clear();
            subject.getPrivateCredentials().clear();
        }
    }
}