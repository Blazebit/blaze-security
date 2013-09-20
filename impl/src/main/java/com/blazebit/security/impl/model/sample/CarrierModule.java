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
package com.blazebit.security.impl.model.sample;

import com.blazebit.security.Module;
import com.blazebit.security.impl.model.EntityConstants;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cuszk
 */
public class CarrierModule implements Module {

    private static CarrierModule instance = new CarrierModule();

    private CarrierModule() {
    }

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
        carrierModule.add(EntityConstants.PARTY);
        
        
        return carrierModule;
    }
}
