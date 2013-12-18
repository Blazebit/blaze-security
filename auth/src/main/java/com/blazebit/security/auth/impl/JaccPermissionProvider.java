package com.blazebit.security.auth.impl;

import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebRoleRefPermission;
import javax.servlet.http.HttpServletRequest;

import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.constants.ActionConstants;
import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.service.resource.UserDataAccess;

@Stateless
public class JaccPermissionProvider {

    @Inject
    private EntityResourceFactory entityResourceFactory;

    @Inject
    private ActionFactory actionFactory;

    @Inject
    private PermissionFactory permissionFactory;

    @Inject
    private UserDataAccess userDataAccess;

    /**
     * checks the roles of the authenticated subject and transforms them into the system's permissions
     * 
     * @return
     * @throws PolicyContextException
     */
    public List<Permission> getUserRoles() throws PolicyContextException {
        String SUBJECT_HANDLER_KEY = "javax.security.auth.Subject.container";
        javax.security.auth.Subject subject = (javax.security.auth.Subject) PolicyContext.getContext(SUBJECT_HANDLER_KEY);
        List<Permission> permissions = new ArrayList<Permission>();
        Iterator<Principal> principals = subject.getPrincipals().iterator();
        User user = null;
        while (principals.hasNext()) {
            Principal next = principals.next();
            if (next instanceof SimpleGroup) {
                if ("Roles".equals(next.getName())) {
                    Enumeration<Principal> groups = ((SimpleGroup) next).members();
                    while (groups.hasMoreElements()) {
                        String resourceName = groups.nextElement().getName();
                        for (ActionConstants actionConstant : ActionConstants.values()) {
                            permissions.add(permissionFactory.create(user, actionFactory.createAction(actionConstant), entityResourceFactory.createResource(resourceName)));
                        }
                    }
                }
            } else {
                if (next instanceof SimplePrincipal) {
                    user = userDataAccess.findUser(Integer.valueOf(next.getName()));
                }
            }
        }
        return permissions;
    }

    /**
     * transforms the authenticated subject's permissions into the system's permissions
     * 
     * @return list of permissions
     */
    public List<Permission> getUserPermissions() {
        List<Permission> permissions = new ArrayList<Permission>();
        try {
            for (String resourceName : getRoleSet()) {
                for (ActionConstants actionConstant : ActionConstants.values()) {
                    permissions.add(permissionFactory.create(actionFactory.createAction(actionConstant), entityResourceFactory.createResource(resourceName)));
                }
            }
        } catch (PolicyContextException e) {
        }
        return permissions;
    }

    /**
     * looks at the security constraints and returns a list with the authenticated subject's granted permissions
     * 
     * @return
     * @throws PolicyContextException
     */
    private Set<String> getRoleSet() throws PolicyContextException {
        String SUBJECT_HANDLER_KEY = "javax.security.auth.Subject.container";
        javax.security.auth.Subject subject = (javax.security.auth.Subject) PolicyContext.getContext(SUBJECT_HANDLER_KEY);

        Policy policy = Policy.getPolicy();

        CodeSource cs = new CodeSource(null, (java.security.cert.Certificate[]) null);
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, subject.getPrincipals().toArray(new Principal[subject.getPrincipals().size()]));

        PermissionCollection pc = policy.getPermissions(pd);
        Enumeration permissions = pc.elements();
        Set<String> roleSet = new HashSet<String>();
        HttpServletRequest request = (HttpServletRequest) PolicyContext.getContext(HttpServletRequest.class.getName());
        while (permissions.hasMoreElements()) {

            java.security.Permission permission = (java.security.Permission) permissions.nextElement();

            if (permission instanceof WebRoleRefPermission) {
                String roleRef = permission.getActions();
                // confirm roleRef via isUserInRole to ensure proper scoping to Servlet Name
                if (!roleSet.contains(roleRef) && request.isUserInRole(roleRef)) {
                    roleSet.add(permission.getActions());
                }
            }
        }
        return roleSet;
    }

}
