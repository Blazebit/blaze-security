/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.impl.model.EntityField;

/**
 *
 * @author cuszk
 */
public class ResourceAction {

    private EntityField resource;
    private EntityAction action;

    public ResourceAction(EntityField resource, EntityAction action) {
        this.resource = resource;
        this.action = action;
    }

    public EntityField getResource() {
        return resource;
    }

    public void setResource(EntityField resource) {
        this.resource = resource;
    }

    public EntityAction getAction() {
        return action;
    }

    public void setAction(EntityAction action) {
        this.action = action;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceAction other = (ResourceAction) obj;
        if (this.resource != other.resource && (this.resource == null || !this.resource.equals(other.resource))) {
            return false;
        }
        if (this.action != other.action && (this.action == null || !this.action.equals(other.action))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ResourceAction{" + "resource=" + resource + ", action=" + action + '}';
    }
    
    
    
    
}
