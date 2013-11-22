package com.blazebit.security.metamodel;

import java.util.List;
import java.util.Map;

import com.blazebit.security.spi.EntityResource;
import com.blazebit.security.spi.ResourceDefinition;

public interface ResourceMetamodel {

    public Map<ResourceDefinition, EntityResource> getResourceDefinitions();

    public Map<EntityResource, List<ResourceDefinition>> getEntityResources();

    /**
     * resource names groupped by module
     * 
     * @return
     */
    public Map<String, List<String>> getResourcesByModule();

    /**
     * real class name for resourcename
     * 
     * @param resourceName
     * @return
     */
    public String getEntityClassNameByResourceName(String resourceName);

    /**
     * primitive field names of resource name's class
     * 
     * @param resourceName
     * @return list of field names
     * @throws ClassNotFoundException
     */
    public List<String> getPrimitiveFields(String resourceName) throws ClassNotFoundException;

    /**
     * collection field name of resource name's class
     * 
     * @param resourceName
     * @return list of field names
     * @throws ClassNotFoundException
     */
    public List<String> getCollectionFields(String resourceName) throws ClassNotFoundException;

    /**
     * 
     * @param resourceName
     * @return
     * @throws ClassNotFoundException
     */
    public List<String> getFields(String resourceName) throws ClassNotFoundException;

    /**
     * 
     * @param entityClass
     * @return
     */
    public List<String> getPrimitiveFields(Class<?> entityClass);

    /**
     * 
     * @param entityClass
     * @return
     */
    public List<String> getCollectionFields(Class<?> entityClass);

    /**
     * 
     * @param entityClass
     * @return
     */
    public List<String> getFields(Class<?> entityClass);
}
