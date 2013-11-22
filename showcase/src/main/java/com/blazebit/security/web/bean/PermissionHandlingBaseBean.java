/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.blazebit.security.ActionFactory;
import com.blazebit.security.EntityResourceFactory;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionFactory;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.PermissionService;
import com.blazebit.security.ResourceFactory;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.utils.PermissionHandlingUtils;
import com.blazebit.security.metamodel.ResourceMetamodel;
import com.blazebit.security.web.service.api.ActionUtils;

/**
 * 
 * @author cuszk
 */
@ViewScoped
@ManagedBean(name = "permissionHandlingBaseBean")
@Named
public class PermissionHandlingBaseBean extends TreeHandlingBaseBean {

    @Inject
    protected PermissionFactory permissionFactory;
    @Inject
    protected UserSession userSession;
    @Inject
    protected PermissionDataAccess permissionDataAccess;
    @Inject
    protected PermissionManager permissionManager;
    @Inject
    protected EntityResourceFactory entityFieldFactory;
    @Inject
    protected ResourceFactory resourceFactory;
    @Inject
    protected ActionFactory actionFactory;
    @Inject
    protected PermissionService permissionService;
    @Inject
    protected ActionUtils actionUtils;
    @Inject
    protected ResourceMetamodel resourceMetamodel;
    @Inject
    protected PermissionHandlingUtils permissionHandlingUtils;

    private List<Permission> notGranted = new ArrayList<Permission>();
    private List<Permission> notRevoked = new ArrayList<Permission>();

    protected void performRevokeAndGrant(Subject subject, Collection<Permission> current, Set<Permission> revoked, Set<Permission> granted) {
        List<Set<Permission>> permissions = permissionHandlingUtils.getReconsideredRevokeAndGrant(current, revoked, granted);
        Set<Permission> finalRevoked = permissions.get(0);
        for (Permission permission : finalRevoked) {
            permissionService.revoke(userSession.getUser(), subject, permission.getAction(), permission.getResource());
        }
        Set<Permission> finalGranted = permissions.get(1);
        for (Permission permission : finalGranted) {
            permissionService.grant(userSession.getUser(), subject, permission.getAction(), permission.getResource());
        }

    }

    protected void performRevokeAndGrant(Role role, Collection<Permission> current, Set<Permission> revoked, Set<Permission> granted) {
        List<Set<Permission>> permissions = permissionHandlingUtils.getReconsideredRevokeAndGrant(current, revoked, granted);
        Set<Permission> finalRevoked = permissions.get(0);
        for (Permission permission : finalRevoked) {
            permissionService.revoke(userSession.getUser(), role, permission.getAction(), permission.getResource());
        }
        Set<Permission> finalGranted = permissions.get(1);
        for (Permission permission : finalGranted) {
            permissionService.grant(userSession.getUser(), role, permission.getAction(), permission.getResource());
        }

    }

    public List<Permission> getNotGranted() {
        return this.notGranted;
    }

    public void setNotGranted(Set<Permission> notGranted) {
        List<Permission> ret = new ArrayList<Permission>(notGranted);
        Collections.sort(ret, new Comparator<Permission>() {

            @Override
            public int compare(Permission o1, Permission o2) {
                return ((EntityField) o1.getResource()).getEntity().compareToIgnoreCase(((EntityField) o1.getResource()).getEntity());
            }

        });
        this.notGranted = ret;

    }

    public List<Permission> getNotRevoked() {
        return notRevoked;
    }

    public void setNotRevoked(Set<Permission> notRevoked) {
        List<Permission> ret = new ArrayList<Permission>(notRevoked);
        Collections.sort(ret, new Comparator<Permission>() {

            @Override
            public int compare(Permission o1, Permission o2) {
                return ((EntityField) o1.getResource()).getEntity().compareToIgnoreCase(((EntityField) o1.getResource()).getEntity());
            }

        });
        this.notRevoked = ret;
    }

}
