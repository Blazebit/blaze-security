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

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.entity.EntityResourceDefinition;
import com.blazebit.security.entity.EntityResourceExtension;
import com.blazebit.security.entity.EntityResourceType;

/**
 * 
 * @author cuszk
 * 
 */
@ServiceProvider(Extension.class)
public class EntityResourceTypeExtension implements Extension {

	private final Map<String, List<EntityResourceDefinition>> entityResources = new HashMap<String, List<EntityResourceDefinition>>();
	private final Map<EntityResourceDefinition, String> resourceDefinitions = new HashMap<EntityResourceDefinition, String>();
	private final Collection<Class<?>> resourceClasses = new HashSet<Class<?>>();

	@SuppressWarnings("rawtypes")
	protected void detectInterfaces(
			@Observes ProcessAnnotatedType processAnnotatedType) {
		AnnotatedType<?> type = processAnnotatedType.getAnnotatedType();
		if (type.isAnnotationPresent(EntityResourceType.class)) {
			EntityResourceType annotation = (EntityResourceType) AnnotationUtils
					.findAnnotation(type.getJavaClass(), EntityResourceType.class);
			if ((annotation!=null && !annotation.skip())) {
				Class<?> entityClass = (Class<?>) type.getBaseType();
				resourceClasses.add(entityClass);
				String entityResource = entityClass.getName();
				List<EntityResourceDefinition> resources = new ArrayList<EntityResourceDefinition>();
				if (entityResources.containsKey(entityResource)) {
					resources = entityResources.get(entityResource);
				} else {
					resources = new ArrayList<EntityResourceDefinition>();
				}
				EntityResourceDefinition resourceDefinition = new EntityResourceDefinition(
						annotation.module(), annotation.name(),
						annotation.test());
				resources.add(resourceDefinition);
				this.resourceDefinitions
						.put(resourceDefinition, entityResource);
				entityResources.put(entityResource, resources);
			}
		}
	}

	protected void cleanup(
			@Observes AfterDeploymentValidation afterDeploymentValidation) {
		// resourceNames.clear();
		Iterator<EntityResourceExtension> iter = ServiceLoader.load(
				EntityResourceExtension.class).iterator();
		while (iter.hasNext()) {
			EntityResourceExtension callback = iter.next();
			Map<String, List<EntityResourceDefinition>> result = callback
					.handle(resourceClasses);
			// add to entity resources
			for (String entityResource : result.keySet()) {
				List<EntityResourceDefinition> resources = entityResources
						.get(entityResource);
				resources.addAll(result.get(entityResource));
				entityResources.put(entityResource, resources);
				for (EntityResourceDefinition rd : resources) {
					resourceDefinitions.put(rd, entityResource);
				}
			}
		}
		// Iterator<ActionImplicationProvider> actionImplicationIterator =
		// ServiceLoader.load(ActionImplicationProvider.class).iterator();
		// while (actionImplicationIterator.hasNext()) {
		// ActionImplicationProvider implication =
		// actionImplicationIterator.next();
		// actions.putAll(implication.getActionImplications());
		// }
	}

	public Map<String, List<EntityResourceDefinition>> getEntityResources() {
		return entityResources;
	}

	public Map<EntityResourceDefinition, String> getResourceDefinitions() {
		return resourceDefinitions;
	}

}
