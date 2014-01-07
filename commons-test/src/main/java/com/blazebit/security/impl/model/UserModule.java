/*
 * Copyright 2013 Blazebit.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.blazebit.security.impl.model;

import java.security.Principal;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.blazebit.security.Module;
import com.blazebit.security.Subject;

/**
 * @author Christian Beikov
 */
public class UserModule implements Module {

    private String name;

    public UserModule() {
    }

    public UserModule(String name) {
        this.name = name;
    }

    private Set<Principal> members = new HashSet<Principal>();

    public Set<Principal> getMembers() {
        return members;
    }

    @Override
    public boolean addMember(Principal user) {
        return getMembers().add((Principal) user);
    }

    @Override
    public boolean removeMember(Principal user) {
        return getMembers().remove((Subject) user);
    }

    @Override
    public boolean isMember(Principal member) {
        return getMembers().contains((Subject) member);
    }

    @Override
    public Enumeration<? extends Principal> members() {
        return new Vector<Principal>(getMembers()).elements();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "UserModule [name=" + name + ", members=" + members + "]";
    }
    
    

}
