/*
 * Copyright 2013 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.security.impl.model;

import com.blazebit.security.Module;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cuszk
 */
public class CarrierModule implements Module {

    private static CarrierModule instance = new CarrierModule();

    /* A private Constructor prevents any other 
     * class from instantiating.
     */
    private CarrierModule() {
    }

    /* Static 'instance' method */
    public static CarrierModule getInstance() {
        return instance;
    }

    @Override
    public List<Enum<?>> getEntities() {
        List<Enum<?>> carrierModule = new ArrayList<Enum<?>>();
        carrierModule.add(EntityConstants.CARRIER);
        carrierModule.add(EntityConstants.HAULIER);
        carrierModule.add(EntityConstants.CARRIER_USER);
        carrierModule.add(EntityConstants.PICKUPADDRESS);
        carrierModule.add(EntityConstants.CARRIER_PARTY);
        carrierModule.add(EntityConstants.HAULIER_PARTY);
        carrierModule.add(EntityConstants.CARRIER_PARTY_CONTACT);
        carrierModule.add(EntityConstants.HAULIER_PARTY_CONTACT);
        return carrierModule;
    }
}
