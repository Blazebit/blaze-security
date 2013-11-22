package com.blazebit.security.impl.spi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.blazebit.reflection.ReflectionUtils;
import com.blazebit.security.metamodel.ResourceMetamodel;
import com.blazebit.security.spi.EntityResource;
import com.blazebit.security.spi.ResourceDefinition;

public class ResourceMetamodelProducer {

    @Inject
    ResourceNameExtension extension;

    @Produces
    @Default
    @ApplicationScoped
    public ResourceMetamodel produce() {
        return new ResourceMetamodel() {

            @Override
            public Map<ResourceDefinition, EntityResource> getResourceDefinitions() {
                return extension.getResourceDefinitions();
            }

            @Override
            public Map<EntityResource, List<ResourceDefinition>> getEntityResources() {
                return extension.getEntityResources();
            }

            @Override
            public Map<String, List<String>> getResourcesByModule() {
                Map<String, List<String>> ret = new TreeMap<String, List<String>>();
                for (EntityResource entityClassName : getEntityResources().keySet()) {
                    for (ResourceDefinition resourceDefinition : getEntityResources().get(entityClassName)) {

                        List<String> resourceNames = new ArrayList<String>();
                        if (!StringUtils.isEmpty(resourceDefinition.getModuleName())) {
                            if (ret.containsKey(resourceDefinition.getModuleName())) {
                                resourceNames = ret.get(resourceDefinition.getModuleName());
                            } else {
                                resourceNames = new ArrayList<String>();
                            }
                            resourceNames.add(resourceDefinition.getResourceName());
                            ret.put(resourceDefinition.getModuleName(), resourceNames);
                        }
                    }
                }
                return ret;
            }

            @Override
            public String getEntityClassNameByResourceName(String resourceName) {
                for (ResourceDefinition resourceDefinition : getResourceDefinitions().keySet()) {
                    if (resourceDefinition.getResourceName().equals(resourceName)) {
                        return getResourceDefinitions().get(resourceDefinition).getEntityClassName();
                    }
                }
                return null;
            }

            @Override
            public List<String> getFields(String resourceName) throws ClassNotFoundException {
                List<String> ret = new ArrayList<String>();
                ret.addAll(getPrimitiveFields(resourceName));
                ret.addAll(getCollectionFields(resourceName));
                return ret;
            }
            
            @Override
            public List<String> getFields(Class<?> entityClass)  {
                List<String> ret = new ArrayList<String>();
                ret.addAll(getPrimitiveFields(entityClass));
                ret.addAll(getCollectionFields(entityClass));
                return ret;
            }

            @Override
            public List<String> getPrimitiveFields(String resourceName) throws ClassNotFoundException {
                Class<?> entityClass = Class.forName(getEntityClassNameByResourceName(resourceName));
                return getPrimitiveFields(entityClass);
            }

            @Override
            public List<String> getPrimitiveFields(Class<?> entityClass) {
                List<String> ret = new ArrayList<String>();
                List<Field> fields = filterFields(entityClass).get(0);
                for (Field field : fields) {
                    ret.add(field.getName());
                }
                return ret;
            }

            @Override
            public List<String> getCollectionFields(String resourceName) throws ClassNotFoundException {
                Class<?> entityClass = Class.forName(getEntityClassNameByResourceName(resourceName));
                return getCollectionFields(entityClass);
            }

            @Override
            public List<String> getCollectionFields(Class<?> entityClass) {
                List<String> ret = new ArrayList<String>();
                List<Field> fields = filterFields(entityClass).get(1);
                for (Field field : fields) {
                    ret.add(field.getName());
                }
                return ret;
            }

            private List<List<Field>> filterFields(Class<?> entityClass) {
                List<List<Field>> ret = new ArrayList<List<Field>>();
                ret.add(new ArrayList<Field>());
                ret.add(new ArrayList<Field>());
                Field[] all = ReflectionUtils.getInstanceFields(entityClass);
                for (Field field : all) {
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        ret.get(1).add(field);
                    } else {
                        ret.get(0).add(field);
                    }
                }
                return ret;
            }

        };
    }
}
