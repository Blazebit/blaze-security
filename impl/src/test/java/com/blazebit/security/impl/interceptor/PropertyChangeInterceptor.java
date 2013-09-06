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
package com.blazebit.security.impl.interceptor;

/**
 *
 * @author cuszk
 */
import com.blazebit.security.impl.model.User;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;


import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

/**
 * Intercept changes on entities
 *
 * @author Nimo Naamani
 *
 */
@Interceptor
public class PropertyChangeInterceptor extends EmptyInterceptor {

    private static final long serialVersionUID = 1L;
    private Map<Class, PropertyChangeListener> listeners;

    @SuppressWarnings("unchecked")
    @Override
    @AroundInvoke
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
            Type[] types) {

        PropertyChangeListener listener = listeners.get(entity.getClass());

        //Only check for changes if an entity-specific listener was registered
        if (listener != null) {
            boolean report = false;
            for (int i = 0; i < currentState.length; i++) {
                if (currentState[i] == null) {
                    if (previousState[i] != null) {
                        report = true;
                    }
                } else if (!currentState[i].equals(previousState[i])) {
                    report = true;
                }

                if (report) {
                    listener.onChange(previousState[i], currentState[i], propertyNames[i]);
                    report = false;
                }
            }
        }
        return false;
    }

    public PropertyChangeInterceptor() {
        listeners = new HashMap<Class, PropertyChangeListener>();
        listeners.put(User.class, new GeneralPropertyChangeListener());
    }
}