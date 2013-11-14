/**
 * 
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.apache.commons.lang3.StringUtils;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.Action;
import com.blazebit.security.ActionImplicationProvider;
import com.blazebit.security.ResourceExtensionCallback;
import com.blazebit.security.impl.model.ResourceName;

/**
 * @author Thomas Herzog <t.herzog@curecomp.com>
 * @company curecomp
 * @date 12.08.2013
 */
@ServiceProvider(Extension.class)
public class ResourceNameExtension implements Extension {

    public static class EntityResource {

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

        @Override
        public String toString() {
            return "EntityResource [entityClassName=" + entityClassName + "]";
        }

    }

    public static class ResourceDefinition {

        public ResourceDefinition() {
        }

        public ResourceDefinition(String moduleName, String resourceName) {
            this.moduleName = moduleName;
            this.resourceName = resourceName;
        }

        String moduleName;
        String resourceName;
        String testExpression;

        @Override
        public String toString() {
            return "ResourceDefinition [moduleName=" + moduleName + ", resourceName=" + resourceName + "]";
        }

    }

    public static class EntityResourceDefinition {

        public EntityResourceDefinition() {

        }

        public EntityResourceDefinition(EntityResource resource, String resourceName) {
            this.resource = resource;
            this.resourceName = resourceName;
        }

        EntityResource resource;
        String resourceName;

        @Override
        public String toString() {
            return "EntityResourceDefinition [resource=" + resource + ", resourceName=" + resourceName + "]";
        }

    }

    private Map<EntityResource, List<ResourceDefinition>> resources = new HashMap<EntityResource, List<ResourceDefinition>>();
    private Map<ResourceDefinition, EntityResource> entities = new HashMap<ResourceDefinition, EntityResource>();
    private Map<Action, List<Action>> actions = new HashMap<Action, List<Action>>();
    private final Collection<Class<?>> resourceClasses = new HashSet<Class<?>>();

    @SuppressWarnings("rawtypes")
    protected void detectInterfaces(@Observes ProcessAnnotatedType processAnnotatedType) {
        AnnotatedType<?> type = processAnnotatedType.getAnnotatedType();
        if (type.isAnnotationPresent(ResourceName.class)) {
            ResourceName annotation = type.getAnnotation(ResourceName.class);
            // TODO find out why doesnt this work anymore!! ResourceName annotation = (ResourceName)
            // AnnotationUtils.findAnnotation(type.getClass(), ResourceName.class);
            Class<?> entityClass = (Class<?>) type.getBaseType();
            resourceClasses.add(entityClass);
            EntityResource entityResource = new EntityResource(entityClass.getName());
            List<ResourceDefinition> resourceDefinitions = new ArrayList<ResourceDefinition>();
            if (resources.containsKey(entityResource)) {
                resourceDefinitions = resources.get(entityResource);
            } else {
                resourceDefinitions = new ArrayList<ResourceDefinition>();
            }
            ResourceDefinition resourceDefinition = new ResourceDefinition(annotation.module(), annotation.name());
            resourceDefinitions.add(resourceDefinition);
            entities.put(resourceDefinition, entityResource);
            resources.put(entityResource, resourceDefinitions);
        }
    }

    protected void cleanup(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        // resourceNames.clear();
        Iterator<ResourceExtensionCallback> iter = ServiceLoader.load(ResourceExtensionCallback.class).iterator();
        while (iter.hasNext()) {
            ResourceExtensionCallback callback = iter.next();
            Map<EntityResource, List<ResourceDefinition>> result = callback.handle(resourceClasses);
            // add to entity resources
            for (EntityResource entityResource : result.keySet()) {
                List<ResourceDefinition> resourceDefinitions = resources.get(entityResource);
                resourceDefinitions.addAll(result.get(entityResource));
                resources.put(entityResource, resourceDefinitions);
            }
        }
        Iterator<ActionImplicationProvider> actionImplicationIterator = ServiceLoader.load(ActionImplicationProvider.class).iterator();
        while (actionImplicationIterator.hasNext()) {
            ActionImplicationProvider implication = actionImplicationIterator.next();
            actions.putAll(implication.getActionImplications());
        }
    }

    public Map<String, List<EntityResourceDefinition>> getResourcesByModule() {
        Map<String, List<EntityResourceDefinition>> ret = new TreeMap<String, List<EntityResourceDefinition>>();
        for (EntityResource entityClassName : resources.keySet()) {
            for (ResourceDefinition resourceDefinition : resources.get(entityClassName)) {
                List<EntityResourceDefinition> resourceNames = new ArrayList<EntityResourceDefinition>();
                if (!StringUtils.isEmpty(resourceDefinition.moduleName)) {
                    if (ret.containsKey(resourceDefinition.moduleName)) {
                        resourceNames = ret.get(resourceDefinition.moduleName);
                    } else {
                        resourceNames = new ArrayList<EntityResourceDefinition>();
                    }
                    resourceNames.add(new EntityResourceDefinition(entityClassName, resourceDefinition.resourceName));
                    ret.put(resourceDefinition.moduleName, resourceNames);
                }
            }
        }
        for (String moduleName : ret.keySet()) {
            // sort resource name inside modules
            Collections.sort(ret.get(moduleName), new Comparator<EntityResourceDefinition>() {

                @Override
                public int compare(EntityResourceDefinition o1, EntityResourceDefinition o2) {
                    return o1.resourceName.compareToIgnoreCase(o2.resourceName);
                }

            });
            ret.put(moduleName, ret.get(moduleName));
        }
        return ret;
    }

    public EntityResource getEntityResourceByResourceName(String resourceName) {
        for (ResourceDefinition resourceDefinition : entities.keySet()) {
            if (resourceDefinition.resourceName.equals(resourceName)) {
                return entities.get(resourceDefinition);
            }
        }
        return null;
    }

    public List<Action> getImpliedActions(Action action) {
        if (actions.containsKey(action)) {
            return actions.get(action);
        } else {
            return new ArrayList<Action>();
        }
    }
}
