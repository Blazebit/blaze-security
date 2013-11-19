/**
 * 
 */
package com.blazebit.security;

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
import com.blazebit.security.impl.model.ResourceName;

/**
 * @author Thomas Herzog <t.herzog@curecomp.com>
 * @company curecomp
 * @date 12.08.2013
 */
@ServiceProvider(Extension.class)
public class ResourceNameExtension implements Extension {

    private Map<EntityResource, List<ResourceDefinition>> resources = new HashMap<EntityResource, List<ResourceDefinition>>();

    public Map<EntityResource, List<ResourceDefinition>> getResources() {
        return resources;
    }

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
            ResourceDefinition resourceDefinition = new ResourceDefinition(annotation.module(), annotation.name(), "core");
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
                for (ResourceDefinition rd: resourceDefinitions){
                    entities.put(rd, entityResource);
                }
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
                if (!StringUtils.isEmpty(resourceDefinition.getModuleName())) {
                    if (ret.containsKey(resourceDefinition.getModuleName())) {
                        resourceNames = ret.get(resourceDefinition.getModuleName());
                    } else {
                        resourceNames = new ArrayList<EntityResourceDefinition>();
                    }
                    resourceNames.add(new EntityResourceDefinition(entityClassName, resourceDefinition.getResourceName()));
                    ret.put(resourceDefinition.getModuleName(), resourceNames);
                }
            }
        }
        for (String moduleName : ret.keySet()) {
            // sort resource name inside modules
            Collections.sort(ret.get(moduleName), new Comparator<EntityResourceDefinition>() {

                @Override
                public int compare(EntityResourceDefinition o1, EntityResourceDefinition o2) {
                    return o1.getResourceName().compareToIgnoreCase(o2.getResourceName());
                }

            });
            ret.put(moduleName, ret.get(moduleName));
        }
        return ret;
    }

    public EntityResource getEntityResourceByResourceName(String resourceName) {
        for (ResourceDefinition resourceDefinition : entities.keySet()) {
            if (resourceDefinition.getResourceName().equals(resourceName)) {
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
