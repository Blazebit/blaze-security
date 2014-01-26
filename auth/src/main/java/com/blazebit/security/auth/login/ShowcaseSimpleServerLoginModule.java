package com.blazebit.security.auth.login;

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
import javax.xml.registry.infomodel.User;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import com.blazebit.security.PermissionManager;
import com.blazebit.security.Subject;
/**
 * inspired from Jboss's org.jboss.security.auth.spi.SimpleServerLoginModule
 * 
 * AD:
 * http://stackoverflow.com/questions/8551809/how-to-connect-with-java-into-active-directory
 * 
 * @author cuszk
 *
 */
public class ShowcaseSimpleServerLoginModule implements LoginModule {

    private Subject user;
    private boolean loginOk;

    private javax.security.auth.Subject subject;
    private Map<String, Object> sharedState;
    private CallbackHandler callbackHandler;
    // read options
    private boolean useFirstPass;

    private char[] credential;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(javax.security.auth.Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.sharedState = (Map<String, Object>) sharedState;
        this.callbackHandler = callbackHandler;
        String passwordStacking = (String) options.get("password-stacking");
        if (passwordStacking != null) {
            this.useFirstPass = passwordStacking.equals("useFirstPass");
        }

    }

    @Override
    public boolean login() throws LoginException {
        loginOk = false;

        // If useFirstPass is true, look for the shared password
        if (useFirstPass == true) {
            // Setup our view of the user
            Object sharedUsername = sharedState.get("javax.security.auth.login.name");
            Object sharedPassword = sharedState.get("javax.security.auth.login.password");
            if (sharedUsername != null && sharedPassword != null) {
                if (sharedUsername instanceof Principal) {
                    String name = ((Principal) sharedUsername).getName();
                    if (name != null) {
                        user = (Subject) createIdentity(name);
                        if (user == null) {
                            throw new LoginException("Unable to create identity from given principal");
                        }
                    } else {
                        throw new LoginException("Unable to create identity from given principal");
                    }
                    if (sharedPassword instanceof char[])
                        credential = (char[]) sharedPassword;
                    else if (sharedPassword != null) {
                        String tmp = sharedPassword.toString();
                        credential = tmp.toCharArray();
                        loginOk = true;
                        return true;
                    }
                }
            } else {
                // no shared state
                String[] info = getUsernameAndPassword();
                final String username = info[0];
                String password = info[1];

                // validate the retrieved username and password.
                if (validateUsernameAndPassword(username, password)) {
                    user = (Subject) createIdentity(username);
                    if (user != null) {

                        if (useFirstPass == true) { // Add the principal and password to the shared state map
                            sharedState.put("javax.security.auth.login.name", new Principal() {

                                @Override
                                public String getName() {
                                    return username;
                                }
                            });

                            sharedState.put("javax.security.auth.login.password", password);
                        }
                        loginOk = true;
                        return true;
                    }

                }
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

    protected Principal createIdentity(String userId) {
//        BeanManagerProvider.getInstance().getBeanManager().fireEvent(event);
        User user = userDataAccess.findUser(Integer.valueOf(userId));
        return user;
    }

    @Override
    public boolean commit() throws LoginException {
        if (loginOk == false)
            return false;
        subject.getPrincipals().clear();
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
        // reset shared state
        if (useFirstPass) {
            sharedState.put("javax.security.auth.login.name", null);
            sharedState.put("javax.security.auth.login.password", null);
        }
        // Group[] groups = this.getRoleSets();
        subject.getPrincipals().clear();
        subject.getPrincipals().clear();
        subject.getPublicCredentials().clear();
        subject.getPrivateCredentials().clear();
        //subject = new javax.security.auth.Subject();
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return false;
    }
}
