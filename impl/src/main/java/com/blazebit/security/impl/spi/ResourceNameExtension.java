/**
 * 
 */
package com.blazebit.security.impl.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.impl.model.ResourceName;
import com.blazebit.security.spi.EntityResource;
import com.blazebit.security.spi.ResourceDefinition;
import com.blazebit.security.spi.ResourceExtensionCallback;

/**
 * 
 * @author cuszk
 * 
 */
@ServiceProvider(Extension.class)
public class ResourceNameExtension implements Extension {

    private final Map<EntityResource, List<ResourceDefinition>> entityResources = new HashMap<EntityResource, List<ResourceDefinition>>();
    private final Map<ResourceDefinition, EntityResource> resourceDefinitions = new HashMap<ResourceDefinition, EntityResource>();
    private final Collection<Class<?>> resourceClasses = new HashSet<Class<?>>();

    @SuppressWarnings("rawtypes")
    protected void detectInterfaces(@Observes ProcessAnnotatedType processAnnotatedType) {
        AnnotatedType<?> type = processAnnotatedType.getAnnotatedType();
        if (type.isAnnotationPresent(ResourceName.class)) {
            ResourceName annotation = type.getAnnotation(ResourceName.class);
            // TODO find out why doesnt this work anymore!! ResourceName annotation = (ResourceName)
            // AnnotationUtils.findAnnotation(type.getClass(), ResourceName.class);
            if (!annotation.skip()) {
                Class<?> entityClass = (Class<?>) type.getBaseType();
                resourceClasses.add(entityClass);
                EntityResource entityResource = new EntityResource(entityClass.getName());
                List<ResourceDefinition> resources = new ArrayList<ResourceDefinition>();
                if (entityResources.containsKey(entityResource)) {
                    resources = entityResources.get(entityResource);
                } else {
                    resources = new ArrayList<ResourceDefinition>();
                }
                ResourceDefinition resourceDefinition = new ResourceDefinition(annotation.module(), annotation.name(), annotation.test());
                resources.add(resourceDefinition);
                this.resourceDefinitions.put(resourceDefinition, entityResource);
                entityResources.put(entityResource, resources);
            }
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
                List<ResourceDefinition> resources = entityResources.get(entityResource);
                resources.addAll(result.get(entityResource));
                entityResources.put(entityResource, resources);
                for (ResourceDefinition rd : resources) {
                    resourceDefinitions.put(rd, entityResource);
                }
            }
        }
        // Iterator<ActionImplicationProvider> actionImplicationIterator =
        // ServiceLoader.load(ActionImplicationProvider.class).iterator();
        // while (actionImplicationIterator.hasNext()) {
        // ActionImplicationProvider implication = actionImplicationIterator.next();
        // actions.putAll(implication.getActionImplications());
        // }
    }

    public Map<EntityResource, List<ResourceDefinition>> getEntityResources() {
        return entityResources;
    }

    public Map<ResourceDefinition, EntityResource> getResourceDefinitions() {
        return resourceDefinitions;
    }

}
