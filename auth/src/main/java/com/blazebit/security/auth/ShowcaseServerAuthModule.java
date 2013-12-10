package com.blazebit.security.auth;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.context.UserContext;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.service.resource.UserGroupDataAccess;

/**
 * The actual Server Authentication Module AKA SAM.
 * 
 */
public class ShowcaseServerAuthModule implements ServerAuthModule {

    private CallbackHandler handler;
    private Class<?>[] supportedMessageTypes = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
    private MessagePolicy requestPolicy;

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, @SuppressWarnings("rawtypes") Map options) throws AuthException {
        this.handler = handler;
        this.requestPolicy = requestPolicy;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        System.out.println("Session = " + request.getSession().getId());
        System.out.println("\n\n**** validateRequest ****\n\n");

        if (requestPolicy.isMandatory()) {
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                // Authenticate a received service request. This method is called to transform the mechanism-specific request
                // message
                // acquired by calling getRequestMessage (on messageInfo) into the validated application message to be returned
                // to the
                // message processing runtime. If the received message is a (mechanism-specific) meta-message, the method
                // implementation
                // must attempt to transform the meta-message into a corresponding mechanism-specific response message, or to
                // the
                // validated application request message. The runtime will bind a validated application message into the the
                // corresponding service invocation.

                // login??

            } else {
                // logged in
                UserContext userContext = BeanProvider.getContextualReference(UserContext.class);
                PermissionManager permissionManager = BeanProvider.getContextualReference(PermissionManager.class);
                UserGroupDataAccess userGroupDataAccess = BeanProvider.getContextualReference(UserGroupDataAccess.class);
                User user = userContext.getUser();
                if (user != null) {
                    // Normally we would check here for authentication credentials being
                    // present and perform actual authentication, or in absence of those
                    // ask the user in some way to authenticate.

                    // Create a handler to add the caller principal (AKA
                    // user principal)
                    // This will be the name of the principal returned by e.g.
                    // HttpServletRequest#getUserPrincipal
                    PasswordValidationCallback pvcb = new PasswordValidationCallback(clientSubject, user.getUsername(), user.getPassword() != null ? user
                        .getPassword()
                        .toCharArray() : "".toCharArray());

                    CallerPrincipalCallback callerPrincipalCallback = new CallerPrincipalCallback(clientSubject, user.getUsername());

                    List<UserGroup> groups = userGroupDataAccess.getGroupsForUser(user);
                    List<String> resourceNames = permissionManager.getPermissionResources(user);

                    for (UserGroup userGroup : groups) {
                        resourceNames.add(userGroup.getName());
                    }

                    // Create a handler to add the groups
                    // This is what e.g. HttpServletRequest#isUserInRole and @RolesAllowed test for
                    GroupPrincipalCallback groupPrincipalCallback = new GroupPrincipalCallback(clientSubject, resourceNames.toArray(new String[resourceNames.size()]));

                    // Execute the handlers we created above. This will typically add the
                    // principal and the
                    // role in an application server specific way to the JAAS Subject.
                    try {
                        handler.handle(new Callback[] { callerPrincipalCallback, groupPrincipalCallback, pvcb });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (UnsupportedCallbackException ee) {
                        ee.printStackTrace();
                    }
                    // Extra: wrap response -> in secureResponse unwrap it
                    // https://github.com/arjantijms/jaspic-capabilities-test/blob/master/wrapping/src/main/java/org/omnifaces/jaspictest/sam/TestWrappingServerAuthModule.java
                    return AuthStatus.SUCCESS;
                }
            }
        } else {
            return AuthStatus.SUCCESS;
        }

        return AuthStatus.FAILURE;
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

    }
}