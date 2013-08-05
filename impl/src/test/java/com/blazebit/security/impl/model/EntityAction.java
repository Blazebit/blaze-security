/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl.model;

import com.blazebit.security.Action;
import javax.persistence.Basic;
import javax.persistence.Embeddable;

/**
 *
 * @author Christian
 */
public class EntityAction implements Action {

    private PermissionId<?> permissionId;
    private String actionName;

    public <P extends PermissionId<?>> EntityAction(P permissionId) {
        this.permissionId = permissionId;
    }
    
    <P extends PermissionId<?>> void attachToPermissionId(P permissionId) {
        this.permissionId = permissionId;
        
        if(actionName != null) {
            permissionId.setActionName(actionName);
            actionName = null;
        }
    }
    
    @Override
    public boolean matches(Action action) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getActionName() {
        return permissionId == null ? actionName : permissionId.getActionName();
    }

    public void setActionName(String actionName) {
        if(permissionId == null) {
            this.actionName = actionName;
        } else {
            permissionId.setActionName(actionName);
        }
    }
    
}
