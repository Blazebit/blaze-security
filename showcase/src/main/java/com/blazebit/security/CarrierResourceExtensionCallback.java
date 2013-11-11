package com.blazebit.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.security.impl.model.sample.Party;
import com.blazebit.security.web.bean.ResourceNameExtension;
import com.blazebit.security.web.bean.ResourceNameExtension.EntityResource;
import com.blazebit.security.web.bean.ResourceNameExtension.ResourceDefinition;

@ServiceProvider(ResourceExtensionCallback.class)
public class CarrierResourceExtensionCallback implements ResourceExtensionCallback {

    @Override
    public Map<EntityResource, List<ResourceDefinition>> handle(Collection<Class<?>> collection) {
        // Create Carrier party and add it
        Map<EntityResource, List<ResourceDefinition>> ret = new HashMap<EntityResource, List<ResourceDefinition>>();
        for (Class<?> type : collection) {
            if (type.equals(Party.class)) {
                ResourceDefinition def = new ResourceNameExtension.ResourceDefinition("Carrier", "Carrier_Party");
                ret.put(new EntityResource(type.getName()), Arrays.asList(def));
            }
        }
        return ret;
    }

}
