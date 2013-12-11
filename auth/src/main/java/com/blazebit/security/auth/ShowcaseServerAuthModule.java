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
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.impl.service.resource.UserDataAccess;
import com.blazebit.security.impl.service.resource.UserGroupDataAccess;

/**
 * The actual Server Authentication Module AKA SAM.
 * http://arjan-tijms.blogspot.co.at/2012/11/implementing-container-authentication.html
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

    // TODO examples for other validateRequests: https://java.net/projects/nobis/sources/git/show/Nobis/authentication
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        if (requestPolicy.isMandatory()) {
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                PermissionManager permissionManager = BeanProvider.getContextualReference(PermissionManager.class);
                UserGroupDataAccess userGroupDataAccess = BeanProvider.getContextualReference(UserGroupDataAccess.class);
                UserDataAccess userDataAccess = BeanProvider.getContextualReference(UserDataAccess.class);
                User user = userDataAccess.findUser(Integer.valueOf(userPrincipal.getName()));
                if (user != null) {
                    // Create a handler to add the caller principal (AKA
                    // user principal)
                    // This will be the name of the principal returned by e.g.
                    // HttpServletRequest#getUserPrincipal
                    CallerPrincipalCallback callerPrincipalCallback = new CallerPrincipalCallback(clientSubject, String.valueOf(user.getId()));

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
                        handler.handle(new Callback[] { callerPrincipalCallback, groupPrincipalCallback });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (UnsupportedCallbackException ee) {
                        ee.printStackTrace();
                    }
                    return AuthStatus.SUCCESS;
                }
            }
        } else {
            // if requestPolicy is not mandatory, everything OK
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