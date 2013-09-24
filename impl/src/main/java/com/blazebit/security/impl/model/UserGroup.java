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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.Role;

/**
 * @author Christian Beikov
 */
@Entity
@ResourceName(name = "User group")
public class UserGroup implements Role<UserGroup>, Serializable, IdHolder {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String name;
    private UserGroup parent;
    private Set<User> users = new HashSet<User>(0);
    private Set<UserGroup> userGroups = new HashSet<UserGroup>(0);
    private Set<UserGroupPermission> permissions = new HashSet<UserGroupPermission>(0);
    private Set<UserGroupDataPermission> dataPermissions = new HashSet<UserGroupDataPermission>(0);

    public UserGroup() {
    }

    public UserGroup(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue
    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic(optional = false)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne
    @JoinColumn(name = "parent_group", nullable = true)
    public UserGroup getParent() {
        return this.parent;
    }

    public void setParent(UserGroup parent) {
        this.parent = parent;
    }

    @ManyToMany
    public Set<User> getUsers() {
        return this.users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    @OneToMany(mappedBy = "parent")
    public Set<UserGroup> getUserGroups() {
        return this.userGroups;
    }

    public void setUserGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups;
    }

    @OneToMany(mappedBy = "id.subject")
    public Set<UserGroupPermission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<UserGroupPermission> permissions) {
        this.permissions = permissions;
    }

    @OneToMany(mappedBy = "id.subject")
    public Set<UserGroupDataPermission> getDataPermissions() {
        return this.dataPermissions;
    }

    public void setDataPermissions(Set<UserGroupDataPermission> dataPermissions) {
        this.dataPermissions = dataPermissions;
    }

    @Transient
    @Override
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<Permission>();
        allPermissions.addAll(this.permissions);
        allPermissions.addAll(this.dataPermissions);
        return allPermissions;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
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
        final UserGroup other = (UserGroup) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UserGroup{" + "name=" + name + '}';
    }

}
