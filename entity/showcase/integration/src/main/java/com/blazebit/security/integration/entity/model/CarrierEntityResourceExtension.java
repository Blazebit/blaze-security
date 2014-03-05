package com.blazebit.security.integration.entity.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blazebit.security.entity.EntityResourceDefinition;
import com.blazebit.security.entity.EntityResourceExtension;
import com.blazebit.security.model.sample.Party;

public class CarrierEntityResourceExtension implements EntityResourceExtension {

    @Override
    public Map<String, List<EntityResourceDefinition>> handle(Collection<Class<?>> collection) {
        // Create Carrier party and add it
        Map<String, List<EntityResourceDefinition>> ret = new HashMap<String, List<EntityResourceDefinition>>();
        for (Class<?> type : collection) {
            if (type.equals(Party.class)) {
                EntityResourceDefinition def = new EntityResourceDefinition("Carrier", "Carrier_Party", "object.type eq 'Carrier'");
                ret.put(type.getName(), Arrays.asList(def));
            }
        }
        return ret;
    }

}
