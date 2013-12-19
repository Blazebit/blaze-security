package com.blazebit.security.auth.provider;

import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebRoleRefPermission;
import javax.servlet.http.HttpServletRequest;

import com.blazebit.security.impl.model.User;
import com.blazebit.security.impl.model.UserModule;
import com.blazebit.security.impl.service.resource.UserDataAccess;

@Stateless
public class JaccPermissionProvider {

    @Inject
    private UserDataAccess userDataAccess;

    /**
     * looks at the security constraints and returns a list with the authenticated subject's granted permissions
     * 
     * @return
     * @throws PolicyContextException
     */
    public Set<String> getRequiredRoles() throws PolicyContextException {
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