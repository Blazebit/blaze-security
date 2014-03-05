/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.blazebit.security.model.Permission;
import com.blazebit.security.model.RolePermission;
import com.blazebit.security.model.SubjectPermission;
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

    public List<PermissionModel> getPermissions() {
        return permissions;
    }
}
