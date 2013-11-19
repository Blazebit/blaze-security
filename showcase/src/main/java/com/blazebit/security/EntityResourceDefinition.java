package com.blazebit.security;

public class EntityResourceDefinition {

    public EntityResourceDefinition() {
    }

    public EntityResourceDefinition(EntityResource resource, String resourceName) {
        this.resource = resource;
        this.resourceName = resourceName;
    }

    private EntityResource resource;
    private String resourceName;

    
    public EntityResource getResource() {
        return resource;
    }

    
    public void setResource(EntityResource resource) {
        this.resource = resource;
    }

    
    public String getResourceName() {
        return resourceName;
    }

    
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String toString() {
        return "EntityResourceDefinition [resource=" + resource + ", resourceName=" + resourceName + "]";
    }

}