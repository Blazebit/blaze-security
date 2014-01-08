package com.blazebit.security.web.bean.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import com.blazebit.security.Permission;
import com.blazebit.security.impl.model.EntityField;
import com.blazebit.security.impl.model.EntityObjectField;

@Named
@SessionScoped
public class DialogBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private List<Permission> notGranted = new ArrayList<Permission>();
    private List<Permission> notRevoked = new ArrayList<Permission>();

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

    public String displayPermission(Permission permission) {
        if (permission.getResource() instanceof EntityObjectField) {
            EntityObjectField resource = (EntityObjectField) permission.getResource();
            return resource.getEntity() + " " + resource.getEntityId() + (!resource.isEmptyField() ? "-" + resource.getField() : "");
        } else {
            EntityField resource = (EntityField) permission.getResource();
            return resource.getEntity() + (!resource.isEmptyField() ? "-" + resource.getField() : "");
        }
    }
}
