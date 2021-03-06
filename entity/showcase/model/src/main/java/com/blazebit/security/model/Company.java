package com.blazebit.security.model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.blazebit.security.entity.EntityResourceType;

@Entity
@EntityResourceType(name = "Company", module = "Core")
public class Company extends BaseEntity<Integer> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String name;
    private boolean fieldLevelEnabled = true;
    private boolean objectLevelEnabled = true;
    private boolean userLevelEnabled = true;
    private boolean groupHierarchyEnabled = true;
    private boolean actAsUser = true;

    public Company(String name) {
        this.name = name;
    }

    public Company() {
    }

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGroupHierarchyEnabled() {
        return groupHierarchyEnabled;
    }

    public void setGroupHierarchyEnabled(boolean groupHierarchyEnabled) {
        this.groupHierarchyEnabled = groupHierarchyEnabled;
    }

    public boolean isFieldLevelEnabled() {
        return fieldLevelEnabled;
    }

    public void setFieldLevelEnabled(boolean fieldLevelEnabled) {
        this.fieldLevelEnabled = fieldLevelEnabled;
    }

    public boolean isObjectLevelEnabled() {
        return objectLevelEnabled;
    }

    public void setObjectLevelEnabled(boolean objectLevelEnabled) {
        this.objectLevelEnabled = objectLevelEnabled;
    }

    public boolean isUserLevelEnabled() {
        return userLevelEnabled;
    }

    public void setUserLevelEnabled(boolean userLevelEnabled) {
        this.userLevelEnabled = userLevelEnabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Company other = (Company) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public boolean isActAsUser() {
        return actAsUser;
    }

    public void setActAsUser(boolean actAsUser) {
        this.actAsUser = actAsUser;
    }
    
    

}
