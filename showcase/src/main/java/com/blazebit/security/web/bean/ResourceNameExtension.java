/**
 * 
 */
package com.blazebit.security.web.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.impl.model.ResourceName;

/**
 * @author Thomas Herzog <t.herzog@curecomp.com>
 * @company curecomp
 * @date 12.08.2013
 */
@ServiceProvider(Extension.class)
public class ResourceNameExtension implements Extension {

    private final Collection<AnnotatedType<?>> resourceClasses = new HashSet<AnnotatedType<?>>();

    @SuppressWarnings("rawtypes")
    protected void detectInterfaces(@Observes ProcessAnnotatedType processAnnotatedType) {
        AnnotatedType<?> type = processAnnotatedType.getAnnotatedType();
        if (type.isAnnotationPresent(ResourceName.class)) {
            resourceClasses.add(type);
        }
    }

    protected void cleanup(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        // resourceNames.clear();
    }

    public List<AnnotatedType<?>> getResourceNames() {
        List<AnnotatedType<?>> ret = new ArrayList<AnnotatedType<?>>(resourceClasses);
        Collections.sort(ret, new Comparator<AnnotatedType<?>>() {

            @Override
            public int compare(AnnotatedType<?> o1, AnnotatedType<?> o2) {
                return ((Class<?>) o1.getBaseType()).getSimpleName().compareToIgnoreCase(((Class<?>) o2.getBaseType()).getSimpleName());
            }

        });
        return ret;
    }

    public Map<String, List<AnnotatedType<?>>> getResourceNamesByModule() {
        Map<String, List<AnnotatedType<?>>> ret = new TreeMap<String, List<AnnotatedType<?>>>();
        List<AnnotatedType<?>> resourceTypes = new ArrayList<AnnotatedType<?>>(resourceClasses);
        for (AnnotatedType<?> type : resourceTypes) {
            Class<?> clazz = (Class<?>) type.getBaseType();
            ResourceName annotation = (ResourceName) AnnotationUtils.findAnnotation(clazz, ResourceName.class);
            for (String module : annotation.module()) {
                List<AnnotatedType<?>> resources;
                if (ret.containsKey(module)) {
                    resources = ret.get(module);
                } else {
                    resources = new ArrayList<AnnotatedType<?>>();
                }
                resources.add(type);
                ret.put(module, resources);
            }
            for (String module : ret.keySet()) {
                Collections.sort(ret.get(module), new Comparator<AnnotatedType<?>>() {

                    @Override
                    public int compare(AnnotatedType<?> o1, AnnotatedType<?> o2) {
                        return ((Class<?>) o1.getBaseType()).getSimpleName().compareToIgnoreCase(((Class<?>) o2.getBaseType()).getSimpleName());
                    }

                });
            }
        }
        return ret;
    }
}
