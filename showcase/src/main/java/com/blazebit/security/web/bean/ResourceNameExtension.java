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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.impl.model.ResourceName;

/**
 * @author Thomas Herzog <t.herzog@curecomp.com>
 * @company curecomp
 * @date 12.08.2013
 */
@ServiceProvider(Extension.class)
public class ResourceNameExtension implements Extension {

    private final Collection<AnnotatedType<?>> resourceNames = new HashSet<AnnotatedType<?>>();

    @SuppressWarnings("rawtypes")
    protected void detectInterfaces(@Observes ProcessAnnotatedType processAnnotatedType) {
        AnnotatedType<?> type = processAnnotatedType.getAnnotatedType();

        if (type.isAnnotationPresent(ResourceName.class)) {
            resourceNames.add(type);
        }
    }

    protected void cleanup(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        // resourceNames.clear();
    }

    public List<AnnotatedType<?>> getResourceNames() {
        List<AnnotatedType<?>> ret = new ArrayList<AnnotatedType<?>>(resourceNames);
        Collections.sort(ret, new Comparator<AnnotatedType<?>>() {

            @Override
            public int compare(AnnotatedType<?> o1, AnnotatedType<?> o2) {
                return o1.getBaseType().getClass().getSimpleName().compareToIgnoreCase(o2.getBaseType().getClass().getSimpleName());
            }

        });
        return ret;
    }

}
