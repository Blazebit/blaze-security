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
package com.blazebit.security.impl.utils;

import com.blazebit.security.Action;
import com.blazebit.security.impl.model.EntityAction;

/**
 *
 * @author cuszk
 */
public class ActionUtils {

    public static EntityAction getGrantAction() {
        EntityAction a = new EntityAction();
        a.setActionName("Grant");
        return a;
    }

    public static EntityAction getRevokeAction() {
        EntityAction a = new EntityAction();
        a.setActionName("Revoke");
        return a;
    }

    public static EntityAction getNoneAction() {
        EntityAction a = new EntityAction();
        a.setActionName("None");
        return a;
    }

    public static EntityAction getAccessAction() {
        EntityAction a = new EntityAction();
        a.setActionName("Access");
        return a;
    }

    public static EntityAction getReadAction() {
        EntityAction a = new EntityAction();
        a.setActionName("Read");
        return a;
    }

    public static EntityAction getWriteAction() {
        EntityAction a = new EntityAction();
        a.setActionName("Write");
        return a;
    }
}
