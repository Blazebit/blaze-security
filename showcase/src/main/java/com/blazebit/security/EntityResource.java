package com.blazebit.security;

public class EntityResource {

    private String entityClassName;

    public EntityResource() {
    }

    public EntityResource(String entityClassName) {
        super();
        this.entityClassName = entityClassName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityClassName == null) ? 0 : entityClassName.hashCode());
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
        EntityResource other = (EntityResource) obj;
        if (entityClassName == null) {
            if (other.entityClassName != null)
                return false;
        } else if (!entityClassName.equals(other.entityClassName))
            return false;
        return true;
    }
    
    

}