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

import com.blazebit.security.Permission;
import com.blazebit.security.Subject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * @author Christian Beikov
 */
@Entity
@Table(name = "USER_", schema = "USERROLES")
public class User implements Subject<UserGroup, UserPermission, UserDataPermission>, Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String username;
    private String password;
    private Set<UserGroup> userGroups = new HashSet<UserGroup>(0);
    private Set<UserPermission> permissions = new HashSet<UserPermission>(0);
    private Set<UserDataPermission> dataPermissions = new HashSet<UserDataPermission>(0);

    public User() {
    }

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User(String username) {
        this.username = username;
    }

    @Basic
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Basic
    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    @Transient
    public Set<UserGroup> getRoles() {
        return this.userGroups;
    }

    @ManyToMany
    @JoinTable(name = "USER_IN_ROLES")
    public Set<UserGroup> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups;
    }

    @OneToMany(mappedBy = "id.subject")
    @Override
    public Set<UserPermission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<UserPermission> permissions) {
        this.permissions = permissions;
    }

//    @Override
    @OneToMany(mappedBy = "id.subject")
    public Set<UserDataPermission> getDataPermissions() {
        return this.dataPermissions;
    }

    public void setDataPermissions(Set<UserDataPermission> dataPermissions) {
        this.dataPermissions = dataPermissions;
    }

    @Override
    public String toString() {
        return "User{" + "username=" + username + '}';
    }
}
