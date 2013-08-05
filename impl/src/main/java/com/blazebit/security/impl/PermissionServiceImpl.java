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
package com.blazebit.security.impl;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionService;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Christian Beikov
 */
public class PermissionServiceImpl implements PermissionService{
    
    private final Map<Subject, Collection<Action>> permissions = new HashMap<Subject, Collection<Action>>();

    @Override
    public <R extends Role<R, ?>, P extends Permission<?>> boolean isGranted(Subject<R, P> subject, Action action) {
        if(subject == null) {
            throw new NullPointerException("subject");
        }
        if(action == null) {
            throw new NullPointerException("action");
        }
        
        Collection<Action> grantedActions = permissions.get(subject);
        
        if(grantedActions.contains(action)) {
            return true;
        }
        
        for (Permission permission : subject.getPermissions()) {
            if(permission.getAction().equals(action)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public <R extends Role<R, ?>, P extends Permission<?>> void grant(Subject<R, P> authorizer, Subject<R, P> subject, Action action) {
        if(subject == null) {
            throw new NullPointerException("subject");
        }
        if(action == null) {
            throw new NullPointerException("action");
        }
        if(authorizer == null) {
            throw new SecurityException("No valid authorizer given");
        }
        if (!isGranted(authorizer, getGrantAction())) {
            throw new SecurityException("The authorizer '" + authorizer + "' is not granted to grant actions");
        }
        
        Collection<Action> grantedActions = permissions.get(subject);
        
        if(grantedActions == null) {
            grantedActions = new HashSet<Action>();
            permissions.put(subject, grantedActions);
        }
        
        grantedActions.add(action);
    }

    @Override
    public <R extends Role<R, ?>, P extends Permission<?>> void revoke(Subject<R, P> authorizer, Subject<R, P> subject, Action action) {
        if(subject == null) {
            throw new NullPointerException("subject");
        }
        if(action == null) {
            throw new NullPointerException("action");
        }
        if(authorizer == null) {
            throw new SecurityException("No valid authorizer given");
        }
        if (!isGranted(authorizer, getRevokeAction())) {
            throw new SecurityException("The authorizer '" + authorizer + "' is not granted to revoke actions");
        }
        
        Collection<Action> grantedActions = permissions.get(subject);
        
        if(grantedActions == null) {
            grantedActions = new HashSet<Action>();
            permissions.put(subject, grantedActions);
        }
        
        grantedActions.remove(action);
    }

    @Override
    public <R extends Role<R, ?>, P extends Permission<?>> Collection<Action> getAllowedActions(Subject<R, P> subject) {
        if(subject == null) {
            throw new NullPointerException("subject");
        }
        
        Collection<Action> grantedActions = permissions.get(subject);
        Collection<Action> allowedActions;
        
        if(grantedActions == null) {
            allowedActions = new ArrayList<Action>(subject.getPermissions().size());
        } else {
            allowedActions = new ArrayList<Action>(subject.getPermissions().size() + grantedActions.size());
            allowedActions.addAll(grantedActions);
        }
        
        for(Permission permission : subject.getPermissions()) {
            allowedActions.add(permission.getAction());
        }
        
        return allowedActions;
    }
    
    @Override
    public Action getGrantAction() {
        return grantAction;
    }
    
    @Override
    public Action getRevokeAction() {
        return revokeAction;
    }
    
    @Override
    public Action getAllAction() {
        return allAction;
    }
    
    @Override
    public Action getNoneAction() {
        return noneAction;
    }
    
    @Override
    public Resource getAllResource() {
        return allResource;
    }
    
    private final Resource allResource = new Resource() {

        @Override
        public boolean matches(Resource resource) {
            return this == resource;
        }
    };
    
    private final Action grantAction = new Action() {

        @Override
        public boolean matches(Action action) {
            return this == action;
        }
    };
    
    private final Action revokeAction = new Action() {

        @Override
        public boolean matches(Action action) {
            return this == action;
        }
    };
    
    private final Action allAction = new Action() {

        @Override
        public boolean matches(Action action) {
            return this == action;
        }
    };
    
    private final Action noneAction = new Action() {

        @Override
        public boolean matches(Action action) {
            return this == action;
        }
    };
}
