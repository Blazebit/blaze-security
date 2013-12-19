package com.blazebit.security.auth;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.log4j.Logger;
import org.jboss.security.SimpleGroup;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserModule;
import com.blazebit.security.impl.service.resource.UserDataAccess;

public class ShowcaseSimpleServerLoginModule implements LoginModule {

    protected Logger log = Logger.getLogger(ShowcaseSimpleServerLoginModule.class);

    private Subject user;
    private boolean loginOk;
    private javax.security.auth.Subject subject;
    private boolean useFirstPass;
    private Map sharedState;
    private CallbackHandler callbackHandler;

    private char[] credential;

    @Override
    public void initialize(javax.security.auth.Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.sharedState = sharedState;
        this.callbackHandler = callbackHandler;
        // init
        useFirstPass = true;
    }

    @Override
    public boolean login() throws LoginException {
        loginOk = false;
        // If useFirstPass is true, look for the shared password
        if (useFirstPass == true) {
            try {
                Object identity = sharedState.get("javax.security.auth.login.name");
                Object credential = sharedState.get("javax.security.auth.login.password");
                if (identity != null && credential != null) {
                    loginOk = true;
                    // Setup our view of the user
                    Object username = identity;
                    // if (username instanceof Principal)
                    // identity = (Principal) username;
                    // else {
                    String name = username.toString();
                    try {
                        identity = createIdentity(name);
                    } catch (Exception e) {
                        LoginException le = new LoginException("Failed to create subject!");
                        throw le;
                        // }
                    }
                    Object password = credential;
                    if (password instanceof char[])
                        credential = (char[]) password;
                    else if (password != null) {
                        String tmp = password.toString();
                        credential = tmp.toCharArray();
                    }
                    return true;
                }
                // retry??
                loginOk = false;
                String[] info = getUsernameAndPassword();
                String username = info[0];
                String password = info[1];

                // validate the retrieved username and password.
                if (validateUsernameAndPassword(username, password)) {

                    if (useFirstPass == true) { // Add the principal and password to the shared state map
                        sharedState.put("javax.security.auth.login.name", username);
                        sharedState.put("javax.security.auth.login.password", password);
                        user = (Subject) createIdentity(username);
                        if (user != null) {
                            loginOk = true;
                            return true;
                        }
                    }
                }
                // Else, fall through and perform the login
            } catch (Exception e) { // Dump the exception and continue
                log.error("Login failed", e);
            }
        }
        return false;

    }

    protected Principal getIdentity() {
        return user;
    }

    protected Group[] getRoleSets() throws LoginException {
        PermissionManager permissionManager = BeanProvider.getContextualReference(PermissionManager.class);
        Group[] roleSets = { new UserModule("Roles") };
        Set<String> modules = permissionManager.getPermissionModules(user);
        for (String module : modules) {
            roleSets[0].addMember(new UserModule(module));
        }
        return roleSets;
    }

    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        // add password validation
        return true;
    }

    /**
     * Called by login() to acquire the username and password strings for authentication. This method does no validation of
     * either.
     * 
     * @return String[], [0] = username, [1] = password
     * @exception LoginException thrown if CallbackHandler is not set or fails.
     */
    protected String[] getUsernameAndPassword() throws LoginException {
        String[] info = { null, null };
        // prompt for a username and password
        if (callbackHandler == null) {
            throw new LoginException("No callbackhandler found");
        }

        NameCallback nc = new NameCallback("Enter the username: ", "guest");
        PasswordCallback pc = new PasswordCallback("Enter the password: ", false);
        Callback[] callbacks = { nc, pc };
        String username = null;
        String password = null;
        try {
            callbackHandler.handle(callbacks);
            username = nc.getName();
            char[] tmpPassword = pc.getPassword();
            if (tmpPassword != null) {
                credential = new char[tmpPassword.length];
                System.arraycopy(tmpPassword, 0, credential, 0, tmpPassword.length);
                pc.clearPassword();
                password = new String(credential);
            }
        } catch (IOException e) {
            LoginException le = new LoginException("Callbackhandler cannot be invoked");
            le.initCause(e);
            throw le;
        } catch (UnsupportedCallbackException e) {
            LoginException le = new LoginException();
            le.initCause(e);
            throw le;
        }
        info[0] = username;
        info[1] = password;
        return info;
    }

    private boolean validateUsernameAndPassword(String username, String password) {
        return validatePassword(password, null);
    }

    protected Principal createIdentity(String userId) throws Exception {
        UserDataAccess userDataAccess = BeanProvider.getContextualReference(UserDataAccess.class);
        User user = userDataAccess.findUser(Integer.valueOf(userId));
        return user;
    }

    @Override
    public boolean commit() throws LoginException {
        if (loginOk == false)
            return false;

        Set<Principal> principals = subject.getPrincipals();
        Principal identity = getIdentity();
        principals.add(identity);
        // add role groups returned by getRoleSets.
        Group[] roleSets = getRoleSets();
        for (int g = 0; g < roleSets.length; g++) {
            Group group = roleSets[g];
            String name = group.getName();
            Group subjectGroup = createGroup(name, principals);

            // UserModule tmp = new UserModule("Roles");
            // subjectGroup.addMember(tmp);
            // subjectGroup = tmp;

            // Copy the group members to the Subject group
            Enumeration<? extends Principal> members = group.members();
            while (members.hasMoreElements()) {
                Principal role = (Principal) members.nextElement();
                subjectGroup.addMember(role);
            }
        }
        // add the CallerPrincipal group if none has been added in getRoleSets
        Group callerGroup = getCallerPrincipalGroup(principals);
        if (callerGroup == null) {
            callerGroup = new UserModule("CallerPrincipal");
            callerGroup.addMember(identity);
            principals.add(callerGroup);
        }
        return true;
    }

    protected Group getCallerPrincipalGroup(Set<Principal> principals) {
        Group callerGroup = null;
        for (Principal principal : principals) {
            if (principal instanceof Group) {
                Group group = Group.class.cast(principal);
                if (group.getName().equals("CallerPrincipal")) {
                    callerGroup = group;
                    break;
                }
            }
        }
        return callerGroup;
    }

    /**
     * Find or create a Group with the given name. Subclasses should use this method to locate the 'Roles' group or create
     * additional types of groups.
     * 
     * @return A named Group from the principals set.
     */
    protected Group createGroup(String name, Set<Principal> principals) {
        Group roles = null;
        Iterator<Principal> iter = principals.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if ((next instanceof Group) == false)
                continue;
            Group grp = (Group) next;
            if (grp.getName().equals(name)) {
                roles = grp;
                break;
            }
        }
        // If we did not find a group create one
        if (roles == null) {
            roles = new UserModule(name);
            principals.add(roles);
        }
        return roles;
    }

    @Override
    public boolean logout() throws LoginException {
        Group[] groups = this.getRoleSets();
        subject.getPrincipals().remove(groups[0]);
        Principal identity = getIdentity();
        Set<Principal> principals = subject.getPrincipals();
        principals.remove(identity);
        Group callerGroup = getCallerPrincipalGroup(principals);
        if (callerGroup != null)
            principals.remove(callerGroup);
        // Remove any added Groups...
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return false;
    }
}
