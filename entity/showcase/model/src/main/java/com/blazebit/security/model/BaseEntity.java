package com.blazebit.security.model;

import java.io.Serializable;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;


@MappedSuperclass
public abstract class BaseEntity<I extends Serializable> implements IdHolder<I>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected I id;
    
    public void setId(I id) {
        this.id = id;
    }

    @Override
    @Transient
    @SuppressWarnings("unchecked")
    public <T extends IdHolder<I>> Class<T> getRealClass() {
        Class<?> clazz = getClass();
        
        while (clazz.getName().lastIndexOf('$') > 0) {
            clazz = clazz.getSuperclass();
        }
        
        return (Class<T>) clazz;
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IdHolder<?>)) {
            return false;
        }
        IdHolder<?> other = (IdHolder<?>) obj;
        if (!other.getRealClass().equals(getRealClass())) {
            return false;
        }
        I thisId = getId();
        if (thisId == null) {
            if (other.getId() != null) {
                return false;
            }
        } else if (!thisId.equals(other.getId())) {
            return false;
        }
        return true;
    }

}
