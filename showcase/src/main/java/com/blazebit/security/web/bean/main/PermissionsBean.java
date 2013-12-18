/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main;

import java.io.Serializable;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebRoleRefPermission;
import javax.servlet.http.HttpServletRequest;

import com.blazebit.security.Permission;
import com.blazebit.security.RolePermission;
import com.blazebit.security.SubjectPermission;
import com.blazebit.security.web.bean.model.PermissionModel;

/**
 * 
 * @author cuszk
 */
@ManagedBean(name = "permissionsBean")
@ViewScoped
public class PermissionsBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @PersistenceContext(unitName = "SecurityPU")
    EntityManager entityManager;

    private List<PermissionModel> permissions = new ArrayList<PermissionModel>();

    @SuppressWarnings("unchecked")
    public void initPermissions() {
        permissions.clear();
        List<Permission> ret = entityManager.createQuery("select p from " + SubjectPermission.class.getName()
                                                             + " p order by p.id.subject.company.id, p.id.subject.username, p.id.entity, p.id.field").getResultList();
        for (Permission p : ret) {
            permissions.add(new PermissionModel(p));
        }
        ret = entityManager
            .createQuery("select p from " + RolePermission.class.getName() + " p order by p.id.subject.company.id,  p.id.subject.name, p.id.entity, p.id.field")
            .getResultList();
        for (Permission p : ret) {
            permissions.add(new PermissionModel(p));
        }
    }

    public String testJacc() throws PolicyContextException {
        StringBuilder ret = new StringBuilder();
        // test JACC
        String SUBJECT_HANDLER_KEY = "javax.security.auth.Subject.container";
        javax.security.auth.Subject subject = (javax.security.auth.Subject) PolicyContext.getContext(SUBJECT_HANDLER_KEY);

        Policy policy = Policy.getPolicy();

        CodeSource cs = new CodeSource(null, (java.security.cert.Certificate[]) null);
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, subject.getPrincipals().toArray(new Principal[subject.getPrincipals().size()]));

        PermissionCollection pc = policy.getPermissions(pd);
        Enumeration permissions = pc.elements();
        while (permissions.hasMoreElements()) {

            java.security.Permission permission = (java.security.Permission) permissions.nextElement();

            ret.append(permission.getClass()).append("-").append("Name :").append(permission.getName()).append(". Actions: ").append(permission.getActions()).append("<br/>");
        }
        HashSet roles = getRoleSet(pc);
        for (Object role : roles) {
            ret.append(role).append("<br/>");
        }
        pc.implies(new WebRoleRefPermission("a", "b"));
        return ret.toString();
    }

    private HashSet getRoleSet(PermissionCollection pc) throws PolicyContextException {
        Enumeration permissions = pc.elements();
        HashSet roleSet = null;
        HttpServletRequest request = (HttpServletRequest) PolicyContext.getContext(HttpServletRequest.class.getName());
        while (permissions.hasMoreElements()) {

            java.security.Permission permission = (java.security.Permission) permissions.nextElement();

            if (permission instanceof WebRoleRefPermission) {
                String roleRef = permission.getActions();
                if (roleSet == null) {
                    roleSet = new HashSet();
                }
                // confirm roleRef via isUserInRole to ensure proper scoping to Servlet Name
                if (!roleSet.contains(roleRef) && request.isUserInRole(roleRef)) {
                    roleSet.add(permission.getActions());
                }
            }
        }
        return roleSet;
    }

    public List<PermissionModel> getPermissions() {
        return permissions;
    }
}
