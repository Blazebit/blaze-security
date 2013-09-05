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

import com.blazebit.security.IdHolder;
import com.blazebit.security.Permission;
import com.blazebit.security.Subject;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import org.hibernate.annotations.Cascade;

/**
 * @author Christian Beikov
 */
@Entity
public class User implements Subject<UserGroup>, Serializable, IdHolder {

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

    @Basic(optional = false)
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

    @ManyToMany(targetEntity = UserGroup.class)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @JoinTable(name = "User_Role", joinColumns = {
        @JoinColumn(
                name = "users_id",
                nullable = false)},
            inverseJoinColumns = {
        @JoinColumn(
                name = "usergroups_id",
                nullable = false)})
    public Set<UserGroup> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups;
    }

    @OneToMany(mappedBy = "id.subject")
    public Set<UserPermission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<UserPermission> permissions) {
        this.permissions = permissions;
    }

    @OneToMany(mappedBy = "id.subject")
    public Set<UserDataPermission> getDataPermissions() {
        return this.dataPermissions;
    }

    @Transient
    @Override
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<Permission>();
        allPermissions.addAll(this.permissions);
        allPermissions.addAll(this.dataPermissions);
        return allPermissions;
    }

    public void setDataPermissions(Set<UserDataPermission> dataPermissions) {
        this.dataPermissions = dataPermissions;
    }

    @Override
    public String toString() {
        return "User{" + "username=" + username + '}';
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
        final User other = (User) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    @Transient
    public String getEntityId() {
        //TODO add json format
        return String.valueOf(id);
    }
}
