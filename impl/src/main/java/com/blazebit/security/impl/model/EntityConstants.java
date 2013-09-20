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

import com.blazebit.security.impl.model.sample.Carrier;
import com.blazebit.security.impl.model.sample.CarrierGroup;
import com.blazebit.security.impl.model.sample.CarrierTeam;
import com.blazebit.security.impl.model.sample.Contact;
import com.blazebit.security.impl.model.sample.Email;
import com.blazebit.security.impl.model.sample.Haulier;
import com.blazebit.security.impl.model.sample.Party;

/**
 *
 * @author Christian
 */
public enum EntityConstants {

    DOCUMENT,
    EMAIL(Email.class.getName()),
    WORKFLOW,
    CARRIER(Carrier.class.getName()),
    CARRIERGROUP(CarrierGroup.class.getName()),
    CARRIERTEAM(CarrierTeam.class.getName()),
    HAULIER(Haulier.class.getName()),
    CARRIER_USER(Carrier.class.getName()),
    PARTY(Party.class.getName()),
    CONTACT(Contact.class.getName()),
    PICKUPADDRESS(Carrier.class.getName());
    private String className;

    private EntityConstants() {
        this.className = name();
    }

    private EntityConstants(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
