package com.blazebit.security.auth;

import java.io.IOException;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.jacc.WebUserDataPermission;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.security.Permission;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.service.resource.UserDataAccess;

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
            initSecurity(userId);
            authenticated = true;
        } catch (IOException e) {
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            throw new LoginException(e.getMessage());
        }
        return authenticated;
    }

    private void initSecurity(final String userId) {

        // Setup the PermissionCollection for this web app context
        // based on the permissions configured for the root of the
        // web app context directory, then add a file read permission
        // for that directory.
        Policy policy = Policy.getPolicy();
        if (policy != null) {
            CodeSource codeSource = new CodeSource(null, (Certificate[]) null);
            Principal principals[] = new Principal[] { new Principal() {

                @Override
                public String getName() {
                    return userId;
                }
            } };
            ProtectionDomain pd = new ProtectionDomain(codeSource, null, null, principals);
            PermissionCollection permissionCollection = policy.getPermissions(pd);
            PermissionManager permissionManager = BeanProvider.getContextualReference(PermissionManager.class);

            UserDataAccess userDataAccess = BeanProvider.getContextualReference(UserDataAccess.class);
            User user = userDataAccess.findUser(Integer.valueOf(userId));
            if (user != null) {
                List<Permission> permissions = permissionManager.getPermissions(user);
                for (Permission permission : permissions) {
                    WebUserDataPermission webudp = new WebUserDataPermission(((EntityField) permission.getResource()).getEntity(),
                        ((EntityAction) permission.getAction()).getActionName());
                    permissionCollection.add(webudp);
                }
            }

        }
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
