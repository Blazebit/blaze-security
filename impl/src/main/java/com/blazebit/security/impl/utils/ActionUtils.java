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

import com.blazebit.security.impl.model.EntityAction;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cuszk
 */
public class ActionUtils {

    public enum ActionConstants {

        CREATE,
        UPDATE,
        DELETE,
        GRANT,
        REVOKE,
        READ,
    }

    public static EntityAction getAction(ActionConstants action) {
        EntityAction a = new EntityAction();
        a.setActionName(action.name());
        return a;
    }

    public static List<EntityAction> getActionListForEntities() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.add(getAction(ActionConstants.CREATE));
        ret.add(getAction(ActionConstants.UPDATE));
        ret.add(getAction(ActionConstants.DELETE));
        ret.add(getAction(ActionConstants.READ));
        return ret;
    }

    public static List<EntityAction> getActionListForFields() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.add(getAction(ActionConstants.UPDATE));
        ret.add(getAction(ActionConstants.READ));
        return ret;
    }
    
     public static List<EntityAction> getActionListForActors() {
        List<EntityAction> ret = new ArrayList<EntityAction>();
        ret.add(getAction(ActionConstants.CREATE));
        ret.add(getAction(ActionConstants.UPDATE));
        ret.add(getAction(ActionConstants.DELETE));
        ret.add(getAction(ActionConstants.READ));
        ret.add(getAction(ActionConstants.GRANT));
        ret.add(getAction(ActionConstants.REVOKE));
        return ret;
    }
}
