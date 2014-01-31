package com.blazebit.security.auth.jaspic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessagePolicy.TargetPolicy;
import javax.security.auth.message.ServerAuth;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 * The Server Authentication Context is an extra (required) indirection between the Application Server and the actual Server
 * Authentication Module (SAM). This can be used to encapsulate any number of SAMs and either select one at run-time, invoke
 * them all in order, etc.
 * <p>
 * Since this simple example only has a single SAM, we delegate directly to that one. Note that this {@link ServerAuthContext}
 * and the {@link ServerAuthModule} (SAM) share a common base interface: {@link ServerAuth}.
 * 
 */
public class MainServerAuthContext implements ServerAuthContext {

    private ServerAuthModule serverAuthModule;
    private List<ServerAuthModule> serverAuthModules = new ArrayList<ServerAuthModule>();

    public MainServerAuthContext(CallbackHandler handler) throws AuthException {
        // use the one existing authentication module.
        serverAuthModule = new ShowcaseServerAuthModule();
        MessagePolicy policy = new MessagePolicy(new TargetPolicy[] {}, true);
        Map<String, String> options = new HashMap<String, String>();
        serverAuthModule.initialize(policy, policy, handler, Collections.<String, String>emptyMap());
        //if there are more auth modules
        serverAuthModules.add(serverAuthModule);
    }

    /**
     * Invoke validateRequest. ServerAuthContext implementation invokes validateRequest of one or more encapsulated
     * ServerAuthModules. Modules validate credentials present in request (for example, decrypt and verify a signature).
     */

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        return serverAuthModule.validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return serverAuthModule.secureResponse(messageInfo, serviceSubject);
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        serverAuthModule.cleanSubject(messageInfo, subject);
    }

}